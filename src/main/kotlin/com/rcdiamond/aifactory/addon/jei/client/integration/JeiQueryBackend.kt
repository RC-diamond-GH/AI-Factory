package com.rcdiamond.aifactory.addon.jei.client.integration

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.addon.jei.client.JeiIngredientQuery
import com.rcdiamond.aifactory.addon.jei.client.JeiQueryService
import com.rcdiamond.aifactory.addon.jei.client.JeiRecipeQuery
import com.rcdiamond.aifactory.addon.jei.client.tool.JeiToolLimits
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import mezz.jei.api.forge.ForgeTypes
import mezz.jei.api.gui.ingredient.IRecipeSlotView
import mezz.jei.api.helpers.IJeiHelpers
import mezz.jei.api.ingredients.IIngredientHelper
import mezz.jei.api.ingredients.IIngredientType
import mezz.jei.api.ingredients.ITypedIngredient
import mezz.jei.api.ingredients.subtypes.UidContext
import mezz.jei.api.recipe.IFocus
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.IRecipeCategoriesLookup
import mezz.jei.api.recipe.IRecipeManager
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.runtime.IIngredientManager
import mezz.jei.api.runtime.IIngredientVisibility
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidType
import net.minecraftforge.registries.ForgeRegistries
import java.util.Locale
import java.util.UUID

/** Production main-thread assertion: JEI's runtime is client-only and single-threaded. */
private fun assertOnClientThread() {
    val client = Minecraft.getInstance()
    check(client.isSameThread) { "AI Factory JEI queries must run on the client thread" }
}

/**
 * Real [JeiQueryService] over JEI's public runtime API (CommonApi plus the Forge fluid type).
 *
 * Every key carries a random per-runtime nonce and a counter that never restarts, so a key from
 * another runtime (or one saved to disk) can only answer `unknown_key` - never another ingredient.
 * Search walks JEI's registered ingredient lists (there is no public search index) and keeps the
 * entries visible in the recipe context whose localized name or resource id contains the literal,
 * case-insensitive filter. Recipe lookup uses JEI's own category/recipe lookup with one focus,
 * paginates *before* building any layout, then reads the real layout's slot view with the original
 * focus group; every JEI stream it opens is closed.
 *
 * Replies claim exactly what JEI shows: slot candidate lists are JEI's rotation alternatives (focus
 * independent, `addInvisibleIngredients` never exposed, null placeholders where an entry is hidden or
 * absent) and are not independent valid combinations. Nothing is fabricated - no air fallback, recipe
 * ids only from [IRecipeCategory.getRegistryName], an unavailable or empty layout is incomplete rather
 * than empty, amounts are omitted when the helper reports -1 - and every bound that bit, clipped
 * string or incomplete entry is reported with its reason.
 *
 * JEI's runtime API is synchronous and client-thread only, so every call starts with
 * [assertClientThread]; the production default checks `Minecraft.getInstance().isSameThread`.
 */
class JeiQueryBackend(
    private val ingredientManager: IIngredientManager,
    private val recipeManager: IRecipeManager,
    private val helpers: IJeiHelpers,
    private val generation: Long = 1L,
    private val assertClientThread: () -> Unit = ::assertOnClientThread,
) : JeiQueryService {

    /** Plugin wiring, and the constructor an offline smoke harness uses. */
    constructor(
        runtime: IJeiRuntime,
        generation: Long = 1L,
        assertClientThread: () -> Unit = ::assertOnClientThread,
    ) : this(runtime.ingredientManager, runtime.recipeManager, runtime.jeiHelpers, generation, assertClientThread)

    /** Random per-runtime nonce: keys can never collide across runtimes, worlds or saved sessions. */
    private val nonce: String = UUID.randomUUID().toString().replace("-", "")

    private val visibility: IIngredientVisibility get() = helpers.ingredientVisibility

    /** JEI's own ingredient identity: type uid plus the `UidContext.Ingredient` uid object. */
    private data class IngredientIdentity(val typeUid: String, val uid: Any)

    private val keyByIdentity = HashMap<IngredientIdentity, String>()

    /** LRU key bindings; an eviction (deterministic for a query sequence) drops the identity too. */
    private val bindings: LinkedHashMap<String, BoundIngredient> =
        object : LinkedHashMap<String, BoundIngredient>(512, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BoundIngredient>): Boolean {
                val evict = size > JeiQueryLimits.MAX_BOUND_INGREDIENTS
                if (evict) keyByIdentity.remove(eldest.value.identity)
                return evict
            }
        }

    private var next = 0L

    // ---------------------------------------------------------------------------------------------
    // Ingredient search
    // ---------------------------------------------------------------------------------------------

    override fun findIngredients(query: JeiIngredientQuery): JsonObject {
        guardThread()?.let { return it }
        val filter = query.filter.trim()
        if (filter.isEmpty()) return invalid("'filter' must not be blank.")
        if (filter.length > JeiToolLimits.MAX_FILTER_LENGTH) {
            return invalid("'filter' must be at most ${JeiToolLimits.MAX_FILTER_LENGTH} characters.")
        }
        pageError(query.page, query.pageSize, JeiToolLimits.MAX_INGREDIENT_PAGE_SIZE)?.let { return it }

        val state = SearchState(needle = filter, skip = query.page.toLong() * query.pageSize, wanted = query.pageSize)
        try {
            for (type in registeredTypes()) {
                if (!scanType(type, state)) break
            }
        } catch (e: RuntimeException) {
            LOG.error("AI Factory JEI ingredient search failed", e)
            return failure(
                JeiQueryLimits.ERROR_QUERY_FAILED,
                "JEI ingredient search failed (${e.javaClass.simpleName}); no results are implied.",
            )
        }

        val returned = state.results.size
        val caveats = buildJsonArray {
            add(CAVEAT_SEARCH_VISIBILITY)
            add(CAVEAT_KEYS_SESSION)
            if (state.truncated) {
                add(
                    "The ingredient lists were only examined up to " +
                        "${JeiQueryLimits.MAX_SCANNED_INGREDIENTS} entries, so further matches may exist.",
                )
            }
            if (state.failed > 0) add("${state.failed} ingredient(s) could not be read and were skipped.")
        }
        return buildJsonObject {
            put("ok", true)
            put("page", query.page)
            put("page_size", query.pageSize)
            put("returned", returned)
            put("has_more", state.hasMore)
            put("truncated", state.truncated || state.clipped)
            put("scanned", state.scanned)
            put("scope", JeiQueryLimits.SCOPE_INGREDIENT_SEARCH)
            put("caveats", caveats)
            putJsonArray("ingredients") {
                for (index in 0 until returned) add(state.results[index])
            }
        }
    }

    /** Deterministic scan order: registered ingredient types sorted by their stable uid. */
    private fun registeredTypes(): List<IIngredientType<*>> =
        ingredientManager.registeredIngredientTypes.sortedBy { it.uid }

    /**
     * Appends this type's matches to [state]; false means stop scanning further types (page complete or
     * scan budget exhausted).
     */
    private fun <V> scanType(type: IIngredientType<V>, state: SearchState): Boolean {
        val helper = ingredientManager.getIngredientHelper(type)
        for (candidate in ingredientManager.getAllIngredients(type)) {
            if (state.scanned >= JeiQueryLimits.MAX_SCANNED_INGREDIENTS) {
                state.truncated = true
                return false
            }
            state.scanned++
            val ingredient = candidate ?: continue
            val typed = ingredientManager.createTypedIngredient(type, ingredient).orElse(null) ?: continue
            val matched = try {
                (helper.getDisplayName(ingredient) ?: "").contains(state.needle, ignoreCase = true) ||
                    (helper.getResourceLocation(ingredient)?.toString() ?: "")
                        .contains(state.needle, ignoreCase = true)
            } catch (e: RuntimeException) {
                LOG.warn("AI Factory JEI could not read ingredient ${state.scanned - 1} of type ${type.uid}", e)
                state.failed++
                continue
            }
            if (!matched || !isVisible(typed)) continue
            state.matched++
            if (state.matched <= state.skip) continue
            if (state.results.size >= state.wanted) {
                // The page is full and this is one visible match past it: has_more is observed here,
                // without serializing (or binding a key for) an ingredient the caller never sees.
                state.hasMore = true
                return false
            }
            val json = try {
                candidateJson(typed) { state.clipped = true }
            } catch (e: RuntimeException) {
                LOG.warn("AI Factory JEI could not describe a ${type.uid} ingredient", e)
                state.failed++
                continue
            }
            state.results.add(json)
        }
        return true
    }

    // ---------------------------------------------------------------------------------------------
    // Recipe lookup
    // ---------------------------------------------------------------------------------------------

    override fun queryRecipes(query: JeiRecipeQuery): JsonObject {
        guardThread()?.let { return it }

        val key = query.ingredientKey
        val id = query.ingredientId
        if ((key == null) == (id == null)) {
            return invalid("Provide exactly one of 'ingredient_key' or 'ingredient_id'.")
        }
        if (key != null) {
            if (key.isBlank() || key.length > JeiToolLimits.MAX_KEY_LENGTH) {
                return invalid(
                    "'ingredient_key' must be non-blank and at most ${JeiToolLimits.MAX_KEY_LENGTH} characters.",
                )
            }
            if (query.ingredientType != JeiQueryLimits.KIND_ITEM) {
                return invalid("'ingredient_type' is only valid together with 'ingredient_id'.")
            }
        }
        if (id != null && id.length > JeiToolLimits.MAX_ID_LENGTH) {
            return invalid("'ingredient_id' must be at most ${JeiToolLimits.MAX_ID_LENGTH} characters.")
        }
        if (query.ingredientType !in JeiToolLimits.INGREDIENT_TYPES) {
            return invalid("'ingredient_type' must be one of ${JeiToolLimits.INGREDIENT_TYPES.sorted()}.")
        }
        val role = ROLE_BY_DIRECTION[query.direction]
            ?: return invalid("'direction' must be one of ${JeiToolLimits.DIRECTIONS.sorted()}.")
        pageError(query.page, query.pageSize, JeiToolLimits.MAX_RECIPE_PAGE_SIZE)?.let { return it }

        val rawCategory = query.categoryId
        val wantedCategory = if (rawCategory == null || rawCategory.isBlank()) {
            null
        } else {
            canonicalResourceId(rawCategory)?.toString()
                ?: return invalid("'category_id' must be a lowercase resource id like 'minecraft:crafting'.")
        }

        val resolution = if (key != null) resolveByKey(key) else resolveRegisteredIngredient(query.ingredientType, id!!)
        resolution.failure?.let { return it }
        val typed = resolution.typed
            ?: return failure(JeiQueryLimits.ERROR_QUERY_FAILED, "The queried ingredient could not be resolved.")

        var clipped = false
        val described = try {
            candidateJson(typed) { clipped = true }
        } catch (e: RuntimeException) {
            LOG.error("AI Factory JEI could not describe the queried ingredient", e)
            return failure(
                JeiQueryLimits.ERROR_QUERY_FAILED,
                "JEI could not describe the queried ingredient (${e.javaClass.simpleName}).",
            )
        }

        val categoryTypes = if (wantedCategory == null) null else categoryTypesFor(wantedCategory)
        if (wantedCategory != null && categoryTypes != null && categoryTypes.isEmpty()) {
            return failure(
                JeiQueryLimits.ERROR_UNKNOWN_CATEGORY,
                "'category_id' '${echo(wantedCategory)}' does not match any registered JEI recipe type.",
            )
        }

        val focus = try {
            focusFor(role, typed)
        } catch (e: RuntimeException) {
            LOG.error("AI Factory JEI could not create a focus", e)
            return failure(
                JeiQueryLimits.ERROR_QUERY_FAILED,
                "JEI could not create a focus for the queried ingredient (${e.javaClass.simpleName}).",
            )
        }
        val focusGroup = helpers.focusFactory.createFocusGroup(listOf(focus))

        val selected = try {
            selectRecipes(focus, categoryTypes, query.page, query.pageSize)
        } catch (e: RuntimeException) {
            LOG.error("AI Factory JEI recipe lookup failed", e)
            return failure(
                JeiQueryLimits.ERROR_QUERY_FAILED,
                "JEI recipe lookup failed (${e.javaClass.simpleName}); no recipes are implied.",
            )
        }

        val hasMore = selected.size > query.pageSize
        val pageResults = if (hasMore) selected.subList(0, query.pageSize) else selected
        val catalystCache = HashMap<String, CatalystExtraction>()
        val recipes = buildJsonArray {
            for (selection in pageResults) {
                val catalysts = catalystsFor(selection.category, catalystCache)
                add(
                    try {
                        serializeRecipe(selection.category, selection.recipe, focusGroup, catalysts)
                    } catch (e: RuntimeException) {
                        // A category's own extraction code is the only third-party code that runs here:
                        // log it loudly and mark the entry incomplete instead of pretending it is empty.
                        LOG.warn(
                            "AI Factory JEI extraction failed for category " + selection.category.recipeType.uid,
                            e,
                        )
                        extractionFailureEntry(selection.category, e, catalysts)
                    },
                )
            }
        }

        var anyTruncated = clipped
        var incomplete = 0
        var unreadableCatalysts = 0
        for (entry in recipes) {
            val obj = entry as? JsonObject ?: continue
            if (obj.boolField("truncated")) anyTruncated = true
            if (!obj.boolField("display_extraction_complete")) incomplete++
            if (obj.textField("catalysts_error") != null) unreadableCatalysts++
        }

        val caveats = buildJsonArray {
            add(CAVEAT_SLOT_ROTATION)
            add(CAVEAT_NULL_PLACEHOLDERS)
            add(CAVEAT_NO_CRAFTING_AUTHORIZATION)
            add(CAVEAT_KEYS_SESSION)
            if (anyTruncated) {
                add(
                    "A reply hit a bound (${JeiQueryLimits.MAX_SLOTS_PER_RECIPE} slots, " +
                        "${JeiQueryLimits.MAX_CANDIDATES_PER_SLOT} candidates per slot, " +
                        "${JeiQueryLimits.MAX_CANDIDATES_PER_RECIPE} serialized candidates per recipe, " +
                        "${JeiQueryLimits.MAX_CATALYSTS_PER_RECIPE} catalysts, " +
                        "${JeiQueryLimits.MAX_TEXT_LENGTH} character strings); the counters say what was " +
                        "dropped.",
                )
            }
            if (incomplete > 0) {
                add("$incomplete recipe(s) could not be extracted completely; 'extraction' names the reason.")
            }
            if (unreadableCatalysts > 0) {
                add(
                    "$unreadableCatalysts recipe(s) have incomplete catalysts; 'catalysts_error' names the " +
                        "cause.",
                )
            }
        }

        val ingredient = buildJsonObject {
            described.forEach { (field, value) -> put(field, value) }
            put("variant", resolution.variant)
            put("role", role.name.lowercase(Locale.ROOT))
        }
        return buildJsonObject {
            put("ok", true)
            put("page", query.page)
            put("page_size", query.pageSize)
            put("returned", pageResults.size)
            put("has_more", hasMore)
            put("direction", query.direction)
            put("category_id", wantedCategory)
            put("ingredient", ingredient)
            put("recipes", recipes)
            put("truncated", anyTruncated)
            put("scope", JeiQueryLimits.SCOPE_RECIPE_LOOKUP)
            put("caveats", caveats)
        }
    }

    /**
     * Walks JEI's category lookup (optionally limited to the requested types) and its per-category
     * recipe lookup, skipping whole matches until the page starts and collecting one extra match so
     * `has_more` is observed, not guessed. No layout is created here, and both streams are closed even
     * when the page is complete early.
     */
    private fun selectRecipes(
        focus: IFocus<*>,
        categoryTypes: List<RecipeType<*>>?,
        page: Int,
        pageSize: Int,
    ): List<SelectedRecipe> {
        val focusList = listOf(focus)
        var categoriesLookup: IRecipeCategoriesLookup =
            recipeManager.createRecipeCategoryLookup().limitFocus(focusList)
        if (categoryTypes != null) {
            categoriesLookup = categoriesLookup.limitTypes(categoryTypes)
        }
        val selected = ArrayList<SelectedRecipe>(pageSize + 1)
        var remaining = page.toLong() * pageSize
        categoriesLookup.get().use { categories ->
            val categoryIterator = categories.iterator()
            while (categoryIterator.hasNext()) {
                val category = categoryIterator.next()
                recipeManager.createRecipeLookup(category.recipeType).limitFocus(focusList).get().use { recipes ->
                    val recipeIterator = recipes.iterator()
                    while (recipeIterator.hasNext()) {
                        val recipe = recipeIterator.next()
                        if (remaining > 0) {
                            remaining--
                            continue
                        }
                        selected.add(SelectedRecipe(category, recipe))
                        if (selected.size > pageSize) return selected
                    }
                }
            }
        }
        return selected
    }

    /** Registered recipe types whose uid matches the requested category id. */
    private fun categoryTypesFor(categoryId: String): List<RecipeType<*>> {
        val types = ArrayList<RecipeType<*>>()
        helpers.allRecipeTypes.use { allTypes ->
            val iterator = allTypes.iterator()
            while (iterator.hasNext()) {
                val type = iterator.next()
                if (type.uid.toString() == categoryId) types.add(type)
            }
        }
        return types
    }

    /**
     * Recipe catalysts of one category, in JEI's order, visible ones only and bounded. JEI's catalyst
     * lookup runs category code, so a failure there is reported as `catalysts_error` (with whatever was
     * already read) and never as a silently empty list.
     */
    private fun catalystsFor(
        category: IRecipeCategory<*>,
        cache: MutableMap<String, CatalystExtraction>,
    ): CatalystExtraction {
        val uid = category.recipeType.uid.toString()
        cache[uid]?.let { return it }
        val values = ArrayList<JsonElement>()
        var truncated = false
        var error: String? = null
        try {
            recipeManager.createRecipeCatalystLookup(category.recipeType).get().use { catalysts ->
                val iterator = catalysts.iterator()
                while (iterator.hasNext()) {
                    val typed = iterator.next() ?: continue
                    if (!isVisible(typed)) continue
                    if (values.size >= JeiQueryLimits.MAX_CATALYSTS_PER_RECIPE) {
                        truncated = true
                        break
                    }
                    values.add(candidateJson(typed) { truncated = true })
                }
            }
        } catch (e: RuntimeException) {
            LOG.warn("AI Factory JEI could not read catalysts of category $uid", e)
            error = e.javaClass.simpleName
        }
        val extraction = CatalystExtraction(JsonArray(values), truncated, error)
        cache[uid] = extraction
        return extraction
    }

    // ---------------------------------------------------------------------------------------------
    // Recipe serialization
    // ---------------------------------------------------------------------------------------------

    /**
     * Serializes one recipe of whatever category JEI returned. The category's generic parameter is
     * captured from the star-projected value (no raw cast), so [IRecipeCategory.getRegistryName] and
     * `createRecipeLayoutDrawable` only ever see a recipe of this category's own recipe class.
     */
    private fun <T> serializeRecipe(
        category: IRecipeCategory<T>,
        rawRecipe: Any,
        focusGroup: IFocusGroup,
        catalysts: CatalystExtraction,
    ): JsonObject {
        var truncated = false
        val categoryId = clipped(category.recipeType.uid.toString()) { truncated = true }
        val title = clippedOrNull(readTitle(category)) { truncated = true }
        val typed = castRecipe(category, rawRecipe)
        if (typed == null) {
            return recipeEntry(
                categoryId, title, null, false, "recipe_object_not_of_category_type",
                JsonArray(emptyList()), catalysts, truncated,
            )
        }
        val recipeId = clippedOrNull(readRegistryName(category, typed)) { truncated = true }
        val layout = recipeManager.createRecipeLayoutDrawable(category, typed, focusGroup).orElse(null)
        if (layout == null) {
            return recipeEntry(
                categoryId, title, recipeId, false, "layout_unavailable",
                JsonArray(emptyList()), catalysts, truncated,
            )
        }
        val slotViews = layout.recipeSlotsView.slotViews
        if (slotViews.isEmpty()) {
            // JEI built no slots: extraction is unavailable, not an empty ingredient list.
            return recipeEntry(
                categoryId, title, recipeId, false, "no_slots",
                JsonArray(emptyList()), catalysts, truncated,
            )
        }
        val slots = serializeSlots(slotViews) { truncated = true }
        return recipeEntry(
            categoryId, title, recipeId, true, null, slots.json, catalysts,
            truncated || slots.truncated, slots.omittedCandidates, slots.omittedSlots, slots.hiddenCandidates,
        )
    }

    private fun recipeEntry(
        categoryId: String,
        categoryName: String?,
        recipeId: String?,
        displayExtractionComplete: Boolean,
        extraction: String?,
        slots: JsonArray,
        catalysts: CatalystExtraction,
        truncated: Boolean,
        omittedCandidates: Int = 0,
        omittedSlots: Int = 0,
        hiddenCandidates: Int = 0,
    ): JsonObject = buildJsonObject {
        put("category_id", categoryId)
        put("category_name", categoryName)
        put("recipe_id", recipeId)
        put("display_extraction_complete", displayExtractionComplete)
        put("extraction", extraction)
        put("truncated", truncated || catalysts.truncated)
        put("candidates_scope", JeiQueryLimits.SCOPE_SLOT_CANDIDATES)
        put("hidden_candidates", hiddenCandidates)
        put("omitted_candidates", omittedCandidates)
        put("omitted_slots", omittedSlots)
        put("catalysts", catalysts.values)
        put("catalysts_truncated", catalysts.truncated)
        put("catalysts_error", catalysts.error)
        put("slots", slots)
    }

    /** Entry for a recipe whose category threw while JEI was building or reading its layout. */
    private fun extractionFailureEntry(
        category: IRecipeCategory<*>,
        cause: RuntimeException,
        catalysts: CatalystExtraction,
    ): JsonObject = buildJsonObject {
        put("category_id", clipped(category.recipeType.uid.toString()) {})
        put("category_name", clippedOrNull(readTitle(category)) {})
        put("recipe_id", JsonNull)
        put("display_extraction_complete", false)
        put("extraction", "extraction_failed")
        put("extraction_error", cause.javaClass.simpleName)
        put("message", clippedOrNull(cause.message ?: "no message") {})
        put("truncated", catalysts.truncated)
        put("candidates_scope", JeiQueryLimits.SCOPE_SLOT_CANDIDATES)
        put("hidden_candidates", 0)
        put("omitted_candidates", 0)
        put("omitted_slots", 0)
        put("catalysts", catalysts.values)
        put("catalysts_truncated", catalysts.truncated)
        put("catalysts_error", catalysts.error)
        put("slots", JsonArray(emptyList()))
    }

    private class CatalystExtraction(val values: JsonArray, val truncated: Boolean, val error: String?)

    private class SlotExtraction(
        val json: JsonArray,
        val truncated: Boolean,
        val omittedCandidates: Int,
        val omittedSlots: Int,
        val hiddenCandidates: Int,
    )

    /**
     * Reads JEI's slot view: indices stay stable, a null candidate (absent or hidden rotation entry)
     * stays a null placeholder, visible alternatives keep JEI's rotation order (which ignores focus),
     * and slot, per-slot candidate and per-recipe candidate bounds are counted as they bite.
     */
    private fun serializeSlots(slotViews: List<IRecipeSlotView>, onClipped: () -> Unit): SlotExtraction {
        var truncated = false
        var omittedCandidates = 0
        var omittedSlots = 0
        var hiddenCandidates = 0
        var budget = JeiQueryLimits.MAX_CANDIDATES_PER_RECIPE
        val keptSlots = minOf(slotViews.size, JeiQueryLimits.MAX_SLOTS_PER_RECIPE)
        if (slotViews.size > keptSlots) {
            truncated = true
            omittedSlots = slotViews.size - keptSlots
        }
        val slots = buildJsonArray {
            for (index in 0 until keptSlots) {
                val slot = slotViews[index]
                val candidates = slot.allIngredientsList
                val keptCandidates = minOf(candidates.size, JeiQueryLimits.MAX_CANDIDATES_PER_SLOT)
                var slotHidden = 0
                var slotOmitted = 0
                val candidateArray = buildJsonArray {
                    for (candidateIndex in 0 until keptCandidates) {
                        val typed = candidates[candidateIndex]
                        if (typed == null) {
                            add(JsonNull)
                            continue
                        }
                        if (!isVisible(typed)) {
                            add(JsonNull)
                            slotHidden++
                            continue
                        }
                        if (budget <= 0) {
                            add(JsonNull)
                            slotOmitted++
                            continue
                        }
                        add(candidateJson(typed, onClipped))
                        budget--
                    }
                }
                if (candidates.size > keptCandidates) slotOmitted += candidates.size - keptCandidates
                hiddenCandidates += slotHidden
                omittedCandidates += slotOmitted
                if (slotHidden > 0 || slotOmitted > 0) truncated = true
                add(
                    buildJsonObject {
                        put("index", index)
                        put("role", slot.role.name.lowercase(Locale.ROOT))
                        put("name", clippedOrNull(slot.slotName.orElse(null), onClipped))
                        put("total_candidates", candidates.size)
                        put("hidden_candidates", slotHidden)
                        put("omitted_candidates", slotOmitted)
                        put("candidates", candidateArray)
                    },
                )
            }
        }
        return SlotExtraction(slots, truncated, omittedCandidates, omittedSlots, hiddenCandidates)
    }

    // ---------------------------------------------------------------------------------------------
    // Ingredient resolution
    // ---------------------------------------------------------------------------------------------

    private class IngredientResolution(
        val typed: ITypedIngredient<*>? = null,
        val variant: String = "",
        val failure: JsonObject? = null,
    )

    /**
     * A bound key, re-checked for visibility on every query: an ingredient can be hidden (by the
     * player, another mod or a server-side removal) without JEI's runtime restarting.
     */
    private fun resolveByKey(key: String): IngredientResolution {
        val bound = bindings[key]
            ?: return IngredientResolution(
                failure = failure(
                    JeiQueryLimits.ERROR_UNKNOWN_KEY,
                    "ingredient_key '${echo(key)}' is not bound in this JEI runtime. It may have expired " +
                        "(only the ${JeiQueryLimits.MAX_BOUND_INGREDIENTS} most recently used keys stay " +
                        "valid) or come from an earlier runtime or session; run " +
                        "${JeiToolLimits.OPERATION_FIND_INGREDIENTS} again for a fresh key.",
                ),
            )
        if (!isVisible(bound.typed)) {
            return IngredientResolution(
                failure = failure(
                    JeiQueryLimits.ERROR_INGREDIENT_NOT_VISIBLE,
                    "The ingredient of ingredient_key '${echo(key)}' is not visible in JEI's recipe " +
                        "context any more, so it is not queryable right now.",
                ),
            )
        }
        return IngredientResolution(bound.typed, VARIANT_BY_KEY)
    }

    /**
     * Direct selection of a registered item or fluid by resource id: the registry's default stack,
     * typed by JEI and required to be visible in the recipe context. Air and the empty fluid are
     * rejected explicitly instead of becoming an empty stack.
     */
    private fun resolveRegisteredIngredient(ingredientType: String, id: String): IngredientResolution {
        val parsed = canonicalResourceId(id)
            ?: return IngredientResolution(
                failure = invalid(
                    "'ingredient_id' must be a lowercase resource id like 'minecraft:iron_ingot' or 'iron_ingot'.",
                ),
            )
        val typeUid = JeiQueryLimits.TYPE_UIDS[ingredientType]
            ?: return IngredientResolution(
                failure = invalid("'ingredient_type' must be one of ${JeiToolLimits.INGREDIENT_TYPES.sorted()}."),
            )
        val type = ingredientManager.registeredIngredientTypes.firstOrNull { it.uid == typeUid }
            ?: return IngredientResolution(
                failure = failure(
                    JeiQueryLimits.ERROR_INGREDIENT_TYPE_UNAVAILABLE,
                    "No '$ingredientType' ingredient type is registered with JEI on this client, so " +
                        "'$ingredientType' ids cannot be resolved.",
                ),
            )
        val ingredient: Any = if (ingredientType == JeiQueryLimits.KIND_ITEM) {
            val item = ForgeRegistries.ITEMS.getValue(parsed)
                ?: return IngredientResolution(
                    failure = failure(
                        JeiQueryLimits.ERROR_UNKNOWN_INGREDIENT,
                        "No item is registered as '${echo(parsed.toString())}'.",
                    ),
                )
            if (item === Items.AIR) {
                return IngredientResolution(
                    failure = failure(
                        JeiQueryLimits.ERROR_UNKNOWN_INGREDIENT,
                        "'${echo(parsed.toString())}' is air, which is not a queryable ingredient.",
                    ),
                )
            }
            ItemStack(item)
        } else {
            val fluid: Fluid = ForgeRegistries.FLUIDS.getValue(parsed)
                ?: return IngredientResolution(
                    failure = failure(
                        JeiQueryLimits.ERROR_UNKNOWN_INGREDIENT,
                        "No fluid is registered as '${echo(parsed.toString())}'.",
                    ),
                )
            if (fluid === Fluids.EMPTY) {
                return IngredientResolution(
                    failure = failure(
                        JeiQueryLimits.ERROR_UNKNOWN_INGREDIENT,
                        "'${echo(parsed.toString())}' is the empty fluid, which is not a queryable ingredient.",
                    ),
                )
            }
            FluidStack(fluid, FluidType.BUCKET_VOLUME)
        }

        val typed = try {
            typedIngredient(type, ingredient)
        } catch (e: RuntimeException) {
            LOG.error("AI Factory JEI could not create a typed ingredient for $parsed", e)
            return IngredientResolution(
                failure = failure(
                    JeiQueryLimits.ERROR_QUERY_FAILED,
                    "JEI could not create a typed ingredient for '${echo(parsed.toString())}'.",
                ),
            )
        } ?: return IngredientResolution(
            failure = failure(
                JeiQueryLimits.ERROR_UNKNOWN_INGREDIENT,
                "JEI does not expose '${echo(parsed.toString())}' as a queryable ingredient of type " +
                    "'$ingredientType'.",
            ),
        )

        if (!isVisible(typed)) {
            return IngredientResolution(
                failure = failure(
                    JeiQueryLimits.ERROR_INGREDIENT_NOT_VISIBLE,
                    "'${echo(parsed.toString())}' is not visible in JEI's recipe context (hidden by the " +
                        "player or a mod, removed by the server, or not registered with JEI), so it is not " +
                        "queryable right now.",
                ),
            )
        }
        return IngredientResolution(typed, VARIANT_DEFAULT)
    }

    // ---------------------------------------------------------------------------------------------
    // Generic JEI helpers: each takes one star-projected argument, which Kotlin captures at the call
    // site, so no call site needs a raw or unchecked cast.
    // ---------------------------------------------------------------------------------------------

    private fun <V> candidateJson(typed: ITypedIngredient<V>, onClipped: () -> Unit): JsonObject {
        val type: IIngredientType<V> = typed.type
        val helper: IIngredientHelper<V> = ingredientManager.getIngredientHelper(type)
        val ingredient = typed.ingredient
        val amount = helper.getAmount(ingredient)
        return buildJsonObject {
            put("key", bind(type, helper, typed))
            put("type", clipped(type.uid, onClipped))
            put("id", clippedOrNull(helper.getResourceLocation(ingredient)?.toString(), onClipped))
            put("name", clipped(helper.getDisplayName(ingredient), onClipped))
            put("amount", if (amount >= 0) amount else null)
            put("unit", JeiQueryLimits.UNIT_BY_TYPE_UID[type.uid])
        }
    }

    private fun <V> isVisible(typed: ITypedIngredient<V>): Boolean =
        visibility.isIngredientVisible(typed, UidContext.Recipe)

    private fun <V> focusFor(role: RecipeIngredientRole, typed: ITypedIngredient<V>): IFocus<V> =
        helpers.focusFactory.createFocus(role, typed)

    private fun <V> bind(type: IIngredientType<V>, helper: IIngredientHelper<V>, typed: ITypedIngredient<V>): String {
        // JEI's own identity: the type uid plus the uid object (equals/hashCode), never its toString.
        val identity = IngredientIdentity(type.uid, helper.getUid(typed.ingredient, UidContext.Ingredient))
        val existing = keyByIdentity[identity]
        if (existing != null && bindings[existing] != null) return existing
        val key = "${JeiQueryLimits.KEY_PREFIX}-$nonce-${generation.toString(36)}-${(++next).toString(36)}"
        bindings[key] = BoundIngredient(identity, typed)
        keyByIdentity[identity] = key
        return key
    }

    private fun <V> typedIngredient(type: IIngredientType<V>, ingredient: Any): ITypedIngredient<V>? {
        val cast = type.getCastIngredient(ingredient) ?: return null
        return ingredientManager.createTypedIngredient(type, cast).orElse(null)
    }

    private fun <T> castRecipe(category: IRecipeCategory<T>, rawRecipe: Any): T? {
        val recipeClass = category.recipeType.recipeClass
        return if (recipeClass.isInstance(rawRecipe)) recipeClass.cast(rawRecipe) else null
    }

    private fun <T> readRegistryName(category: IRecipeCategory<T>, recipe: T & Any): String? = try {
        category.getRegistryName(recipe)?.toString()
    } catch (e: RuntimeException) {
        // A category that cannot name its recipe has no name; one is never fabricated.
        LOG.debug("AI Factory JEI: category " + category.recipeType.uid + " could not name its recipe", e)
        null
    }

    private fun readTitle(category: IRecipeCategory<*>): String? = try {
        val title: Component = category.title
        title.string
    } catch (e: RuntimeException) {
        LOG.debug("AI Factory JEI: category " + category.recipeType.uid + " has no readable title", e)
        null
    }

    private class BoundIngredient(val identity: IngredientIdentity, val typed: ITypedIngredient<*>)

    private class SelectedRecipe(val category: IRecipeCategory<*>, val recipe: Any)

    private class SearchState(val needle: String, val skip: Long, val wanted: Int) {
        var scanned = 0
        var matched = 0L
        var hasMore = false
        var truncated = false
        var clipped = false
        var failed = 0
        val results = ArrayList<JsonObject>(wanted)
    }

    // ---------------------------------------------------------------------------------------------
    // Small shared utilities
    // ---------------------------------------------------------------------------------------------

    /**
     * Main-thread gate. The injected assertion is expected to fail with [IllegalStateException]; that
     * failure becomes a structured reply, so a wrong-thread call never touches JEI. Anything else the
     * assertion throws propagates unchanged.
     */
    private fun guardThread(): JsonObject? = try {
        assertClientThread()
        null
    } catch (e: IllegalStateException) {
        failure(JeiQueryLimits.ERROR_WRONG_THREAD, "JEI queries must run on the client thread; nothing was queried.")
    }

    private fun failure(error: String, message: String): JsonObject = buildJsonObject {
        put("ok", false)
        put("error", error)
        put("message", message)
    }

    private fun invalid(message: String): JsonObject = failure(JeiQueryLimits.ERROR_INVALID_ARGUMENTS, message)

    private fun pageError(page: Int, pageSize: Int, maxPageSize: Int): JsonObject? = when {
        page !in 0..JeiToolLimits.MAX_PAGE -> invalid("'page' must be between 0 and ${JeiToolLimits.MAX_PAGE}.")
        pageSize !in 1..maxPageSize -> invalid("'page_size' must be between 1 and $maxPageSize.")
        else -> null
    }

    /** Canonical resource id: lowercase, namespace defaulted like the tools do; null when invalid. */
    private fun canonicalResourceId(value: String): ResourceLocation? =
        if (value.length > JeiToolLimits.MAX_ID_LENGTH) null else ResourceLocation.tryParse(value)

    private fun clipped(value: String, onClipped: () -> Unit = {}): String {
        if (value.length <= JeiQueryLimits.MAX_TEXT_LENGTH) return value
        onClipped()
        return value.substring(0, JeiQueryLimits.MAX_TEXT_LENGTH)
    }

    private fun clippedOrNull(value: String?, onClipped: () -> Unit = {}): String? = value?.let { clipped(it, onClipped) }

    private fun JsonObject.boolField(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.booleanOrNull == true

    private fun JsonObject.textField(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun echo(value: String): String {
        val text = if (value.length <= 64) value else value.substring(0, 64) + "..."
        return text.replace('\n', ' ').replace('\r', ' ')
    }

    companion object {
        private val LOG = LogUtils.getLogger()

        private const val VARIANT_BY_KEY = "exact_bound_key"
        private const val VARIANT_DEFAULT = "default_registry_stack"

        /** Must cover [JeiToolLimits.DIRECTIONS]; any other direction is rejected as invalid. */
        private val ROLE_BY_DIRECTION: Map<String, RecipeIngredientRole> = mapOf(
            "recipes" to RecipeIngredientRole.OUTPUT,
            "uses" to RecipeIngredientRole.INPUT,
            "catalyst" to RecipeIngredientRole.CATALYST,
        )

        private const val CAVEAT_SEARCH_VISIBILITY =
            "Only registered ingredients that JEI shows in its recipe context are listed; ingredients " +
                "hidden from recipes are never returned as if they were usable."
        private const val CAVEAT_KEYS_SESSION =
            "ingredient keys are valid only for the current JEI runtime (and while they stay among the " +
                "most recently used ${JeiQueryLimits.MAX_BOUND_INGREDIENTS} keys); reuse them with " +
                "'ingredient_key', stale keys answer ${JeiQueryLimits.ERROR_UNKNOWN_KEY}."
        private const val CAVEAT_SLOT_ROTATION =
            "Slot candidates are JEI's per-slot display rotation and ignore the current focus: " +
                "alternatives within a slot are mutually exclusive, alternatives of correlated slots may " +
                "be linked, and the candidates of one recipe are therefore not independent valid " +
                "combinations or a list of simultaneous inputs."
        private const val CAVEAT_NULL_PLACEHOLDERS =
            "A null candidate is a hidden or absent rotation entry (JEI never exposes " +
                "addInvisibleIngredients entries to plugins); indices stay aligned with JEI's slot " +
                "contents and the counts report what was hidden or omitted."
        private const val CAVEAT_NO_CRAFTING_AUTHORIZATION =
            "Recipes are listed only because JEI knows them for the focused ingredient: this is not " +
                "crafting authorization, item availability or a recipe transfer, and nothing is consumed."
    }
}
