package com.rcdiamond.aifactory.addon.integrated_dynamics.dsl

/**
 * Fixed, source-verified contract for the core read aspects whose element type cannot be recovered
 * from `IAspectRead.getValueType()` alone (that returns `integrateddynamics:list`).
 *
 * The server must also verify the actual part type and that the aspect is registered on that part.
 * Extension aspects are not listed here: a list aspect outside this table is not statically
 * provable and is rejected by the DSL input description contract.
 */
data class IdAspectContract(
    val id: String,
    val partId: String,
    val type: IdDslType,
    val finiteness: IdFiniteness,
    val description: String = "",
)

object IdAspectContracts {
    private fun d(id: String): String = "${IdOperatorCatalog.MOD_ID}:$id"

    val listAspects: Map<String, IdAspectContract> = listOf(
        IdAspectContract(
            d("list_inventory_itemstacks"),
            d("inventory_reader"),
            IdDslTypes.list(IdDslTypes.ITEM),
            IdFiniteness.FINITE,
            "inventory slot item stacks",
        ),
        IdAspectContract(
            d("list_fluid_fluidstacks"),
            d("fluid_reader"),
            IdDslTypes.list(IdDslTypes.FLUID),
            IdFiniteness.FINITE,
            "tank fluid stacks",
        ),
        IdAspectContract(
            d("list_fluid_capacities"),
            d("fluid_reader"),
            IdDslTypes.list(IdDslTypes.INTEGER),
            IdFiniteness.FINITE,
            "tank capacities in mB",
        ),
        IdAspectContract(
            d("list_entity_entities"),
            d("entity_reader"),
            IdDslTypes.list(IdDslTypes.ENTITY),
            IdFiniteness.FINITE,
            "entities in the reader area",
        ),
        IdAspectContract(
            d("list_entity_players"),
            d("entity_reader"),
            IdDslTypes.list(IdDslTypes.ENTITY),
            IdFiniteness.FINITE,
            "players in the reader area",
        ),
        IdAspectContract(
            d("list_extradimensional_players"),
            d("extradimensional_reader"),
            IdDslTypes.list(IdDslTypes.ENTITY),
            IdFiniteness.FINITE,
            "players in other dimensions",
        ),
        IdAspectContract(
            d("list_world_players"),
            d("world_reader"),
            IdDslTypes.list(IdDslTypes.ENTITY),
            IdFiniteness.FINITE,
            "players in the current world",
        ),
        IdAspectContract(
            d("list_recipehandler_recipes"),
            d("machine_reader"),
            IdDslTypes.list(IdDslTypes.RECIPE),
            IdFiniteness.FINITE,
            "recipes of the positioned recipe handler",
        ),
    ).associateByTo(LinkedHashMap()) { it.id }

    fun find(aspectId: String): IdAspectContract? = listAspects[aspectId]
}
