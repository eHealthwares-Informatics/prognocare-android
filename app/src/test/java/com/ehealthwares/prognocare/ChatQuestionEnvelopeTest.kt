package com.ehealthwares.prognocare

import com.ehealthwares.prognocare.data.remote.models.ExchangeMessage
import com.ehealthwares.prognocare.feature.chat.ChatQuestionEnvelope
import com.ehealthwares.prognocare.feature.chat.ChatSelectionMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6.1 — structured Exchange.payload QUESTION envelope parsing.
 */
class ChatQuestionEnvelopeTest {

    private fun messageWithPayload(payloadJson: String) = ExchangeMessage(
        id = "ex-1",
        conversationId = "c1",
        direction = "outbound",
        text = "Should not be needed",
        payload = Json.parseToJsonElement(payloadJson),
    )

    @Test
    fun `parses structured question payload`() {
        val message = messageWithPayload(
            """
            {"question": {
              "id": "q-symptoms",
              "attribute": "symptoms",
              "text": "Which symptoms do you have?",
              "options": [
                {"key": "HEADACHE", "label": "Headache"},
                {"key": "FEVER", "label": "Fever"}
              ],
              "selectionMode": "MULTI",
              "allowNone": true,
              "progress": {"index": 2, "total": 5}
            }}
            """.trimIndent(),
        )
        val envelope = ChatQuestionEnvelope.from(message)
        assertNotNull(envelope)
        assertTrue(envelope!!.isStructured)
        assertEquals("q-symptoms", envelope.questionId)
        assertEquals("symptoms", envelope.attribute)
        assertEquals("Which symptoms do you have?", envelope.title)
        assertEquals(2, envelope.options.size)
        assertEquals("FEVER", envelope.options[1].key)
        assertEquals(ChatSelectionMode.MULTI, envelope.selectionMode)
        assertTrue(envelope.isMultiSelect)
        assertTrue(envelope.allowNone)
        assertEquals(2, envelope.progressIndex)
        assertEquals(5, envelope.progressTotal)
    }

    @Test
    fun `accepts question at top level of payload`() {
        val message = messageWithPayload(
            """
            {"id": "q-2", "attribute": "complaint", "text": "What is wrong?",
             "options": [{"key": "A1", "label": "Acne"}]}
            """.trimIndent(),
        )
        val envelope = ChatQuestionEnvelope.from(message)
        assertNotNull(envelope)
        assertTrue(envelope!!.isStructured)
        assertEquals("A1", envelope.options[0].key)
        assertEquals(ChatSelectionMode.SINGLE, envelope.selectionMode)
        assertFalse(envelope.allowNone)
        assertNull(envelope.progressIndex)
    }

    @Test
    fun `falls back to text parser when payload absent`() {
        val message = ExchangeMessage(
            id = "ex-2",
            direction = "outbound",
            text = "Select an action\u200B\n1: Book appointment\n2: Ask a pharmacist",
        )
        val envelope = ChatQuestionEnvelope.from(message)
        assertNotNull(envelope)
        assertFalse(envelope!!.isStructured)
        // The engine appends the \u200B sentinel to option-bearing text; the
        // envelope keeps the raw first line (UI already strips the sentinel).
        assertEquals("Select an action\u200B", envelope.title)
        assertEquals(2, envelope.options.size)
        assertEquals("1", envelope.options[0].key)
        assertEquals("Book appointment", envelope.options[0].label)
    }

    @Test
    fun `returns null for inbound messages even with text options`() {
        val message = ExchangeMessage(
            id = "ex-3",
            direction = "inbound",
            text = "Select\n1: One\n2: Two",
        )
        assertNull(ChatQuestionEnvelope.from(message))
    }

    @Test
    fun `malformed payload options degrade to text parsing`() {
        val message = messageWithPayload("""{"question": {"options": "not-an-array"}}""")
        // Text is not parseable either → null.
        assertNull(ChatQuestionEnvelope.from(message))
    }

    @Test
    fun `multi-select sentinel NONE maps to constant`() {
        assertEquals("NONE", ChatQuestionEnvelope.NONE_SENTINEL)
    }

    @Test
    fun `optionless structured payload returns null and falls back`() {
        val message = ExchangeMessage(
            id = "ex-4",
            direction = "outbound",
            text = "Simple note",
            payload = JsonPrimitive("""{"question":{"id":"q9","options":[]}}""".let { it }),
        )
        assertNull(ChatQuestionEnvelope.from(message))
    }
}
