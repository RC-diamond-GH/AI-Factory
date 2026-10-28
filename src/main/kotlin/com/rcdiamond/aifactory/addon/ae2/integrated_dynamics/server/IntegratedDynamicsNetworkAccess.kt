package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolArguments
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolLimits
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.tool.IntegratedDynamicsToolResponse
import kotlin.jvm.optionals.getOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.minecraft.core.Direction
import org.cyclops.cyclopscore.datastructure.DimPos
import org.cyclops.cyclopscore.helper.BlockEntityHelpers
import org.cyclops.integrateddynamics.api.block.IVariableContainer
import org.cyclops.integrateddynamics.api.item.IAspectVariableFacade
import org.cyclops.integrateddynamics.api.item.IDelayVariableFacade
import org.cyclops.integrateddynamics.api.item.IOperatorVariableFacade
import org.cyclops.integrateddynamics.api.item.IProxyVariableFacade
import org.cyclops.integrateddynamics.api.item.IValueTypeVariableFacade
import org.cyclops.integrateddynamics.api.item.IVariableFacade
import org.cyclops.integrateddynamics.api.network.INetwork
import org.cyclops.integrateddynamics.api.network.IPartNetwork
import org.cyclops.integrateddynamics.api.network.IPartNetworkElement
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients
import org.cyclops.integrateddynamics.api.network.IPositionedNetworkElement
import org.cyclops.integrateddynamics.api.part.PartTarget
import org.cyclops.integrateddynamics.capability.network.EnergyNetworkConfig
import org.cyclops.integrateddynamics.capability.network.PartNetworkConfig
import org.cyclops.integrateddynamics.capability.variablecontainer.VariableContainerConfig
import org.cyclops.integrateddynamics.core.helper.PartHelpers

/**
 * Read-only inspection of a live Integrated Dynamics network.
 *
 * This is the server half of the three `id_*` tools. It answers from the network Integrated Dynamics itself
 * built ([INetwork]) and hands the result to the shared, Integrated Dynamics-free response shaper
 * ([IntegratedDynamicsToolResponse]); it never stores state, never mutates the network, and never evaluates a
 * variable, so an inspection can not change anything an operator would later observe.
 *
 * Only loaded elements are reported. Anything whose position is no longer loaded is skipped rather than
 * force-loaded, so listing a large network never pulls chunks in. Entries are sorted deterministically before
 * paging, so a page's contents are stable for a client that pages through the listing.
 *
 * Failures are rejected with an [IllegalArgumentException] carrying a caller-safe message; the session layer
 * turns that into the usual `{"ok":false,"error":...}` reply. An unknown operation is a rejection too.
 *
 * The tool contract (also enforced here, not only in the client) is:
 * - [IntegratedDynamicsToolLimits.OPERATION_NETWORK_INFO]: no parameters; a one-object summary.
 * - [IntegratedDynamicsToolLimits.OPERATION_LIST_PARTS]: `{filter,page,page_size}`; a page of parts with their
 *   real part id, type, channel and both the part's own and its target's position.
 * - [IntegratedDynamicsToolLimits.OPERATION_LIST_VARIABLES]: `{filter,page,page_size}`; a page of variable
 *   definitions with their references (operator, aspect, proxy) and output type - never a value.
 *
 * `filter` is matched case-insensitively as a substring. Parts are matched against their type id and their
 * unlocalized name; variables against their id, label, output type and referenced operator/aspect.
 */
object IntegratedDynamicsNetworkAccess {

    /** Inspects [network] for [operation]. See the type documentation for the exact contract. */
    fun inspect(network: INetwork, operation: String, arguments: JsonObject): JsonObject = when (operation) {
        IntegratedDynamicsToolLimits.OPERATION_NETWORK_INFO -> {
            IntegratedDynamicsToolArguments.networkInfo(arguments)
            networkInfo(network)
        }
        IntegratedDynamicsToolLimits.OPERATION_LIST_PARTS ->
            listParts(network, IntegratedDynamicsToolArguments.listParts(arguments))
        IntegratedDynamicsToolLimits.OPERATION_LIST_VARIABLES ->
            listVariables(network, IntegratedDynamicsToolArguments.listVariables(arguments))
        else -> throw IllegalArgumentException("Unknown Integrated Dynamics network operation '$operation'")
    }

    /**
     * A real summary of the network: how Integrated Dynamics currently sees it, not a re-derivation from
     * neighbouring blocks. Ingredient networks are reported by their actual component name (this is what
     * knows item, fluid and energy storage), and the energy figure is the network's own consumption rate.
     */
    private fun networkInfo(network: INetwork): JsonObject {
        val partNetwork = network.getCapability(PartNetworkConfig.CAPABILITY).resolve().getOrNull()
        val partCount = network.getElements().count { it is IPartNetworkElement<*, *> }
        val variableCount = partNetwork?.let { variableIds(network, it).size } ?: 0
        val energy = network.getCapability(EnergyNetworkConfig.CAPABILITY).resolve().getOrNull()
        val ingredientNetworks = network.getFullNetworkListeners()
            .filterIsInstance<IPositionedAddonsNetworkIngredients<*, *>>()
            .map { it.getComponent().getName().toString() }
            .distinct()
            .sorted()

        return IntegratedDynamicsToolResponse.summary(buildJsonObject {
            put("type", TYPE)
            put("initialized", network.isInitialized)
            put("killed", network.isKilled)
            put("cables", network.getCablesCount())
            put("elements", network.getElements().size)
            put("parts", partCount)
            put("variables", variableCount)
            put("energy_consumption", energy?.getConsumptionRate() ?: 0)
            putJsonArray("ingredient_networks") { ingredientNetworks.forEach { add(JsonPrimitive(it)) } }
        })
    }

    private fun listParts(network: INetwork, listing: IntegratedDynamicsToolArguments.Listing): JsonObject {
        val filter = listing.filter.lowercase()
        val entries = ArrayList<PartEntry>()
        for (element in network.getElements()) {
            if (element !is IPartNetworkElement<*, *>) continue
            if (!element.isLoaded) continue
            val id = element.getId()
            if (id < 0) continue
            val partType = element.getPart()
            val type = partType.getUniqueName().toString()
            val name = partType.getTranslationKey()
            if (filter.isNotEmpty() && !type.lowercase().contains(filter) && !name.lowercase().contains(filter)) {
                continue
            }
            val target = element.getTarget()
            entries.add(PartEntry(id, element.getChannel(), type, name, element.getPosition(), target))
        }
        entries.sortWith(compareBy({ it.id }, { it.type }))
        return page(entries, listing) { it.toJson() }
    }

    private fun listVariables(network: INetwork, listing: IntegratedDynamicsToolArguments.Listing): JsonObject {
        val partNetwork = network.getCapability(PartNetworkConfig.CAPABILITY).resolve().getOrNull()
            ?: return IntegratedDynamicsToolResponse.page(emptyList(), 0, listing.page, listing.pageSize)
        val filter = listing.filter.lowercase()
        val entries = variableIds(network, partNetwork)
            .mapNotNull { id -> partNetwork.getVariableFacade(id) }
            .filter { filter.isEmpty() || matches(it, filter) }
            .sortedBy { it.getId() }
        return page(entries, listing) { variableJson(it) }
    }

    /**
     * The ids of every variable facade this network can currently resolve.
     *
     * Integrated Dynamics does not expose an id enumeration: a facade is addressed by id, and the ids
     * themselves live in the variable containers (variable stores and the read parts' card slots) that the
     * network aggregates. This walks the very same containers the network's own composite cache is built from,
     * then keeps only ids the network confirms it can resolve ([IPartNetwork.hasVariableFacade]), so the
     * listing can never name a definition the network would fail to look up.
     */
    private fun variableIds(network: INetwork, partNetwork: IPartNetwork): Set<Int> {
        val ids = LinkedHashSet<Int>()
        for (element in network.getElements()) {
            if (element !is IPositionedNetworkElement) continue
            val position = element.getPosition()
            if (!position.isLoaded()) continue
            val container: IVariableContainer? = if (element is IPartNetworkElement<*, *>) {
                val side = element.getSide()
                PartHelpers.getPartContainer(position, side).resolve().getOrNull()
                    ?.getCapability(VariableContainerConfig.CAPABILITY, side)
                    ?.resolve()?.getOrNull()
            } else {
                BlockEntityHelpers.getCapability(position, null, VariableContainerConfig.CAPABILITY).resolve().getOrNull()
            }
            if (container != null) ids.addAll(container.getVariableCache().keys)
        }
        return ids.filterTo(LinkedHashSet()) { partNetwork.hasVariableFacade(it) }
    }

    /** One page of [all], shaped by [shape]; paging is bounded by the listing the caller already validated. */
    private fun <T> page(
        all: List<T>,
        listing: IntegratedDynamicsToolArguments.Listing,
        shape: (T) -> JsonObject,
    ): JsonObject {
        val total = all.size
        val offset = listing.page.toLong() * listing.pageSize
        val items = if (offset >= total) {
            emptyList()
        } else {
            all.subList(offset.toInt(), minOf(total.toLong(), offset + listing.pageSize).toInt()).map(shape)
        }
        return IntegratedDynamicsToolResponse.page(items, total, listing.page, listing.pageSize)
    }

    /** A part, kept as its raw data so sorting and paging never re-derive it from JSON. */
    private class PartEntry(
        val id: Int,
        private val channel: Int,
        val type: String,
        private val name: String,
        private val position: DimPos,
        private val target: PartTarget,
    ) {
        fun toJson(): JsonObject = buildJsonObject {
            put("id", id)
            put("type", type)
            put("name", name)
            put("channel", channel)
            putJsonObject("position") { putPosition(position, target.getCenter().getSide()) }
            putJsonObject("target") { putPosition(target.getTarget().getPos(), target.getTarget().getSide()) }
        }
    }

    /** The variable definition's own data: identity and references, deliberately never its value. */
    private fun variableJson(facade: IVariableFacade): JsonObject = buildJsonObject {
        put("id", facade.getId())
        facade.getLabel()?.let { put("label", it) }
        val valid = facade.isValid()
        put("valid", valid)
        put("output_type", if (valid) facade.getOutputType().getUniqueName().toString() else null)
        when (facade) {
            is IDelayVariableFacade -> {
                put("kind", "delay")
                put("proxy_id", facade.getProxyId())
            }
            is IProxyVariableFacade -> {
                put("kind", "proxy")
                put("proxy_id", facade.getProxyId())
            }
            is IAspectVariableFacade -> {
                put("kind", "aspect")
                put("aspect", facade.getAspect()?.getUniqueName()?.toString())
                put("part_id", facade.getPartId())
            }
            is IOperatorVariableFacade -> {
                put("kind", "operator")
                put("operator", facade.getOperator()?.getUniqueName()?.toString())
                facade.getVariableIds()?.let { inputs ->
                    putJsonArray("inputs") { inputs.forEach { add(JsonPrimitive(it)) } }
                }
            }
            is IValueTypeVariableFacade<*> -> put("kind", "value")
            else -> put("kind", "variable")
        }
    }

    /** Everything the filter may match on, lowercased once by the caller. */
    private fun matches(facade: IVariableFacade, lowercasedFilter: String): Boolean {
        val haystack = buildString {
            append(facade.getId())
            facade.getLabel()?.let { append(' ').append(it) }
            if (facade.isValid()) append(' ').append(facade.getOutputType().getUniqueName())
            when (facade) {
                is IAspectVariableFacade -> facade.getAspect()?.let { append(' ').append(it.getUniqueName()) }
                is IOperatorVariableFacade -> facade.getOperator()?.let { append(' ').append(it.getUniqueName()) }
                else -> Unit
            }
        }
        return haystack.lowercase().contains(lowercasedFilter)
    }

    /** The `type` marker of a network summary; the client routes on this kind elsewhere. */
    private const val TYPE = "integrated_dynamics"
}

/** A position plus the face it is attached on, in the shape the tools document. */
private fun JsonObjectBuilder.putPosition(position: DimPos, side: Direction?) {
    val pos = position.getBlockPos()
    put("dimension", position.getLevel())
    put("x", pos.x)
    put("y", pos.y)
    put("z", pos.z)
    side?.let { put("face", it.serializedName) }
}
