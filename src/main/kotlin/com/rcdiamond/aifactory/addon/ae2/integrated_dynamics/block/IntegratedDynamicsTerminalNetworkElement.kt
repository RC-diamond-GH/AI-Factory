package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.block

import org.cyclops.cyclopscore.datastructure.DimPos
import org.cyclops.integrateddynamics.api.network.INetwork
import org.cyclops.integrateddynamics.api.network.INetworkElement
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork
import org.cyclops.integrateddynamics.api.network.IPositionedNetworkElement
import org.cyclops.integrateddynamics.core.network.ConsumingNetworkElementBase

/**
 * The Integrated Dynamics network element of one terminal block.
 *
 * Integrated Dynamics' [org.cyclops.integrateddynamics.core.network.Network] asks every path position for its
 * [org.cyclops.integrateddynamics.api.network.INetworkElementProvider] while it is being formed, and this is
 * what that provider hands over. Creating or discarding these instances is how the network adds and removes
 * the terminal, so the block itself contains no network bookkeeping.
 *
 * A terminal performs no reads or writes of its own: it draws no energy ([getConsumptionRate] is zero), never
 * ticks, and only needs its position to be revalidated when its chunk is reloaded - the parts and variable
 * containers already on the network remain the data this element exists to reach. That is why this is a
 * [ConsumingNetworkElementBase] rather than one of Integrated Dynamics' part elements.
 */
class IntegratedDynamicsTerminalNetworkElement(private val elementPosition: DimPos) :
    ConsumingNetworkElementBase(), IPositionedNetworkElement {

    override fun getPosition(): DimPos = elementPosition

    /** A full block is present as long as its chunk is loaded and ticking. */
    override fun isLoaded(): Boolean = INetworkElement.shouldTick(elementPosition)

    override fun canRevalidate(network: INetwork): Boolean = canRevalidatePositioned(network, elementPosition)

    override fun revalidate(network: INetwork) {
        super.revalidate(network)
        revalidatePositioned(network, elementPosition)
    }

    /** A terminal is a passive network member: it consumes nothing and has no channel of its own. */
    @Deprecated("ID's legacy priority/channel hook; passive terminals have no configurable priority or channel")
    override fun setPriorityAndChannel(network: INetwork, priority: Int, channel: Int) {}

    override fun getPriority(): Int = 0

    override fun getChannel(): Int = IPositionedAddonsNetwork.DEFAULT_CHANNEL

    /**
     * Ordered by position against our own kind and by class name against everything else, which is the same
     * convention Integrated Dynamics' own positioned elements use inside the network's element set.
     */
    override fun compareTo(other: INetworkElement): Int =
        if (other is IntegratedDynamicsTerminalNetworkElement) {
            elementPosition.compareTo(other.getPosition())
        } else {
            javaClass.name.compareTo(other.javaClass.name)
        }

    /**
     * Identity follows the position, exactly like Integrated Dynamics' own positioned elements. The network
     * stores its elements in a sorted set, but some of its maps are hash based, so leaving this to the base
     * class would make every terminal element equal to every other one.
     */
    override fun equals(other: Any?): Boolean =
        other is IntegratedDynamicsTerminalNetworkElement && elementPosition == other.getPosition()

    override fun hashCode(): Int = elementPosition.hashCode()
}
