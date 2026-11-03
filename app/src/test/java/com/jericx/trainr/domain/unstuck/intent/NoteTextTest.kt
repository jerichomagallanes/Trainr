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
    fun theLimitIsCountedInCodePointsNotUtf16Units() {
        assertThat(NoteText.length("🏋")).isEqualTo(1)
        assertThat(NoteText.isWithinLimit("a".repeat(NoteText.CHARACTER_LIMIT))).isTrue()
        assertThat(NoteText.isWithinLimit("🏋".repeat(NoteText.CHARACTER_LIMIT))).isTrue()
        assertThat(NoteText.isWithinLimit("a".repeat(NoteText.CHARACTER_LIMIT + 1))).isFalse()
    }

    // The grammar keeps 200 tokens, so the character limit has to be the point
    // at which the worst script can still reach every one of them.
    @Test
    fun aNoteAtTheLimitCanNeverOutrunTheGrammar() {
        val japanese = "分".repeat(NoteText.CHARACTER_LIMIT)
        val english = "ab ".repeat(NoteText.CHARACTER_LIMIT)

        assertThat(NoteText.tokens(japanese)).hasSize(NoteText.CHARACTER_LIMIT)
        assertThat(NoteText.CHARACTER_LIMIT).isAtMost(IntentGrammar.TOKEN_LIMIT)
        assertThat(NoteText.tokens(english.take(NoteText.CHARACTER_LIMIT)).size)
            .isAtMost(IntentGrammar.TOKEN_LIMIT)
    }
}
