package com.ehealthinformatics.prognocare.feature.chat

import com.ehealthinformatics.prognocare.data.remote.models.ExchangeMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Structured QUESTION envelope (clerking plan task 6.1).
 *
 * When the engine attaches `Exchange.payload.question`, the chat UI renders
 * from this structure instead of re-parsing the flattened text. Options carry
 * `key` (what is sent back as the ANSWER) and `label` (what the user sees).
 * When the message has no structured payload, [from] degrades to the existing
 * [ChatOptionParser] text contract — the UI has a single render path either way.
 *
 * Shapes accepted (defensive):
 *   payload = { question: { id, attribute, text, options: [{key,label}],
 *             selectionMode: 'SINGLE'|'MULTI', allowNone: bool,
 *             progress: {index, total} } }
 *   payload = { id, attribute, text, options: [...] }   (question at top level)
 */
data class ChatQuestionOption(val key: String, val label: String)

enum class ChatSelectionMode { SINGLE, MULTI }

data class ChatQuestionEnvelope(
    val questionId: String?,
    val attribute: String?,
    val title: String,
    val options: List<ChatQuestionOption>,
    /** Structured payload originated from the wire (vs text-parse fallback). */
    val isStructured: Boolean,
    /** MULTI lets the user batch-select several options before sending. */
    val selectionMode: ChatSelectionMode,
    /** A "None of these" affordance is offered (completes without a value). */
    val allowNone: Boolean,
    val progressIndex: Int?,
    val progressTotal: Int?,
) {
    val isMultiSelect: Boolean get() = selectionMode == ChatSelectionMode.MULTI

    companion object {
        /** Sentinel value sent when the user completes a multi-iterative question with NONE. */
        const val NONE_SENTINEL = "NONE"

        private val json = Json { ignoreUnknownKeys = true }

        fun from(message: ExchangeMessage): ChatQuestionEnvelope? {
            // 1) Structured payload — preferred when present.
            structured(message.payload)?.takeIf { it.options.isNotEmpty() }?.let { return it }

            // 2) Text fallback — the engine flattens options into the message
            //    body (`title\nkey: label…\u200B`); reuse the existing parser.
            val onlyParseOthers = message.direction != "inbound"
            if (!onlyParseOthers) return null
            return ChatOptionParser.parse(message.text)?.let { parsed ->
                ChatQuestionEnvelope(
                    questionId = message.questionId,
                    attribute = message.attribute,
                    title = parsed.title,
                    options = parsed.options.map { ChatQuestionOption(it.value, it.label) },
                    isStructured = false,
                    selectionMode = ChatSelectionMode.SINGLE,
                    allowNone = false,
                    progressIndex = null,
                    progressTotal = null,
                )
            }
        }

        /** Extract an envelope from a raw payload element; null when absent/malformed. */
        fun structured(payload: JsonElement?): ChatQuestionEnvelope? {
            val obj = (payload as? JsonObject) ?: return null
            val question = obj["question"]?.jsonObject ?: obj
            val rawOptions = runCatching { question["options"]!!.jsonArray }.getOrNull()
                ?: return null
            val options = rawOptions.mapNotNull { el ->
                when (el) {
                    is JsonObject -> {
                        val key = (el["key"] ?: el["value"])?.jsonPrimitive?.content
                        val label = el["label"]?.jsonPrimitive?.content
                            ?: (el["name"] as? JsonPrimitive)?.content
                        if (key.isNullOrBlank()) null
                        else ChatQuestionOption(key, label ?: key)
                    }
                    is JsonPrimitive -> ChatQuestionOption(el.content, el.content)
                    else -> null
                }
            }
            if (options.isEmpty()) return null

            val progress = question["progress"] as? JsonObject
            val selectionModeRaw = (question["selectionMode"] as? JsonPrimitive)?.content
                ?: (question["processMode"] as? JsonPrimitive)?.content.orEmpty()
            val selectionMode = if (selectionModeRaw.uppercase().contains("ITERATIVE") ||
                selectionModeRaw.uppercase().contains("MULTI")
            ) ChatSelectionMode.MULTI else ChatSelectionMode.SINGLE

            return ChatQuestionEnvelope(
                questionId = (question["id"] as? JsonPrimitive)?.content,
                attribute = (question["attribute"] as? JsonPrimitive)?.content,
                title = ((question["text"] as? JsonPrimitive)?.content
                    ?: (question["title"] as? JsonPrimitive)?.content
                    ?: (obj["title"] as? JsonPrimitive)?.content).orEmpty(),
                options = options,
                isStructured = true,
                selectionMode = selectionMode,
                allowNone = (question["allowNone"] as? JsonPrimitive)?.booleanOrNull ?: false,
                progressIndex = progress?.get("index")?.jsonPrimitive?.intOrNull,
                progressTotal = progress?.get("total")?.jsonPrimitive?.intOrNull,
            )
        }
    }
}
