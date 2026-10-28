package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdBinding
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnostic
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslType
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslWire
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdFiniteness
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdPlan
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdPlanImport
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdPlanNode
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdPlanValidator
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdReference
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdResolvedInput
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdWireException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext
import org.cyclops.integrateddynamics.api.item.IAspectVariableFacade
import org.cyclops.integrateddynamics.api.item.IValueTypeVariableFacade
import org.cyclops.integrateddynamics.api.item.IOperatorVariableFacade
import org.cyclops.integrateddynamics.api.item.IVariableFacade
import org.cyclops.integrateddynamics.api.item.IVariableFacadeHandler
import org.cyclops.integrateddynamics.api.network.IPartNetwork
import org.cyclops.integrateddynamics.api.part.aspect.IAspect
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes
import org.cyclops.integrateddynamics.core.item.AspectVariableFacade
import org.cyclops.integrateddynamics.core.item.OperatorVariableFacade
import org.cyclops.integrateddynamics.core.item.ValueTypeVariableFacade
import org.cyclops.integrateddynamics.core.item.VariableFacadeHandlerRegistry
import org.cyclops.integrateddynamics.part.aspect.Aspects

/**
 * The native backend behind `id_describe_inputs` and `id_create_cards`.
 *
 * Describe never evaluates a variable: it derives a concrete contract from a real facade's stored
 * metadata, a fixed core-aspect table, or the static operator graph. Create re-parses and
 * re-validates the plan with the pure dsl validator, re-resolves every import against the same
 * authorized network, cross-checks the native operator registry, validates every literal and only
 * then preflights blank cards/capacity. IDs are allocated and cards written only after that
 * successful preflight. A failure while writing reports the phase, code and partial cards; it never
 * pretends a card was created or silently rolls back native side effects.
 */
internal object IdNativeBackend {
    private const val CREATE_PHASE = "create"

    fun describe(
        partNetwork: IPartNetwork,
        level: ServerLevel,
        arguments: JsonObject,
    ): JsonObject {
        val bindings = IdServerBindings.parseArgument(arguments)
        val context = ValueDeseralizationContext.of(level)
        val resolved = IdServerBindings.resolve(bindings, partNetwork, context)
        return buildJsonObject {
            put("ok", true)
            put("contract", com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.ID_DSL_CONTRACT)
            putJsonObject("inputs") {
                for ((name, binding) in bindings) {
                    val proof = resolved.getValue(name)
                    putJsonObject(name) {
                        put("binding", IdDslWire.encodeBinding(binding))
                        put("type", IdDslWire.encodeType(proof.type))
                        put("finite", proof.finite.wire)
                    }
                }
            }
        }
    }

    fun create(
        player: ServerPlayer,
        partNetwork: IPartNetwork,
        level: ServerLevel,
        arguments: JsonObject,
    ): JsonObject {
        val unknownArguments = arguments.keys - setOf("plan")
        if (unknownArguments.isNotEmpty()) {
            throw IdBackendError.create(
                IdDiagnosticCode.E_TYPE,
                "unsupported argument(s) ${unknownArguments.sorted().joinToString(", ")}",
            )
        }
        val planJson = arguments["plan"] as? JsonObject
            ?: throw IdBackendError.create(IdDiagnosticCode.E_TYPE, "argument 'plan' must be an object")

        var plan = try {
            IdDslWire.parsePlan(planJson)
        } catch (invalid: IdWireException) {
            throw IdBackendError.validate(
                invalid.code,
                invalid.message,
                node = invalid.nodeId,
                input = invalid.inputName,
                hint = wireHint(invalid.code),
            )
        }

        val context = ValueDeseralizationContext.of(level)
        val imports = plan.imports.associateBy { it.name }
        val resolved = try {
            resolveImports(plan, partNetwork, context)
        } catch (rejected: IdBackendError) {
            throw IdBackendError.validate(
                rejected.code,
                rejected.message ?: "an input could not be resolved",
                input = rejected.input,
                hint = rejected.hint,
            )
        }

        // Validate the original JSON, not a re-encoded copy: the pure validator owns the total
        // plan/serialized-literal budgets, and nothing is materialized as native values before it
        // has accepted the plan.
        val validation = try {
            IdPlanValidator.validate(
                planJson,
                resolved.mapValues { (_, proof) -> IdResolvedInput(proof.type, proof.finite) },
            )
        } catch (failure: RuntimeException) {
            throw IdBackendError.validate(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "the pure plan validator could not complete: ${safeMessage(failure)}",
                hint = "Recompile the source and retry with a fresh plan.",
            )
        }
        if (!validation.ok) {
            val first = validation.diagnostics.firstOrNull { it.isError } ?: validation.diagnostics.firstOrNull()
            if (first != null) throw diagnosticError(first)
            throw IdBackendError.validate(IdDiagnosticCode.E_TARGET_CONTRACT, "the plan validator rejected the plan")
        }
        plan = validation.plan ?: plan

        // Re-check every import's declared type/finiteness against the live network before any
        // mutation. A describe from an older network state must not silently authorize this create.
        for (import in plan.imports) {
            val proof = resolved.getValue(import.name)
            if (proof.type != import.type) {
                throw IdBackendError.validate(
                    IdDiagnosticCode.E_TARGET_CONTRACT,
                    "input '${import.name}' now resolves to ${proof.type.display()}, but the plan declares " +
                        "${import.type.display()}",
                    input = import.name,
                    hint = "Run id_describe_inputs again and recompile after changing network cards.",
                )
            }
            if (proof.finite != import.finiteness) {
                throw IdBackendError.validate(
                    IdDiagnosticCode.E_TARGET_CONTRACT,
                    "input '${import.name}' now resolves to finiteness '${proof.finite.wire}', but the plan " +
                        "declares '${import.finiteness.wire}'",
                    input = import.name,
                    hint = "Run id_describe_inputs again and recompile after changing network cards.",
                )
            }
        }

        val prepared = try {
            preflightNodes(plan, imports, resolved)
        } catch (rejected: IdBackendError) {
            throw rejected
        } catch (failure: IdNativeContractError) {
            throw IdBackendError.validate(
                failure.code,
                failure.message ?: "native preflight failed",
                hint = failure.hint,
            )
        } catch (failure: RuntimeException) {
            throw IdBackendError.validate(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "native preflight failed: ${safeMessage(failure)}",
                hint = "No variable ids were allocated.",
            )
        }
        preflightOutputs(plan, prepared.types, resolved)

        val aspectCards = linkedMapOf<AspectKey, PreparedAspect>()
        for (import in plan.imports) {
            val binding = import.binding
            if (binding !is IdBinding.Aspect) continue
            val key = AspectKey(binding.partId, binding.aspect)
            if (key in aspectCards) continue
            val location = ResourceLocation.tryParse(binding.aspect)
                ?: throw IdBackendError.validate(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    "aspect '${binding.aspect}' is not a valid resource id",
                    input = import.name,
                )
            val aspect = Aspects.REGISTRY.getAspect(location)
                ?: throw IdBackendError.validate(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    "aspect '${binding.aspect}' is not registered",
                    input = import.name,
                )
            aspectCards[key] = PreparedAspect(binding.partId, aspect, resolved.getValue(import.name))
        }

        val cardsNeeded = plan.nodes.size + aspectCards.size
        val blankPlan = try {
            IdBlankCards.plan(player.inventory, cardsNeeded)
        } catch (rejected: IdBackendError) {
            throw rejected
        } catch (failure: RuntimeException) {
            throw IdBackendError.preflight(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "the player inventory could not be checked for blank cards",
                hint = "Retry after closing any inventory screen.",
            )
        }

        val blanks = try {
            IdBlankCards.extract(player.inventory, blankPlan)
        } catch (rejected: IdBackendError) {
            throw rejected
        } catch (failure: RuntimeException) {
            throw IdBackendError.create(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "blank cards could not be consumed; no variable ids were allocated",
            )
        }

        val createdCards = ArrayList<JsonObject>(cardsNeeded)
        val createdCountBefore = { createdCards.size }
        var blankCursor = 0
        var slotCursor = 0
        val importIds = LinkedHashMap<String, Int>()
        val aspectCreated = LinkedHashMap<AspectKey, Int>()
        val nodeIds = IntArray(plan.nodes.size) { -1 }

        fun takeBlank(): ItemStack {
            if (blankCursor >= blanks.size || slotCursor >= blankPlan.outputSlots.size) {
                throw IdBackendError.create(
                    IdDiagnosticCode.E_TARGET_CONTRACT,
                    "the blank-card plan was exhausted before all definitions were written",
                    hint = "This is an internal capacity mismatch; no further cards were written.",
                )
            }
            return blanks[blankCursor]
        }

        fun place(card: ItemStack) {
            if (slotCursor >= blankPlan.outputSlots.size) {
                throw IdBackendError.create(
                    IdDiagnosticCode.E_TARGET_CONTRACT,
                    "no reserved inventory slot remains for a created card",
                )
            }
            IdBlankCards.place(player.inventory, blankPlan.outputSlots[slotCursor], card)
            // Only a placed card consumes its blank item and reserved slot; a failed write/place
            // leaves the blank available for the restore path.
            blankCursor++
            slotCursor++
        }

        try {
            // Every aspect/read binding needs one physical card; identical descriptors share it so
            // an alias does not create a duplicate definition.
            for (import in plan.imports) {
                when (val binding = import.binding) {
                    is IdBinding.Variable -> {
                        importIds[import.name] = binding.id
                    }
                    is IdBinding.Aspect -> {
                        val key = AspectKey(binding.partId, binding.aspect)
                        val existing = aspectCreated[key]
                        if (existing != null) {
                            importIds[import.name] = existing
                            continue
                        }
                        val preparedAspect = aspectCards.getValue(key)
                        val facade = AspectVariableFacade(true, preparedAspect.partId, preparedAspect.aspect)
                        writeAndPlace(
                            facade,
                            Aspects.REGISTRY as IVariableFacadeHandler<IAspectVariableFacade>,
                            { takeBlank() },
                            { card -> place(card) },
                        )
                        aspectCreated[key] = facade.id
                        importIds[import.name] = facade.id
                        createdCards.add(buildJsonObject {
                            put("input", import.name)
                            put("id", facade.id)
                        })
                    }
                }
            }

            // The plan is topologically ordered and node ids are their array indices.
            for (node in plan.nodes) {
                when (node) {
                    is IdPlanNode.Value -> {
                        val preparedValue = prepared.values[node.id]
                            ?: throw IdBackendError.create(
                                IdDiagnosticCode.E_TYPE,
                                "value node ${node.id} has no prepared native value",
                                node = node.id,
                            )
                        val expected = IdNativeCatalog.nativeType(node.type)
                        if (preparedValue.type.uniqueName != expected.uniqueName) {
                            throw IdBackendError.create(
                                IdDiagnosticCode.E_TYPE,
                                "native value type '${preparedValue.type.uniqueName}' does not match node type " +
                                    node.type.display(),
                                node = node.id,
                            )
                        }
                        @Suppress("UNCHECKED_CAST")
                        val facade = ValueTypeVariableFacade(
                            true,
                            expected as IValueType<IValue>,
                            preparedValue,
                        )
                        writeAndPlace(
                            facade,
                            ValueTypes.REGISTRY as IVariableFacadeHandler<IValueTypeVariableFacade<*>>,
                            { takeBlank() },
                            { card -> place(card) },
                        )
                        nodeIds[node.id] = facade.id
                        createdCards.add(nodeCard(node.id, facade.id))
                    }
                    is IdPlanNode.Native -> {
                        val operator = prepared.operators[node.id]
                            ?: throw IdBackendError.create(
                                IdDiagnosticCode.E_NATIVE_CONTRACT,
                                "native node ${node.id} has no resolved operator",
                                node = node.id,
                            )
                        val value = ValueTypeOperator.ValueOperator.of(operator)
                        @Suppress("UNCHECKED_CAST")
                        val facade = ValueTypeVariableFacade(
                            true,
                            ValueTypes.OPERATOR as IValueType<IValue>,
                            value as IValue,
                        )
                        writeAndPlace(
                            facade,
                            ValueTypes.REGISTRY as IVariableFacadeHandler<IValueTypeVariableFacade<*>>,
                            { takeBlank() },
                            { card -> place(card) },
                        )
                        nodeIds[node.id] = facade.id
                        createdCards.add(nodeCard(node.id, facade.id))
                    }
                    is IdPlanNode.Call -> {
                        val operator = prepared.operators[node.id]
                            ?: throw IdBackendError.create(
                                IdDiagnosticCode.E_NATIVE_CONTRACT,
                                "call node ${node.id} has no resolved operator",
                                node = node.id,
                            )
                        val arguments = IntArray(node.arguments.size) { index ->
                            idOf(node.arguments[index], importIds, nodeIds, node.id)
                        }
                        val facade = OperatorVariableFacade(true, operator, arguments)
                        writeAndPlace(
                            facade,
                            Operators.REGISTRY as IVariableFacadeHandler<IOperatorVariableFacade>,
                            { takeBlank() },
                            { card -> place(card) },
                        )
                        nodeIds[node.id] = facade.id
                        createdCards.add(nodeCard(node.id, facade.id))
                    }
                }
            }

            val outputs = buildJsonObject {
                for (output in plan.outputs) {
                    putJsonObject(output.name) {
                        put("id", idOf(output.reference, importIds, nodeIds, null))
                        put("type", IdDslWire.encodeType(output.type))
                    }
                }
            }

            return buildJsonObject {
                put("ok", true)
                put("phase", CREATE_PHASE)
                put("created", createdCards.size)
                put("outputs", outputs)
                putJsonArray("cards") { createdCards.forEach { add(it) } }
            }
        } catch (rejected: IdBackendError) {
            restoreQuietly(player, blanks.drop(blankCursor))
            throw IdBackendError(
                rejected.code,
                CREATE_PHASE,
                rejected.message ?: "Card creation failed",
                node = rejected.node,
                input = rejected.input,
                hint = rejected.hint,
            ).also {
                it.partialCreated = createdCountBefore()
                it.partialCards = buildJsonArray { createdCards.forEach { card -> add(card) } }
            }
        } catch (failure: RuntimeException) {
            restoreQuietly(player, blanks.drop(blankCursor))
            throw IdBackendError(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                CREATE_PHASE,
                "native card creation failed: ${safeMessage(failure)}",
                hint = "The reported cards were placed before the failure; IDs already allocated are not reused.",
            ).also {
                it.partialCreated = createdCountBefore()
                it.partialCards = buildJsonArray { createdCards.forEach { card -> add(card) } }
            }
        }
    }

    private fun resolveImports(
        plan: IdPlan,
        partNetwork: IPartNetwork,
        context: ValueDeseralizationContext,
    ): Map<String, IdNativeTyped> {
        val bindings = plan.imports.associate { it.name to it.binding }
        return IdServerBindings.resolve(bindings, partNetwork, context)
    }

    private class PreparedNodes(
        val types: List<IdNativeTyped>,
        val values: Map<Int, IValue>,
        val operators: Map<Int, IOperator>,
    )

    private data class PreparedAspect(
        val partId: Int,
        val aspect: IAspect,
        val type: IdNativeTyped,
    )

    private data class AspectKey(val partId: Int, val aspect: String)

    private fun preflightNodes(
        plan: IdPlan,
        imports: Map<String, IdPlanImport>,
        resolved: Map<String, IdNativeTyped>,
    ): PreparedNodes {
        val types = ArrayList<IdNativeTyped>(plan.nodes.size)
        val values = LinkedHashMap<Int, IValue>()
        val operators = LinkedHashMap<Int, IOperator>()
        for (node in plan.nodes) {
            when (node) {
                is IdPlanNode.Value -> {
                    val value = try {
                        IdNativeValues.value(node.type, node.value, node.id)
                    } catch (error: IdNativeContractError) {
                        throw IdBackendError.validate(error.code, error.message ?: "invalid value literal", node = node.id, hint = error.hint)
                    } catch (failure: RuntimeException) {
                        throw IdBackendError.validate(
                            IdDiagnosticCode.E_LITERAL_RESOURCE,
                            "value literal at node ${node.id} could not be constructed",
                            node = node.id,
                        )
                    }
                    val native = IdNativeCatalog.nativeType(node.type)
                    if (value.type.uniqueName != native.uniqueName) {
                        throw IdBackendError.validate(
                            IdDiagnosticCode.E_TYPE,
                            "value literal at node ${node.id} produced '${value.type.uniqueName}', expected " +
                                "'${native.uniqueName}'",
                            node = node.id,
                        )
                    }
                    values[node.id] = value
                    types += IdNativeTyped(node.type, finitenessOfNode(node.type, value))
                }
                is IdPlanNode.Native -> {
                    val operator = resolveOperator(node.operator, node.id)
                    if (operator.isHigherOrder()) {
                        throw IdBackendError.validate(
                            IdDiagnosticCode.E_NATIVE_CONTRACT,
                            "operator '${node.operator}' must be a call, not a native constant",
                            node = node.id,
                        )
                    }
                    val declared = node.type as? IdDslType.FunctionType
                        ?: throw IdBackendError.validate(
                            IdDiagnosticCode.E_FN_SIGNATURE,
                            "native operator node ${node.id} must declare a function type",
                            node = node.id,
                        )
                    try {
                        IdNativeCatalog.checkNativeConstant(operator, declared)
                    } catch (error: IdNativeContractError) {
                        throw IdBackendError.validate(error.code, error.message ?: "native operator mismatch", node = node.id, hint = error.hint)
                    }
                    operators[node.id] = operator
                    types += IdNativeTyped(node.type, IdFiniteness.FINITE)
                }
                is IdPlanNode.Call -> {
                    val operator = resolveOperator(node.operator, node.id)
                    if (node.arguments.size != operator.inputTypes.size) {
                        throw IdBackendError.validate(
                            IdDiagnosticCode.E_FN_SIGNATURE,
                            "operator '${node.operator}' takes ${operator.inputTypes.size} argument(s), but node " +
                                "${node.id} supplies ${node.arguments.size}",
                            node = node.id,
                        )
                    }
                    val arguments = node.arguments.map { typeOfReference(it, types, imports, resolved, node.id) }
                    if ((operator === Operators.OPERATOR_FILTER || operator === Operators.OPERATOR_REDUCE) &&
                        arguments.getOrNull(1)?.finite == IdFiniteness.INFINITE
                    ) {
                        throw IdBackendError.validate(
                            IdDiagnosticCode.E_INFINITE_CONSUMER,
                            "operator '${node.operator}' cannot consume a known-infinite list",
                            node = node.id,
                        )
                    }
                    if (operator === Operators.OBJECT_ITEMSTACK_LIST_COUNT &&
                        arguments.getOrNull(0)?.finite == IdFiniteness.INFINITE
                    ) {
                        throw IdBackendError.validate(
                            IdDiagnosticCode.E_INFINITE_CONSUMER,
                            "itemstack_itemlistcount cannot consume a known-infinite list",
                            node = node.id,
                        )
                    }
                    val inferred = try {
                        IdNativeCatalog.inferCall(operator, arguments)
                    } catch (error: IdNativeContractError) {
                        throw IdBackendError.validate(
                            error.code,
                            error.message ?: "operator call could not be checked",
                            node = node.id,
                            hint = error.hint,
                        )
                    }
                    if (inferred.type != node.type) {
                        throw IdBackendError.validate(
                            IdDiagnosticCode.E_TYPE,
                            "operator call at node ${node.id} produces ${inferred.type.display()}, but the plan " +
                                "declares ${node.type.display()}",
                            node = node.id,
                        )
                    }
                    operators[node.id] = operator
                    types += inferred
                }
            }
        }
        return PreparedNodes(types, values, operators)
    }

    private fun preflightOutputs(
        plan: IdPlan,
        nodeTypes: List<IdNativeTyped>,
        resolved: Map<String, IdNativeTyped>,
    ) {
        for (output in plan.outputs) {
            val actual = when (val reference = output.reference) {
                is IdReference.Node -> nodeTypes.getOrNull(reference.id)
                is IdReference.Input -> resolved[reference.name]
                ?: throw IdBackendError.validate(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    "output '${output.name}' references input '${reference.name}', which the server did not resolve",
                    input = reference.name,
                )
            } ?: throw IdBackendError.validate(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "output '${output.name}' points at an unknown reference",
            )
            if (actual.type != output.type) {
                throw IdBackendError.validate(
                    IdDiagnosticCode.E_TARGET_CONTRACT,
                    "output '${output.name}' is ${actual.type.display()} but the plan declares ${output.type.display()}",
                    input = if (output.reference is IdReference.Input) (output.reference as IdReference.Input).name else null,
                )
            }
        }
    }

    private fun typeOfReference(
        reference: IdReference,
        nodeTypes: List<IdNativeTyped>,
        imports: Map<String, IdPlanImport>,
        resolved: Map<String, IdNativeTyped>,
        node: Int,
    ): IdNativeTyped = when (reference) {
        is IdReference.Node -> nodeTypes.getOrNull(reference.id)
            ?: throw IdBackendError.validate(
                IdDiagnosticCode.E_TYPE,
                "node $node references node ${reference.id}, which is not an earlier typed node",
                node = node,
            )
        is IdReference.Input -> {
            if (reference.name !in imports) {
                throw IdBackendError.validate(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    "node $node references unknown input '${reference.name}'",
                    node = node,
                    input = reference.name,
                )
            }
            resolved[reference.name]
                ?: throw IdBackendError.validate(
                    IdDiagnosticCode.E_INPUT_BINDING,
                    "node $node references input '${reference.name}', which the server did not resolve",
                    node = node,
                    input = reference.name,
                )
        }
    }

    private fun idOf(
        reference: IdReference,
        importIds: Map<String, Int>,
        nodeIds: IntArray,
        node: Int?,
    ): Int = when (reference) {
        is IdReference.Node -> nodeIds.getOrNull(reference.id)?.takeIf { it >= 0 }
            ?: throw IdBackendError.create(
                IdDiagnosticCode.E_TARGET_CONTRACT,
                "a reference to node ${reference.id} has no allocated card id",
                node = node,
            )
        is IdReference.Input -> importIds[reference.name]
            ?: throw IdBackendError.create(
                IdDiagnosticCode.E_INPUT_BINDING,
                "input '${reference.name}' has no resolved card id",
                node = node,
                input = reference.name,
            )
    }

    private fun resolveOperator(id: String, node: Int): IOperator = try {
        if (com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdOperatorCatalog.resolveNative(id) == null) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                "operator '$id' is not in the pinned DSL operator catalog",
                "Use an operator published by the current id_describe_inputs/contract.",
            )
        }
        IdNativeCatalog.operator(id)
    } catch (error: IdNativeContractError) {
        throw IdBackendError.validate(error.code, error.message ?: "unknown native operator", node = node, hint = error.hint)
    }

    private fun nodeCard(node: Int, id: Int): JsonObject = buildJsonObject {
        put("node", node)
        put("id", id)
    }

    private inline fun <F : IVariableFacade> writeAndPlace(
        facade: F,
        handler: IVariableFacadeHandler<F>,
        blank: () -> ItemStack,
        place: (ItemStack) -> Unit,
    ) {
        val card = VariableFacadeHandlerRegistry.getInstance().writeVariableFacadeItem(blank(), facade, handler)
        place(card)
    }

    private fun diagnosticError(diagnostic: IdDiagnostic): IdBackendError = IdBackendError.validate(
        diagnostic.code,
        diagnostic.message,
        node = diagnostic.node,
        input = diagnostic.input,
        hint = diagnostic.hint,
    )

    private fun wireHint(code: IdDiagnosticCode): String? = when (code) {
        IdDiagnosticCode.E_LIMIT -> "Reduce the plan to the published DSL limits."
        IdDiagnosticCode.E_TARGET_CONTRACT -> "Recompile after refreshing id_describe_inputs."
        IdDiagnosticCode.E_INPUT_BINDING -> "Recompile with freshly described bindings."
        else -> null
    }

    private fun restoreQuietly(player: ServerPlayer, blanks: List<ItemStack>) {
        if (blanks.isEmpty()) return
        try {
            IdBlankCards.restore(player.inventory, blanks)
        } catch (_: RuntimeException) {
            // Never hide the original card-creation failure behind an inventory restore problem.
        }
    }

    private fun safeMessage(failure: RuntimeException): String {
        val message = failure.message?.replace('\n', ' ')?.trim().orEmpty()
        return if (message.isEmpty()) failure.javaClass.simpleName else message.take(256)
    }

    private fun IOperator.isHigherOrder(): Boolean = IdNativeCatalog.isHigherOrder(this)

    /** The plan's node type and literals prove list finiteness; otherwise stay unknown. */
    private fun finitenessOfNode(type: IdDslType, value: IValue): IdFiniteness =
        if (type is IdDslType.ListType && value is org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList.ValueList<*, *>) {
            IdNativeCatalog.finiteOf(value)
        } else {
            IdFiniteness.FINITE
        }
}
