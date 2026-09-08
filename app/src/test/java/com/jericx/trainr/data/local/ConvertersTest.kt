package com.jericx.trainr.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `round-trip preserves multi-element list`() {
        val original = listOf("DUMBBELLS", "BARBELL", "BENCH")

        val restored = converters.fromStringList(converters.fromListString(original))

        assertThat(restored).containsExactlyElementsIn(original).inOrder()
    }

    @Test
    fun `round-trip preserves empty list`() {
        val original = emptyList<String>()

        val restored = converters.fromStringList(converters.fromListString(original))

        assertThat(restored).isEmpty()
    }

    @Test
    fun `round-trip preserves strings containing commas and quotes`() {
        val original = listOf("Lower back, lumbar", "Right \"meniscus\" tear")

        val restored = converters.fromStringList(converters.fromListString(original))

        assertThat(restored).containsExactlyElementsIn(original).inOrder()
    }

    @Test
    fun `fromListString produces valid JSON array`() {
        val json = converters.fromListString(listOf("a", "b"))

        assertThat(json).isEqualTo("[\"a\",\"b\"]")
    }
}
