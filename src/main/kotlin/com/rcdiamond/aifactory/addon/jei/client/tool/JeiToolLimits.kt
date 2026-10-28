package com.rcdiamond.aifactory.addon.jei.client.tool

/**
 * Names and bounds shared by the client-side JEI query tools.
 *
 * Nothing here references JEI or Minecraft, which keeps the tools free of game classes and makes
 * every bound testable without booting the game. The numbers are the policy the model is held to:
 * the tools reject anything outside them before the gateway is asked to do any work.
 */
object JeiToolLimits {
    /** Searches JEI's current ingredient list by name. */
    const val OPERATION_FIND_INGREDIENTS = "jei_find_ingredients"

    /** Looks up the recipes, uses or catalyst entries JEI displays for one ingredient. */
    const val OPERATION_QUERY_RECIPES = "jei_query_recipes"

    /** Longest free-form search text the model may send. */
    const val MAX_FILTER_LENGTH = 128

    /** Longest opaque runtime key or resource id string the model may send. */
    const val MAX_KEY_LENGTH = 128

    /** Longest resource id (namespace included) the model may send. */
    const val MAX_ID_LENGTH = 128

    /** Highest page number either operation accepts. */
    const val MAX_PAGE = 100

    const val DEFAULT_INGREDIENT_PAGE_SIZE = 20
    const val MAX_INGREDIENT_PAGE_SIZE = 50

    const val DEFAULT_RECIPE_PAGE_SIZE = 5
    const val MAX_RECIPE_PAGE_SIZE = 10

    /** Namespace assumed when a resource id is given without one. */
    const val DEFAULT_NAMESPACE = "minecraft"

    /** The ingredient kinds a recipe query may name. */
    val INGREDIENT_TYPES: Set<String> = setOf("item", "fluid")

    /** How the named ingredient is looked up: as an output, as an input, or as a catalyst. */
    val DIRECTIONS: Set<String> = setOf("recipes", "uses", "catalyst")
}
