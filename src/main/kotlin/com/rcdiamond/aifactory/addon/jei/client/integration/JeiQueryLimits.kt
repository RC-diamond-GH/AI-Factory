package com.rcdiamond.aifactory.addon.jei.client.integration

/**
 * Backend-only bounds, reply vocabulary and scope wording.
 *
 * Argument bounds are not repeated here: the tools own that convention
 * ([com.rcdiamond.aifactory.addon.jei.client.tool.JeiToolLimits]), and the backend reuses those
 * numbers for its second line of defence. What stays here is only what the backend itself enforces
 * plus the exact strings a reader of its replies can match on.
 */
object JeiQueryLimits {
    // ---------------------------------------------------------------------------------------------
    // Error codes. A failure reply is {ok:false, error, message}; these are every value `error` can
    // take, apart from `jei_not_ready`, which the pure gateway emits on its own.
    // ---------------------------------------------------------------------------------------------

    /** The pure gateway answered instead of the backend: JEI is absent or reloading. */
    const val ERROR_NOT_READY = "jei_not_ready"

    /** An argument is missing, blank, out of range, of the wrong shape, or not applicable. */
    const val ERROR_INVALID_ARGUMENTS = "invalid_arguments"

    /** The query ran off the client thread, so nothing was queried. */
    const val ERROR_WRONG_THREAD = "wrong_thread"

    /** `ingredient_key` is not bound in this JEI runtime (stale, evicted, or from another runtime). */
    const val ERROR_UNKNOWN_KEY = "unknown_key"

    /** No ingredient is registered under the given item/fluid `ingredient_id`. */
    const val ERROR_UNKNOWN_INGREDIENT = "unknown_ingredient"

    /** The ingredient exists but JEI does not show it in the recipe context, so it is not queryable. */
    const val ERROR_INGREDIENT_NOT_VISIBLE = "ingredient_not_visible"

    /** `category_id` does not match any registered JEI recipe type. */
    const val ERROR_UNKNOWN_CATEGORY = "unknown_category"

    /** The requested `ingredient_type` has no registered JEI ingredient type on this client. */
    const val ERROR_INGREDIENT_TYPE_UNAVAILABLE = "ingredient_type_unavailable"

    /** The query itself failed (an ingredient helper or recipe category threw); nothing is implied. */
    const val ERROR_QUERY_FAILED = "query_failed"

    // ---------------------------------------------------------------------------------------------
    // Ingredient kinds and their JEI identity. The labels are the `ingredient_type` values the tools
    // accept ([JeiToolLimits.INGREDIENT_TYPES]); the uids are JEI's stable ingredient type ids.
    // ---------------------------------------------------------------------------------------------

    const val KIND_ITEM = "item"
    const val KIND_FLUID = "fluid"

    val TYPE_UIDS: Map<String, String> = mapOf(KIND_ITEM to "item_stack", KIND_FLUID to "fluid_stack")

    /** What `amount` counts, per JEI ingredient type uid; absent means the type cannot report one. */
    val UNIT_BY_TYPE_UID: Map<String, String> = mapOf("item_stack" to "count", "fluid_stack" to "millibuckets")

    // ---------------------------------------------------------------------------------------------
    // Backend bounds.
    // ---------------------------------------------------------------------------------------------

    /**
     * Ingredient keys resolvable at once. The binding map is least-recently-used, so keys a session
     * keeps using survive; eviction is a deterministic function of the query sequence and an evicted
     * key answers `unknown_key` from then on. Keys carry a per-runtime nonce and a counter that never
     * restarts, so no key can ever be reassigned to a different ingredient.
     */
    const val MAX_BOUND_INGREDIENTS = 4096

    /**
     * Ingredients one search may examine before it stops and reports `truncated`. JEI exposes no
     * search index, so search walks the registered ingredient lists; this bounds the worst case.
     */
    const val MAX_SCANNED_INGREDIENTS = 100_000

    const val MAX_SLOTS_PER_RECIPE = 64
    const val MAX_CANDIDATES_PER_SLOT = 64
    const val MAX_CANDIDATES_PER_RECIPE = 256
    const val MAX_CATALYSTS_PER_RECIPE = 64

    /** Every free-form string in a reply is clipped to this length, and clipping is reported. */
    const val MAX_TEXT_LENGTH = 256

    /** Prefix of the opaque ingredient keys this backend hands out. */
    const val KEY_PREFIX = "jei"

    // ---------------------------------------------------------------------------------------------
    // Scope labels: short, stable names for exactly what a reply does and does not promise.
    // ---------------------------------------------------------------------------------------------

    /** Search results: registered ingredients visible in JEI's recipe context that match the filter. */
    const val SCOPE_INGREDIENT_SEARCH =
        "registered ingredients visible in JEI's recipe context, matched by literal substring on display name or resource id"

    /** Recipe results: what JEI's own lookup returns for the focused ingredient. */
    const val SCOPE_RECIPE_LOOKUP = "recipes JEI's own category and recipe lookup returns for the focused ingredient"

    /**
     * Slot candidates: JEI's rotation alternatives for one slot, null where the entry is hidden or
     * absent (JEI never exposes `addInvisibleIngredients`, and its candidate lists ignore focus).
     */
    const val SCOPE_SLOT_CANDIDATES =
        "visible display candidates in JEI's slot rotation order, ignoring the current focus; " +
            "null = hidden or absent entry; alternatives are not independent valid combinations"
}
