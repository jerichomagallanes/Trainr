package com.jericx.trainr.domain.unstuck.intent

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// The same vectors are pinned on iOS. A token list that differs by one
// character gives the two apps two different grammars for one note.
class NoteTextTest {

    @Test
    fun anEnglishNoteSplitsOnItsSpaces() {
        assertThat(NoteText.tokens("I have 35 minutes for the whole workout today."))
            .containsExactly("I", "have", "35", "minutes", "for", "the", "whole", "workout", "today.")
            .inOrder()
    }

    @Test
    fun aJapaneseNoteSplitsPerCharacter() {
        assertThat(NoteText.tokens("今日は30分しかありません。"))
            .containsExactly("今", "日", "は", "30", "分", "し", "か", "あ", "り", "ま", "せ", "ん", "。")
            .inOrder()
    }

    @Test
    fun aChineseNoteSplitsPerCharacter() {
        assertThat(NoteText.tokens("我今天只有三十分钟。"))
            .containsExactly("我", "今", "天", "只", "有", "三", "十", "分", "钟", "。")
            .inOrder()
    }

    @Test
    fun katakanaAndLatinInOneNoteEachKeepTheirOwnRule() {
        assertThat(NoteText.tokens("ベンチプレス bench press"))
            .containsExactly("ベ", "ン", "チ", "プ", "レ", "ス", "bench", "press")
            .inOrder()
    }

    @Test
    fun everySeparatorEndsAToken() {
        assertThat(NoteText.tokens("a\tb\nc\u000Bd\u000Ce\rf g\u0085h i j"))
            .containsExactly("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
            .inOrder()
        assertThat(NoteText.tokens("a b c d e f g　h"))
            .containsExactly("a", "b", "c", "d", "e", "f", "g", "h")
            .inOrder()
    }

    @Test
    fun aNoteWithNoWordsHasNoTokens() {
        assertThat(NoteText.tokens("")).isEmpty()
        assertThat(NoteText.tokens("  \n ")).isEmpty()
    }

    @Test
    fun aCombiningMarkIsComposedBeforeSplitting() {
        assertThat(NoteText.tokens("café rack")).containsExactly("café", "rack").inOrder()
    }

    @Test
    fun runsOfSeparatorsCollapseToOneSpace() {
        assertThat(NoteText.canonical("30  minutes\nonly")).isEqualTo("30 minutes only")
        assertThat(NoteText.canonical("I have 35 minutes.")).isEqualTo("I have 35 minutes.")
    }

    @Test
    fun aSpaceBesideASpaceFreeCharacterIsDropped() {
        assertThat(NoteText.canonical("今日は30分しかありません。"))
            .isEqualTo("今日は30分しかありません。")
        assertThat(NoteText.canonical("30 分")).isEqualTo("30分")
        assertThat(NoteText.canonical("ベンチプレス bench press"))
            .isEqualTo("ベンチプレスbench press")
    }

    @Test
    fun theLimitIsCountedInTokensRatherThanCharacters() {
        assertThat(NoteText.length("🏋")).isEqualTo(1)
        assertThat(NoteText.length("30 minutes\nno rack")).isEqualTo(4)
        assertThat(NoteText.isWithinLimit("分".repeat(NoteText.TOKEN_LIMIT))).isTrue()
        assertThat(NoteText.isWithinLimit("分".repeat(NoteText.TOKEN_LIMIT + 1))).isFalse()
    }

    // The grammar can quote only its first 200 tokens, so a note that stays
    // inside the limit can always be quoted whole, whatever its script.
    @Test
    fun aNoteAtTheLimitCanNeverOutrunTheGrammar() {
        val japanese = "分".repeat(NoteText.TOKEN_LIMIT)
        val english = "ab ".repeat(NoteText.TOKEN_LIMIT)

        assertThat(NoteText.tokens(japanese)).hasSize(NoteText.TOKEN_LIMIT)
        assertThat(NoteText.tokens(english)).hasSize(NoteText.TOKEN_LIMIT)
        assertThat(NoteText.isWithinLimit(english)).isTrue()
    }

    // A sentence of ordinary English runs well past 200 characters long before
    // it runs out of tokens, and version/2.0 read notes that long.
    @Test
    fun anEnglishSentenceLongerThanTwoHundredCharactersIsStillReadable() {
        val note = "The gym is packed tonight so the squat rack and both benches are taken. " +
            "I have about forty minutes before it closes and my lower back is still sore " +
            "from Monday, so please keep it light and skip the heavy sets."

        assertThat(note.length).isGreaterThan(200)
        assertThat(NoteText.isWithinLimit(note)).isTrue()
    }
}
