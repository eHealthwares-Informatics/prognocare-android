package com.ehealthwares.prognocare

import com.ehealthwares.prognocare.feature.chat.ChatOptionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChatOptionParserTest {

    @Test
    fun `parses title with value label lines`() {
        val parsed = ChatOptionParser.parse(
            "What is the reason for your visit?\n" +
                "1: Cough\n" +
                "2: Fever\n" +
                "3: Headache"
        )
        assertNotNull(parsed)
        assertEquals("What is the reason for your visit?", parsed!!.title)
        assertEquals(3, parsed.options.size)
        assertEquals("Cough", parsed.options[0].label)
        assertEquals("1", parsed.options[0].value)
    }

    @Test
    fun `returns null for single line`() {
        assertNull(ChatOptionParser.parse("Just a plain message"))
    }

    @Test
    fun `returns null for lines without colons`() {
        assertNull(ChatOptionParser.parse("Title line\nno colon here\nanother line"))
    }

    @Test
    fun `treats plain two-line message without options format as null`() {
        assertNull(ChatOptionParser.parse("Line one\nLine two"))
    }

    @Test
    fun `handles zero width spaces`() {
        val parsed = ChatOptionParser.parse(
            "Choose an option\n" +
                "\u200B1: Option A\n" +
                "\u200B2: Option B"
        )
        assertNotNull(parsed)
        assertEquals(2, parsed!!.options.size)
        assertEquals("Option A", parsed.options[0].label)
    }

    @Test
    fun `blank leading lines are filtered like web parser`() {
        val parsed = ChatOptionParser.parse("\n1: A\n2: B")
        assertNotNull(parsed)
        assertEquals("1: A", parsed!!.title)
        assertEquals("B", parsed.options[0].label)
    }
}