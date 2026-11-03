package com.jericx.trainr.domain.unstuck.intent

import java.text.Normalizer

object NoteText {

    // Every token costs at least one code point, so a note of at most this many
    // code points can never exceed the grammar's token cap in any script.
    const val CHARACTER_LIMIT = 200

    private val SEPARATORS = intArrayOf(
        0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x0020, 0x0085, 0x00A0,
        0x1680, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000
    )

    private val SEPARATOR_RANGES = arrayOf(0x2000..0x200A)

    private val STANDALONE_RANGES = arrayOf(
        0x3000..0x303F,
        0x3040..0x30FF,
        0x31F0..0x31FF,
        0x3400..0x4DBF,
        0x4E00..0x9FFF,
        0xF900..0xFAFF,
        0xFF01..0xFF9F,
        0x20000..0x2FA1F
    )

    fun length(note: String): Int = note.codePointCount(0, note.length)

    fun isWithinLimit(note: String): Boolean = length(note) <= CHARACTER_LIMIT

    fun tokens(note: String): List<String> {
        val text = Normalizer.normalize(note, Normalizer.Form.NFC)
        val tokens = mutableListOf<String>()
        val pending = StringBuilder()
        var index = 0
        while (index < text.length) {
            val point = text.codePointAt(index)
            val width = Character.charCount(point)
            when {
                isSeparator(point) -> pending.flushInto(tokens)
                isStandalone(point) -> {
                    pending.flushInto(tokens)
                    tokens += text.substring(index, index + width)
                }

                else -> pending.appendCodePoint(point)
            }
            index += width
        }
        pending.flushInto(tokens)
        return tokens
    }

    // The grammar rebuilds a quote by joining tokens with one ASCII space, which
    // a space-free script never wrote and a line break never spelled that way.
    fun canonical(text: String): String {
        val builder = StringBuilder()
        for (token in tokens(text)) {
            if (builder.isNotEmpty() && !joinsWithoutSpace(builder, token)) builder.append(' ')
            builder.append(token)
        }
        return builder.toString()
    }

    private fun joinsWithoutSpace(before: StringBuilder, next: String): Boolean =
        isStandalone(before.codePointBefore(before.length)) || isStandalone(next.codePointAt(0))

    private fun StringBuilder.flushInto(tokens: MutableList<String>) {
        if (isNotEmpty()) {
            tokens += toString()
            setLength(0)
        }
    }

    private fun isSeparator(point: Int): Boolean =
        point in SEPARATORS || SEPARATOR_RANGES.any { point in it }

    private fun isStandalone(point: Int): Boolean = STANDALONE_RANGES.any { point in it }
}
