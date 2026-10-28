package com.rcdiamond.aifactory.client.ui

import com.rcdiamond.aifactory.client.llm.ChatEntry
import com.rcdiamond.aifactory.client.llm.ChatMessage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Presentation-only summaries of tool calls, shared by the local chat and the shared server session.
 *
 * A tool result is a JSON observation meant for the model: showing it verbatim in the chat dumps raw
 * JSON, NBT and the model's own arguments. This helper turns one call plus its result into a bounded,
 * human readable line — an operation label, the outcome, and a few compact scalars that describe what
 * actually happened — while the model history keeps the untouched observation.
 *
 * Nothing here touches Minecraft, a provider or a conversation: the two entry points are pure
 * functions, so both runtimes and the tests use exactly the same presentation. Every user-visible
 * string is produced through the caller supplied [text] function (normally
 * `Component.translatable(key, *args).string`), which keeps this file free of game classes and makes
 * the translation keys auditable in one place.
 *
 * Facts are read from the result, never copied out of it: no JSON fragments, no NBT, no argument
 * echo, and every free-form string is bounded by [MAX_DETAIL_LENGTH].
 */
object ToolUseSummary {
    /** Longest free-form text (item name, error code, reason) kept in a summary; longer text is cut. */
    const val MAX_DETAIL_LENGTH = 96

    private const val SPEAKER_USER = "user"
    private const val SPEAKER_ASSISTANT = "assistant"
    private const val SPEAKER_TOOL = "tool"

    private const val TOOL_READ_INVENTORY = "ae2_read_inventory"
    private const val TOOL_ITEM_DETAILS = "ae2_item_details"
    private const val TOOL_CRAFTING_PLAN = "ae2_crafting_plan"
    private const val TOOL_REQUEST_CRAFTING = "ae2_request_crafting"
    private const val TOOL_MANAGE_PATTERNS = "ae2_manage_patterns"
    private const val TOOL_GIVE_ITEM = "give_item"
    private const val TOOL_EXECUTE_COMMAND = "execute_command"
    private const val TOOL_JEI_FIND_INGREDIENTS = "jei_find_ingredients"
    private const val TOOL_JEI_QUERY_RECIPES = "jei_query_recipes"
    private const val TOOL_ID_NETWORK_INFO = "id_network_info"
    private const val TOOL_ID_LIST_PARTS = "id_list_parts"
    private const val TOOL_ID_LIST_VARIABLES = "id_list_variables"

    private const val ACTION_ENCODE_INSTALL = "encode_install"

    private const val KEY_TOOL_RUNNING = "ai_factory.chat.tool_running"
    private const val KEY_NAME_OTHER = "ai_factory.tool.name.other"
    private const val KEY_NAME_UNKNOWN = "ai_factory.tool.name.unknown"
    private const val KEY_ITEM_UNKNOWN = "ai_factory.tool.item_unknown"
    private const val KEY_SUMMARY_FAILED = "ai_factory.tool.summary.failed"
    private const val KEY_SUMMARY_UNREADABLE = "ai_factory.tool.summary.unreadable"
    private const val KEY_SUMMARY_COMPLETED = "ai_factory.tool.summary.completed"
    private const val KEY_SUMMARY_INVENTORY = "ai_factory.tool.summary.inventory"
    private const val KEY_SUMMARY_ITEM_DETAILS = "ai_factory.tool.summary.item_details"
    private const val KEY_SUMMARY_ITEM_DETAILS_UNRESOLVED = "ai_factory.tool.summary.item_details_unresolved"
    private const val KEY_SUMMARY_CRAFTING_PLAN = "ai_factory.tool.summary.crafting_plan"
    private const val KEY_SUMMARY_CRAFTING_SUBMITTED = "ai_factory.tool.summary.crafting_submitted"
    private const val KEY_SUMMARY_PATTERNS_VIEW = "ai_factory.tool.summary.patterns_view"
    private const val KEY_SUMMARY_PATTERN_INSTALLED = "ai_factory.tool.summary.pattern_installed"
    private const val KEY_SUMMARY_GIVE_ITEM = "ai_factory.tool.summary.give_item"
    private const val KEY_SUMMARY_COMMAND = "ai_factory.tool.summary.command"
    private const val KEY_SUMMARY_COMMAND_TRUNCATED = "ai_factory.tool.summary.command_truncated"
    private const val KEY_SUMMARY_JEI_INGREDIENTS = "ai_factory.tool.summary.jei_ingredients"
    private const val KEY_SUMMARY_JEI_INGREDIENTS_MORE = "ai_factory.tool.summary.jei_ingredients_more"
    private const val KEY_SUMMARY_JEI_INGREDIENTS_TRUNCATED = "ai_factory.tool.summary.jei_ingredients_truncated"
    private const val KEY_SUMMARY_JEI_RECIPES = "ai_factory.tool.summary.jei_recipes"
    private const val KEY_SUMMARY_JEI_RECIPES_MORE = "ai_factory.tool.summary.jei_recipes_more"
    private const val KEY_SUMMARY_JEI_RECIPES_TRUNCATED = "ai_factory.tool.summary.jei_recipes_truncated"
    private const val KEY_SUMMARY_NO_RESULT = "ai_factory.tool.summary.no_result"
    private const val KEY_ERROR_UNKNOWN_TOOL = "ai_factory.tool.error.unknown_tool"
    private const val KEY_ERROR_INVALID_ARGUMENTS = "ai_factory.tool.error.invalid_arguments"
    private const val KEY_ERROR_TIMEOUT = "ai_factory.tool.error.timeout"
    private const val KEY_ERROR_INTERRUPTED = "ai_factory.tool.error.interrupted"
    private const val KEY_ERROR_UNKNOWN_KEY = "ai_factory.tool.error.unknown_key"
    private const val KEY_ERROR_JEI_NOT_READY = "ai_factory.tool.error.jei_not_ready"
    private const val KEY_ERROR_UNKNOWN_INGREDIENT = "ai_factory.tool.error.unknown_ingredient"
    private const val KEY_ERROR_UNKNOWN_CATEGORY = "ai_factory.tool.error.unknown_category"
    private const val KEY_ERROR_MISSING_ITEMS = "ai_factory.tool.error.missing_items"
    private const val KEY_ERROR_CRAFTING_REJECTED = "ai_factory.tool.error.crafting_rejected"
    private const val KEY_ERROR_INSTALL_FAILED = "ai_factory.tool.error.install_failed"
    private const val KEY_ERROR_COMMAND_FAILED = "ai_factory.tool.error.command_failed"
    private const val KEY_ERROR_GIVE_ITEM_FAILED = "ai_factory.tool.error.give_item_failed"
    private const val KEY_ERROR_CODE = "ai_factory.tool.error.code"
    private const val KEY_ERROR_OTHER = "ai_factory.tool.error.other"

    private val WHITESPACE = Regex("\\s+")
    private val NO_ARGS = emptyArray<Any>()

    /**
     * Status line for a call that is still running, e.g. for the placeholder entry the live runtimes
     * append on `ToolStarted`. Uses the same operation label as [finished] so the placeholder and the
     * summary describe the same call.
     */
    fun running(name: String, text: (String, Array<out Any>) -> String): String =
        text(KEY_TOOL_RUNNING, arrayOf(label(name, text)))

    /**
     * One bounded, human readable summary of a finished call.
     *
     * [name] is the model's tool name, [result] is the raw observation handed to the model. A result
     * that cannot be read as a JSON object, or one that reports an error, is summarized as such
     * instead of being copied; the raw text is never included in the output.
     */
    fun finished(name: String, result: String, text: (String, Array<out Any>) -> String): String {
        val label = label(name, text)
        val reply = parseObject(result) ?: return text(KEY_SUMMARY_UNREADABLE, arrayOf(label))
        val error = reply.stringOrNull("error")
        if (!error.isNullOrBlank()) return failed(label, error, reply, text)
        if (reply.booleanOrNull("ok") == false) return failed(label, null, reply, text)
        if (reply.booleanOrNull("success") == false) return gameActionFailed(label, name, reply, text)
        return completed(name, reply, label, text)
    }

    /**
     * Chat presentation of a stored conversation: user and assistant text are kept verbatim, and every
     * recorded tool call is folded together with its own result into one summary. A call whose result
     * never arrived is reported as such instead of being dropped or shown as a finished call, and a
     * result that has no recorded call is still reported truthfully.
     *
     * The messages themselves are only read; the model history is never modified.
     */
    fun history(messages: List<ChatMessage>, text: (String, Array<out Any>) -> String): List<ChatEntry> {
        val entries = ArrayList<ChatEntry>(messages.size)
        val pending = LinkedHashMap<String, String>()
        for (message in messages) {
            when (message) {
                is ChatMessage.User -> entries += ChatEntry(SPEAKER_USER, message.content)
                is ChatMessage.Assistant -> {
                    message.content?.takeIf { it.isNotEmpty() }?.let {
                        entries += ChatEntry(SPEAKER_ASSISTANT, it)
                    }
                    for (call in message.toolCalls) pending[call.id] = call.name
                }
                is ChatMessage.Tool -> {
                    val name = pending.remove(message.toolCallId)
                        ?: parseObject(message.content)?.stringOrNull("tool").orEmpty()
                    entries += ChatEntry(SPEAKER_TOOL, finished(name, message.content, text))
                }
            }
        }
        for (name in pending.values) {
            entries += ChatEntry(SPEAKER_TOOL, text(KEY_SUMMARY_NO_RESULT, arrayOf(label(name, text))))
        }
        return entries
    }

    // ------------------------------------------------------------------ success

    private fun completed(
        name: String,
        reply: JsonObject,
        label: String,
        text: (String, Array<out Any>) -> String,
    ): String = when (name) {
        TOOL_READ_INVENTORY -> {
            val page = (reply.longOrNull("page") ?: 0L) + 1L
            val pageCount = (reply.longOrNull("page_count") ?: 0L).coerceAtLeast(1L)
            text(
                KEY_SUMMARY_INVENTORY,
                arrayOf<Any>(label, reply.longOrNull("total_items") ?: 0L, reply.longOrNull("total_amount") ?: 0L, page, pageCount),
            )
        }
        TOOL_ITEM_DETAILS -> {
            val shown = reply["nbt"] as? JsonObject
            val key = if (shown?.booleanOrNull("found") == false) KEY_SUMMARY_ITEM_DETAILS_UNRESOLVED else KEY_SUMMARY_ITEM_DETAILS
            text(key, arrayOf<Any>(label, itemName(reply, text), reply.longOrNull("count") ?: 0L))
        }
        TOOL_CRAFTING_PLAN ->
            text(
                KEY_SUMMARY_CRAFTING_PLAN,
                arrayOf<Any>(
                    label, itemName(reply, text), reply.longOrNull("amount") ?: 0L,
                    reply.longOrNull("missing_item_kinds") ?: 0L,
                ),
            )
        TOOL_REQUEST_CRAFTING -> {
            if (reply.booleanOrNull("submitted") != true) {
                completed(TOOL_CRAFTING_PLAN, reply, label, text)
            } else {
                text(
                    KEY_SUMMARY_CRAFTING_SUBMITTED,
                    arrayOf<Any>(label, itemName(reply, text), reply.longOrNull("amount") ?: 0L),
                )
            }
        }
        TOOL_MANAGE_PATTERNS -> {
            if (reply.stringOrNull("action") == ACTION_ENCODE_INSTALL) {
                text(
                    KEY_SUMMARY_PATTERN_INSTALLED,
                    arrayOf<Any>(label, bounded(reply.stringOrNull("mode")) ?: unknown(text), reply.longOrNull("blank_patterns_remaining") ?: 0L),
                )
            } else {
                val page = (reply.longOrNull("page") ?: 0L) + 1L
                val pageCount = (reply.longOrNull("page_count") ?: 0L).coerceAtLeast(1L)
                text(
                    KEY_SUMMARY_PATTERNS_VIEW,
                    arrayOf<Any>(
                        label, reply.longOrNull("total_patterns") ?: 0L,
                        reply.longOrNull("blank_patterns") ?: 0L, page, pageCount,
                    ),
                )
            }
        }
        TOOL_GIVE_ITEM -> text(KEY_SUMMARY_GIVE_ITEM, arrayOf<Any>(label, reply.longOrNull("count") ?: 0L))
        TOOL_EXECUTE_COMMAND -> {
            val code = reply.longOrNull("result_code") ?: 0L
            val key = if (reply.booleanOrNull("output_truncated") == true) KEY_SUMMARY_COMMAND_TRUNCATED else KEY_SUMMARY_COMMAND
            text(key, arrayOf<Any>(label, code))
        }
        TOOL_JEI_FIND_INGREDIENTS -> jeiPage(
            reply,
            KEY_SUMMARY_JEI_INGREDIENTS,
            KEY_SUMMARY_JEI_INGREDIENTS_MORE,
            KEY_SUMMARY_JEI_INGREDIENTS_TRUNCATED,
            label,
            text,
        )
        TOOL_JEI_QUERY_RECIPES -> jeiPage(
            reply,
            KEY_SUMMARY_JEI_RECIPES,
            KEY_SUMMARY_JEI_RECIPES_MORE,
            KEY_SUMMARY_JEI_RECIPES_TRUNCATED,
            label,
            text,
        )
        else -> text(KEY_SUMMARY_COMPLETED, arrayOf(label))
    }

    /**
     * One JEI page: how many entries came back and which page it was, plus whether the backend had to
     * cut the page or whether more pages remain. The result's contents are never summarized.
     */
    private fun jeiPage(
        reply: JsonObject,
        key: String,
        moreKey: String,
        truncatedKey: String,
        label: String,
        text: (String, Array<out Any>) -> String,
    ): String {
        val page = (reply.longOrNull("page") ?: 0L) + 1L
        val returned = reply.longOrNull("returned") ?: 0L
        val chosen = when {
            reply.booleanOrNull("truncated") == true -> truncatedKey
            reply.booleanOrNull("has_more") == true -> moreKey
            else -> key
        }
        return text(chosen, arrayOf<Any>(label, returned, page))
    }

    // ------------------------------------------------------------------- errors

    /** The tool did not do what was asked; [error] is the code the tool itself reported, if any. */
    private fun failed(
        label: String,
        error: String?,
        reply: JsonObject,
        text: (String, Array<out Any>) -> String,
    ): String {
        val reason = when (error) {
            null -> text(KEY_ERROR_OTHER, NO_ARGS)
            "unknown_tool" -> text(KEY_ERROR_UNKNOWN_TOOL, NO_ARGS)
            "invalid_arguments" -> text(KEY_ERROR_INVALID_ARGUMENTS, NO_ARGS)
            "timeout" -> text(KEY_ERROR_TIMEOUT, NO_ARGS)
            "interrupted" -> text(KEY_ERROR_INTERRUPTED, NO_ARGS)
            "unknown_key" -> text(KEY_ERROR_UNKNOWN_KEY, NO_ARGS)
            "jei_not_ready" -> text(KEY_ERROR_JEI_NOT_READY, NO_ARGS)
            "unknown_ingredient" -> text(KEY_ERROR_UNKNOWN_INGREDIENT, NO_ARGS)
            "unknown_category" -> text(KEY_ERROR_UNKNOWN_CATEGORY, NO_ARGS)
            "missing_items" -> text(KEY_ERROR_MISSING_ITEMS, arrayOf(reply.longOrNull("missing_item_kinds") ?: 0L))
            "crafting_rejected" -> text(
                KEY_ERROR_CRAFTING_REJECTED,
                arrayOf(bounded(reply.stringOrNull("reason")) ?: unknown(text)),
            )
            "install_failed" -> text(KEY_ERROR_INSTALL_FAILED, NO_ARGS)
            else -> text(KEY_ERROR_CODE, arrayOf(bounded(error) ?: unknown(text)))
        }
        return text(KEY_SUMMARY_FAILED, arrayOf(label, reason))
    }

    /** A game action refused by the server: it reports no code, only a message that echoes the request. */
    private fun gameActionFailed(
        label: String,
        name: String,
        reply: JsonObject,
        text: (String, Array<out Any>) -> String,
    ): String {
        val reason = if (name == TOOL_EXECUTE_COMMAND) {
            text(KEY_ERROR_COMMAND_FAILED, arrayOf(reply.longOrNull("result_code") ?: 0L))
        } else {
            text(KEY_ERROR_GIVE_ITEM_FAILED, NO_ARGS)
        }
        return text(KEY_SUMMARY_FAILED, arrayOf(label, reason))
    }

    // ------------------------------------------------------------------ helpers

    /** Translated operation name, so the chat never shows the raw tool identifier for known tools. */
    private fun label(name: String, text: (String, Array<out Any>) -> String): String {
        val key = when (name) {
            TOOL_READ_INVENTORY, TOOL_ITEM_DETAILS, TOOL_CRAFTING_PLAN, TOOL_REQUEST_CRAFTING,
            TOOL_MANAGE_PATTERNS, TOOL_GIVE_ITEM, TOOL_EXECUTE_COMMAND,
            TOOL_JEI_FIND_INGREDIENTS, TOOL_JEI_QUERY_RECIPES,
            TOOL_ID_NETWORK_INFO, TOOL_ID_LIST_PARTS, TOOL_ID_LIST_VARIABLES -> "ai_factory.tool.name.$name"
            else -> null
        }
        if (key != null) return text(key, NO_ARGS)
        val bounded = bounded(name)
        return if (bounded == null) text(KEY_NAME_UNKNOWN, NO_ARGS) else text(KEY_NAME_OTHER, arrayOf(bounded))
    }

    /** Display name of the item a result is about, with the bare id as a fallback. */
    private fun itemName(reply: JsonObject, text: (String, Array<out Any>) -> String): String =
        bounded(reply.stringOrNull("name")) ?: bounded(reply.stringOrNull("id")) ?: unknown(text)

    private fun unknown(text: (String, Array<out Any>) -> String): String = text(KEY_ITEM_UNKNOWN, NO_ARGS)

    /** Parses [result] as a JSON object, or null when it is not one; never throws. */
    private fun parseObject(result: String): JsonObject? = try {
        Json.parseToJsonElement(result) as? JsonObject
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun JsonObject.stringOrNull(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.longOrNull(name: String): Long? = (this[name] as? JsonPrimitive)?.longOrNull

    private fun JsonObject.booleanOrNull(name: String): Boolean? = (this[name] as? JsonPrimitive)?.booleanOrNull

    /** Collapses whitespace and truncates, so a hostile or verbose result cannot grow the chat line. */
    private fun bounded(value: String?): String? {
        val collapsed = value?.replace(WHITESPACE, " ")?.trim().orEmpty()
        if (collapsed.isEmpty()) return null
        if (collapsed.length <= MAX_DETAIL_LENGTH) return collapsed
        return collapsed.take(MAX_DETAIL_LENGTH - 1) + "\u2026"
    }
}
