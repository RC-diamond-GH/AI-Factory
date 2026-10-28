package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslType
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslTypes
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdFiniteness
import java.util.IdentityHashMap
import net.minecraft.resources.ResourceLocation
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType
import org.cyclops.integrateddynamics.api.network.IPartNetwork
import org.cyclops.integrateddynamics.api.part.IPartType
import org.cyclops.integrateddynamics.api.part.aspect.IAspect
import org.cyclops.integrateddynamics.api.part.aspect.IAspectRead
import org.cyclops.integrateddynamics.core.part.PartTypes
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes
import org.cyclops.integrateddynamics.part.aspect.Aspects

/**
 * Static read-aspect contracts for the pinned core aspects whose value type alone does not carry
 * enough type information.
 *
 * An Integrated Dynamics list/operator aspect facade only says `LIST`/`OPERATOR`; it does not say
 * which element type or Fn signature the produced variable will have. For a fixed, source-verified
 * set of core list aspects the element type is known from the constructor that builds the value,
 * so it is recorded here. Any other list/operator aspect is rejected rather than treated as an
 * arbitrary type. Scalar aspects need no table: `aspect.getValueType()` is already concrete.
 *
 * The table is keyed by the actual [IAspect] instance, so a registry entry whose id was changed
 * would not accidentally match this contract. The part type is checked by its registry id, and the
 * live network's read-variable availability is checked before any card is made.
 */
internal object IdAspectContracts {
    data class Contract(
        val aspect: IAspect,
        val part: IPartType<*, *>,
        val type: IdDslType,
        val finite: IdFiniteness,
    )

    private val contracts: IdentityHashMap<IAspect, Contract> = IdentityHashMap<IAspect, Contract>().apply {
        add(Aspects.Read.Inventory.LIST_ITEMSTACKS, PartTypes.INVENTORY_READER, list(IdDslTypes.ITEM))
        add(Aspects.Read.Fluid.LIST_TANKFLUIDS, PartTypes.FLUID_READER, list(IdDslTypes.FLUID))
        add(Aspects.Read.Fluid.LIST_TANKCAPACITIES, PartTypes.FLUID_READER, list(IdDslTypes.INTEGER))
        add(Aspects.Read.Entity.LIST_ENTITIES, PartTypes.ENTITY_READER, list(IdDslTypes.ENTITY))
        add(Aspects.Read.Entity.LIST_PLAYERS, PartTypes.ENTITY_READER, list(IdDslTypes.ENTITY))
        add(Aspects.Read.ExtraDimensional.LIST_PLAYERS, PartTypes.EXTRADIMENSIONAL_READER, list(IdDslTypes.ENTITY))
        add(Aspects.Read.World.LIST_PLAYERS, PartTypes.WORLD_READER, list(IdDslTypes.ENTITY))
        add(Aspects.Read.Machine.LIST_GETRECIPES, PartTypes.MACHINE_READER, list(IdDslTypes.RECIPE))
    }

    private fun IdentityHashMap<IAspect, Contract>.add(
        aspect: IAspect,
        part: IPartType<*, *>,
        type: IdDslType,
    ) {
        put(aspect, Contract(aspect, part, type, IdFiniteness.FINITE))
    }

    private fun list(element: IdDslType): IdDslType = IdDslTypes.list(element)

    /**
     * Resolves one aspect binding against the authorized network and returns the authoritative
     * concrete type plus finiteness. Any unprovable aspect is an [IdBackendError] with
     * `E_INPUT_BINDING`; this never reads a world value.
     */
    fun resolve(
        partNetwork: IPartNetwork,
        partId: Int,
        aspectName: ResourceLocation,
        input: String? = null,
    ): IdNativeTyped {
        if (partId < 0) reject("part_id must be non-negative", input)
        val aspect = Aspects.REGISTRY.getAspect(aspectName)
            ?: reject("aspect '$aspectName' is not registered by the pinned Integrated Dynamics version", input)
        if (aspect !is IAspectRead<*, *>) {
            reject("aspect '$aspectName' is a write aspect, not a readable variable", input)
        }
        if (!partNetwork.hasPart(partId)) {
            reject("part $partId is not present on this authorized network", input)
        }
        val partType = try {
            partNetwork.getPartType(partId)
        } catch (_: RuntimeException) {
            null
        } ?: reject("part $partId has no readable part type on this network", input)

        val contract = contracts[aspect]
        if (contract != null && partType.uniqueName.toString() != contract.part.uniqueName.toString()) {
            reject(
                "aspect '$aspectName' belongs to part type '${contract.part.uniqueName}', " +
                    "but part $partId is '${partType.uniqueName}'",
                input,
            )
        }
        if (!hasReadVariable(partNetwork, partId, aspect)) {
            reject("part $partId does not expose read aspect '$aspectName' on this network", input)
        }

        val type = contract?.type ?: concreteAspectType(aspect, input)
        return IdNativeTyped(type, contract?.finite ?: IdFiniteness.FINITE)
    }

    private fun concreteAspectType(aspect: IAspectRead<*, *>, input: String?): IdDslType {
        val valueType = aspect.valueType
        if (valueType === ValueTypes.LIST) {
            reject(
                "aspect '${aspect.uniqueName}' produces a list whose element type has no fixed contract; " +
                    "bind a supported core list aspect or an existing typed variable",
                input,
            )
        }
        if (valueType === ValueTypes.OPERATOR || valueType.isCategory) {
            reject(
                "aspect '${aspect.uniqueName}' produces an operator or category value whose Fn signature " +
                    "cannot be proven statically",
                input,
            )
        }
        return IdNativeCatalog.scalarOf(valueType)
            ?: reject("aspect '${aspect.uniqueName}' has unsupported value type '${valueType.uniqueName}'", input)
    }

    @Suppress("UNCHECKED_CAST")
    private fun hasReadVariable(partNetwork: IPartNetwork, partId: Int, aspect: IAspectRead<*, *>): Boolean {
        val typed = aspect as IAspectRead<IValue, IValueType<IValue>>
        return try {
            partNetwork.hasPartVariable(partId, typed)
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun reject(message: String, input: String?): Nothing =
        throw IdBackendError.describe(IdDiagnosticCode.E_INPUT_BINDING, message, input = input)
}
