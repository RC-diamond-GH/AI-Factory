package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslType
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslTypes
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdFiniteness
import kotlin.jvm.optionals.getOrNull
import org.cyclops.cyclopscore.helper.BlockEntityHelpers
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext
import org.cyclops.integrateddynamics.api.item.IAspectVariableFacade
import org.cyclops.integrateddynamics.api.item.IDelayVariableFacade
import org.cyclops.integrateddynamics.api.item.IOperatorVariableFacade
import org.cyclops.integrateddynamics.api.item.IProxyVariableFacade
import org.cyclops.integrateddynamics.api.item.IValueTypeVariableFacade
import org.cyclops.integrateddynamics.api.item.IVariableFacade
import org.cyclops.integrateddynamics.api.network.IPartNetwork
import org.cyclops.integrateddynamics.blockentity.BlockEntityDelay
import org.cyclops.integrateddynamics.blockentity.BlockEntityProxy
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes
import org.cyclops.integrateddynamics.core.item.VariableFacadeHandlerRegistry

/**
 * Static type proof for an already existing variable/Proxy/Delay facade.
 *
 * No method here calls `IVariable.getValue()` or `facade.getVariable()`. Constants are read from
 * their stored value metadata; operator calls are re-derived from their operator and the static
 * types of their referenced facades; Proxy/Delay walk the real proxy block's read card. A cycle,
 * missing card, invalid facade or unprovable `Any`/list/operator signature is rejected with
 * `E_INPUT_BINDING` rather than becoming a guessed type.
 */
internal class IdFacadeTypes(
    private val partNetwork: IPartNetwork,
    private val context: ValueDeseralizationContext,
) {
    private val maximumDepth = 32
    private val maximumFacades = 4096
    private val provenVariables = HashMap<Int, Proof>()
    private var expandedFacades = 0

    private class Proof(val typed: IdNativeTyped, val depth: Int)

    fun variable(id: Int, input: String? = null): IdNativeTyped =
        variable(id, LinkedHashSet(), LinkedHashSet(), 0, input)

    private fun variable(
        id: Int,
        variables: MutableSet<Int>,
        proxies: MutableSet<Int>,
        depth: Int,
        input: String?,
    ): IdNativeTyped {
        if (id < 0) reject("variable id $id is invalid", input, "Use a variable card id allocated by Integrated Dynamics.")
        if (depth > maximumDepth) reject("variable reference chain is too deep (limit $maximumDepth)", input)
        if (!variables.add(id)) {
            reject("variable id $id takes part in a reference cycle", input, "Break the cyclic Proxy/Delay reference.")
        }
        try {
            // A proof checked nearer the depth limit is safe at shallower occurrences, not vice versa.
            provenVariables[id]?.let { if (depth <= it.depth) return it.typed }
            if (!partNetwork.hasVariableFacade(id)) {
                reject(
                    "variable id $id is not resolvable on this authorized network",
                    input,
                    "Put the card in a Variable Store or an active part slot on this network.",
                )
            }
            val facade = partNetwork.getVariableFacade(id)
                ?: reject("variable id $id has no facade on this network", input)
            return facade(facade, variables, proxies, depth, input).also { provenVariables[id] = Proof(it, depth) }
        } finally {
            variables.remove(id)
        }
    }

    private fun facade(
        facade: IVariableFacade,
        variables: MutableSet<Int>,
        proxies: MutableSet<Int>,
        depth: Int,
        input: String?,
    ): IdNativeTyped {
        if (depth > maximumDepth) reject("variable reference chain is too deep (limit $maximumDepth)", input)
        if (expandedFacades >= maximumFacades) {
            throw IdBackendError.describe(
                IdDiagnosticCode.E_LIMIT,
                "static type proof exceeds $maximumFacades facade expansions",
                input = input,
                hint = "Simplify the dependency graph or split the request into fewer bindings.",
            )
        }
        expandedFacades++
        if (!facade.isValid) {
            reject("variable ${facade.id} is invalid or is a blank/dummy card", input)
        }
        return when (facade) {
            is IDelayVariableFacade -> delay(facade, variables, proxies, depth, input)
            is IProxyVariableFacade -> proxy(facade, variables, proxies, depth, input)
            is IAspectVariableFacade -> aspect(facade, input)
            is IOperatorVariableFacade -> operatorCall(facade, variables, proxies, depth, input)
            is IValueTypeVariableFacade<*> -> value(facade, input)
            else -> reject("variable ${facade.id} has an unsupported facade kind", input)
        }
    }

    private fun value(facade: IValueTypeVariableFacade<*>, input: String?): IdNativeTyped {
        val valueType = facade.valueType
        if (valueType === ValueTypes.LIST) {
            val value = facade.value as? ValueTypeList.ValueList<*, *>
                ?: reject("list variable ${facade.id} has no readable list metadata", input)
            val element = IdNativeCatalog.listElementOf(value)
                ?: reject(
                    "list variable ${facade.id} has no provable concrete element type",
                    input,
                    "Bind a supported aspect/operator call or a constant list with a concrete element type.",
                )
            return IdNativeTyped(IdDslTypes.list(element), IdNativeCatalog.finiteOf(value))
        }
        if (valueType === ValueTypes.OPERATOR) {
            val value = facade.value as? ValueTypeOperator.ValueOperator
                ?: reject("operator variable ${facade.id} has no readable operator value", input)
            return operatorSignature(value.rawValue, input)
        }
        if (valueType.isCategory) {
            reject("variable ${facade.id} has category type '${valueType.uniqueName}', not a concrete DSL type", input)
        }
        val scalar = IdNativeCatalog.scalarOf(valueType)
            ?: reject("variable ${facade.id} has unsupported value type '${valueType.uniqueName}'", input)
        return IdNativeTyped(scalar, IdFiniteness.FINITE)
    }

    private fun operatorCall(
        facade: IOperatorVariableFacade,
        variables: MutableSet<Int>,
        proxies: MutableSet<Int>,
        depth: Int,
        input: String?,
    ): IdNativeTyped {
        val operator = facade.operator
            ?: reject("operator variable ${facade.id} has no operator", input)
        val ids = facade.variableIds
            ?: reject("operator variable ${facade.id} has no variable references", input)
        if (ids.size != operator.inputTypes.size) {
            reject(
                "operator variable ${facade.id} references ${ids.size} input(s), but its operator expects " +
                    "${operator.inputTypes.size}",
                input,
            )
        }
        val arguments = ids.mapIndexed { index, id ->
            variable(id, variables, proxies, depth + 1, input)
        }
        return try {
            IdNativeCatalog.inferCall(operator, arguments)
        } catch (error: IdNativeContractError) {
            reject(
                "operator variable ${facade.id} ('${operator.uniqueName}') has no provable output type: ${error.message}",
                input,
                error.hint,
            )
        } catch (error: RuntimeException) {
            reject("operator variable ${facade.id} could not be checked statically", input)
        }
    }

    private fun aspect(facade: IAspectVariableFacade, input: String?): IdNativeTyped {
        val aspect = facade.aspect
            ?: reject("aspect variable ${facade.id} has no registered aspect", input)
        return IdAspectContracts.resolve(partNetwork, facade.partId, aspect.uniqueName, input)
    }

    private fun proxy(
        facade: IProxyVariableFacade,
        variables: MutableSet<Int>,
        proxies: MutableSet<Int>,
        depth: Int,
        input: String?,
    ): IdNativeTyped {
        val proxyId = facade.proxyId
        if (proxyId < 0) reject("proxy variable ${facade.id} has an invalid proxy id", input)
        if (!proxies.add(proxyId)) {
            reject("proxy id $proxyId takes part in a reference cycle", input, "Break the Proxy recursion on the network.")
        }
        try {
            val position = partNetwork.getProxy(proxyId)
                ?: reject("proxy id $proxyId is not present on this authorized network", input)
            if (!position.isLoaded()) reject("proxy id $proxyId is not loaded", input)
            val blockEntity = BlockEntityHelpers.get(position, BlockEntityProxy::class.java).getOrNull()
                ?: reject("proxy id $proxyId does not point at a Proxy block", input)
            // A Delay facade (and a Proxy pointing at a Delay) yields the history list. Its element
            // type is the statically known type of the card in the Delay's read slot.
            if (blockEntity is BlockEntityDelay) {
                val element = targetCard(blockEntity, variables, proxies, depth, input)
                return IdNativeTyped(IdDslTypes.list(element.type), IdFiniteness.FINITE)
            }
            return targetCard(blockEntity, variables, proxies, depth, input)
        } finally {
            proxies.remove(proxyId)
        }
    }

    private fun delay(
        facade: IDelayVariableFacade,
        variables: MutableSet<Int>,
        proxies: MutableSet<Int>,
        depth: Int,
        input: String?,
    ): IdNativeTyped {
        val proxyId = facade.proxyId
        if (proxyId < 0) reject("delay variable ${facade.id} has an invalid delay id", input)
        if (!proxies.add(proxyId)) {
            reject("delay id $proxyId takes part in a reference cycle", input, "Break the Delay feedback/reference cycle.")
        }
        try {
            val position = partNetwork.getProxy(proxyId)
                ?: reject("delay id $proxyId is not present on this authorized network", input)
            if (!position.isLoaded()) reject("delay id $proxyId is not loaded", input)
            val blockEntity = BlockEntityHelpers.get(position, BlockEntityDelay::class.java).getOrNull()
                ?: reject("delay id $proxyId does not point at a Delay block", input)
            val element = targetCard(blockEntity, variables, proxies, depth, input)
            // A Delayer's history length is bounded by its configured capacity.
            return IdNativeTyped(IdDslTypes.list(element.type), IdFiniteness.FINITE)
        } finally {
            proxies.remove(proxyId)
        }
    }

    private fun targetCard(
        blockEntity: BlockEntityProxy,
        variables: MutableSet<Int>,
        proxies: MutableSet<Int>,
        depth: Int,
        input: String?,
    ): IdNativeTyped {
        val stack = blockEntity.inventory.getItem(BlockEntityProxy.SLOT_READ)
        if (stack.isEmpty) {
            reject(
                "proxy/delay read slot is empty, so the referenced type cannot be proven",
                input,
                "Put the source variable card in the Proxy/Delay read slot.",
            )
        }
        val facade = VariableFacadeHandlerRegistry.getInstance().handle(context, stack)
        if (!facade.isValid) {
            reject("proxy/delay read slot contains an invalid or blank variable card", input)
        }
        return facade(facade, variables, proxies, depth + 1, input)
    }

    private fun operatorSignature(operator: IOperator, input: String?): IdNativeTyped {
        val parameters = operator.inputTypes.map { native ->
            IdNativeCatalog.scalarOf(native)
                ?: reject(
                    "operator '${operator.uniqueName}' has input type '${native.uniqueName}' whose concrete " +
                        "DSL signature cannot be proven statically",
                    input,
                )
        }
        val result = IdNativeCatalog.scalarOf(operator.outputType)
            ?: reject(
                "operator '${operator.uniqueName}' has output type '${operator.outputType.uniqueName}' whose " +
                    "concrete DSL signature cannot be proven statically",
                input,
            )
        return IdNativeTyped(IdDslTypes.fn(parameters, result = result), IdFiniteness.FINITE)
    }

    private fun reject(message: String, input: String?, hint: String? = null): Nothing =
        throw IdBackendError.describe(IdDiagnosticCode.E_INPUT_BINDING, message, input = input, hint = hint)
}
