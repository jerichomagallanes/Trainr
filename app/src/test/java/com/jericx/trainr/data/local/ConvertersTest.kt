package com.jericx.trainr.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `round-trip preserves multi-element list`() {
        // Arrange
        val original = listOf("DUMBBELLS", "BARBELL", "BENCH")

        // Act
        val restored = converters.fromStringList(converters.fromListString(original))

        // Assert
        assertThat(restored).containsExactlyElementsIn(original).inOrder()
    }

    @Test
    fun `round-trip preserves empty list`() {
        // Arrange
        val original = emptyList<String>()

        // Act
        val restored = converters.fromStringList(converters.fromListString(original))

        // Assert
        assertThat(restored).isEmpty()
    }

    @Test
    fun `round-trip preserves strings containing commas and quotes`() {
        // Arrange: real injury text could contain punctuation
        val original = listOf("Lower back, lumbar", "Right \"meniscus\" tear")

        // Act
        val restored = converters.fromStringList(converters.fromListString(original))

        // Assert
        assertThat(restored).containsExactlyElementsIn(original).inOrder()
    }

    @Test
    fun `fromListString produces valid JSON array`() {
        // Arrange / Act
        val json = converters.fromListString(listOf("a", "b"))

        // Assert
        assertThat(json).isEqualTo("[\"a\",\"b\"]")
    }
}
