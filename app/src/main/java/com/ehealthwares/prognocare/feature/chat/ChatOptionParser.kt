package com.ehealthwares.prognocare.feature.chat

data class ParsedOption(val value: String, val label: String)

data class ParsedQuestionOptions(val title: String, val options: List<ParsedOption>)

/**
 * Port of the web chat-ui `parse-options.ts` parser.
 *
 * Detects interactive option messages formatted as:
 * ```
 * <title>
 * <value>: <label>
 * <value>: <label>
 * ```
 * Requires at least two lines and every non-title line to match
 * `value: label`, otherwise returns null (plain message).
 */
object ChatOptionParser {

    private val OPTION_LINE_RE = Regex("""^\s*([^:\s][^:]*?)\s*:\s*(.+?)\s*$""")

    fun parse(text: String): ParsedQuestionOptions? {
        val lines = text.split('\n').filter { it.isNotBlank() }
        if (lines.size < 2) return null

        val title = lines[0].trim()
        if (title.isEmpty()) return null

        val options = mutableListOf<ParsedOption>()
        for (i in 1 until lines.size) {
            val raw = lines[i].replace("\u200B", "").trim()
            val m = OPTION_LINE_RE.find(raw) ?: return null
            options.add(ParsedOption(value = m.groupValues[1].trim(), label = m.groupValues[2].trim()))
        }

        if (options.isEmpty()) return null
        return ParsedQuestionOptions(title = title, options = options)
    }
}