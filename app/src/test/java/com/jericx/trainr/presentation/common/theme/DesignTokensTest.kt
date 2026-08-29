package com.jericx.trainr.presentation.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DesignTokensTest {

    @Test
    fun brandAndNeutralsMatchTheFigmaValues() {
        assertThat(Orange500).isEqualTo(Color(0xFFD37200))
        assertThat(Slate800).isEqualTo(Color(0xFF243036))
        assertThat(OutlineGray).isEqualTo(Color(0xFFB0BEC5))
        assertThat(TextMuted).isEqualTo(Color(0xFF626262))
        assertThat(DividerGray).isEqualTo(Color(0xFFD9D9D9))
    }

    @Test
    fun statusColoursMatchTheFigmaValues() {
        assertThat(StatusCompleted).isEqualTo(Color(0xFF5F8C32))
        assertThat(StatusInProgress).isEqualTo(Color(0xFFD37200))
        assertThat(StatusNotStarted).isEqualTo(Color(0xFF626262))
    }

    @Test
    fun cornersMatchTheFigmaValues() {
        assertThat(Shapes.small).isEqualTo(RoundedCornerShape(8.dp))
        assertThat(Shapes.medium).isEqualTo(RoundedCornerShape(10.dp))
        assertThat(Shapes.large).isEqualTo(RoundedCornerShape(10.dp))
    }

    @Test
    fun spacingCoversTheStepsTheDesignUses() {
        assertThat(Spacing.tight).isEqualTo(10.dp)
        assertThat(Spacing.card).isEqualTo(15.dp)
        assertThat(Spacing.screen).isEqualTo(20.dp)
        assertThat(Spacing.section).isEqualTo(30.dp)
    }

    // The in-progress chip and the primary action are the same orange in the
    // design; keeping them as one token stops them drifting apart.
    @Test
    fun inProgressReusesTheBrandOrange() {
        assertThat(StatusInProgress).isEqualTo(Orange500)
    }
}
