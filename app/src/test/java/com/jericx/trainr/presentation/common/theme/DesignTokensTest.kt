package com.jericx.trainr.presentation.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
        assertThat(LightTrainrColors.statusDone).isEqualTo(Color(0xFF5F8C32))
        assertThat(LightTrainrColors.statusActive).isEqualTo(Color(0xFFD37200))
        assertThat(LightTrainrColors.statusIdle).isEqualTo(Color(0xFF626262))
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

    // Light stays on the Figma values even where they miss AA; iOS mirrors them.
    // Raising contrast belongs in the dark half of the token, never here.
    @Test
    fun lightTokensStayOnTheFigmaValues() {
        with(LightTrainrColors) {
            assertThat(surfacePage).isEqualTo(Color.White)
            assertThat(surfaceCard).isEqualTo(Color.White)
            assertThat(surfaceRaised).isEqualTo(Color.White)
            assertThat(surfaceSunken).isEqualTo(Gray100)
            assertThat(surfaceSelected).isEqualTo(Slate800)
            assertThat(onSurfaceSelected).isEqualTo(Color.White)
            assertThat(surfaceEmphasis).isEqualTo(Slate800)
            assertThat(onSurfaceEmphasis).isEqualTo(Color.White)
            assertThat(onSurface).isEqualTo(Slate800)
            assertThat(onSurfaceMuted).isEqualTo(TextMuted)
            assertThat(outlineControl).isEqualTo(OutlineGray)
            assertThat(statusDoneEdge).isEqualTo(StatusCompleted)
            assertThat(outlineDivider).isEqualTo(DividerGray)
            assertThat(raisedEdge).isEqualTo(Color.Transparent)
            assertThat(accentRule).isEqualTo(Color.Transparent)
            assertThat(focus).isEqualTo(Orange500)
            assertThat(trackEmpty).isEqualTo(OutlineGray)
            assertThat(brand).isEqualTo(Orange500)
            assertThat(brandStrong).isEqualTo(Orange500)
            assertThat(onBrand).isEqualTo(Color.White)
            assertThat(dangerInk).isEqualTo(RedError)
            assertThat(danger).isEqualTo(RedError)
            assertThat(onDanger).isEqualTo(Color.White)
            assertThat(statusDone).isEqualTo(StatusCompleted)
            assertThat(statusDoneInk).isEqualTo(StatusCompleted)
            assertThat(statusActive).isEqualTo(Orange500)
            assertThat(statusIdle).isEqualTo(TextMuted)
            assertThat(onStatus).isEqualTo(Color.White)
            assertThat(shadowSpotBrand).isEqualTo(Orange500.copy(alpha = 0.15f))
            assertThat(scrim).isEqualTo(Color.Black.copy(alpha = 0.3f))
        }
    }

    @Test
    fun darkStatusFillsCarryAWhiteLabelOnACard() {
        with(DarkTrainrColors) {
            assertThat(contrast(onStatus, statusDone)).isAtLeast(4.5)
            assertThat(contrast(onStatus, statusActive)).isAtLeast(4.5)
            assertThat(contrast(onStatus, statusIdle)).isAtLeast(4.5)
            assertThat(contrast(statusDoneInk, surfaceCard)).isAtLeast(4.5)

            assertThat(contrast(statusDone, surfaceCard)).isAtLeast(3.0)
            assertThat(contrast(statusActive, surfaceCard)).isAtLeast(3.0)
            assertThat(contrast(statusIdle, surfaceCard)).isAtLeast(3.0)
        }
    }

    // Every added token's light value is the constant, or the exact composite,
    // that its site rendered before the token existed. Light does not move.
    @Test
    fun addedTokensKeepTheLightValuesTheirSitesRendered() {
        with(LightTrainrColors) {
            assertThat(placeholder).isEqualTo(Color(0xFFA1A1A1))
            assertThat(surfacePanel).isEqualTo(SurfaceLight)
            assertThat(onSurfaceStrong).isEqualTo(Color.Black)
            assertThat(cardEdge).isEqualTo(Slate800)
            assertThat(cardRule).isEqualTo(Slate800)
            assertThat(brandOnSelected).isEqualTo(Orange500)
            assertThat(shadowSpot).isEqualTo(Color.Black)
            assertThat(shadowSpotSoft).isEqualTo(Color.Black.copy(alpha = 0.05f))
        }
    }

    @Test
    fun darkInkTokensClearAaOnEveryGroundTheyLandOn() {
        with(DarkTrainrColors) {
            assertThat(contrast(placeholder, surfaceCard)).isAtLeast(4.5)
            assertThat(contrast(placeholder, surfacePage)).isAtLeast(4.5)
            assertThat(contrast(onSurfaceStrong, surfacePage)).isAtLeast(4.5)
            assertThat(contrast(onSurfaceStrong, surfaceCard)).isAtLeast(4.5)
            assertThat(contrast(onSurfaceStrong, surfaceRaised)).isAtLeast(4.5)
            assertThat(contrast(onSurfaceStrong, surfaceSunken)).isAtLeast(4.5)
        }
    }

    @Test
    fun darkEdgesAndBrandTintsClearTheNonTextFloor() {
        with(DarkTrainrColors) {
            assertThat(contrast(brandOnSelected, surfaceSelected)).isAtLeast(3.0)
            assertThat(contrast(cardEdge, surfacePage)).isAtLeast(3.0)
            assertThat(contrast(cardEdge, surfaceCard)).isAtLeast(3.0)
            assertThat(contrast(statusDoneEdge, surfaceCard)).isAtLeast(3.0)
            assertThat(contrast(statusDoneEdge, surfacePage)).isAtLeast(3.0)
        }
    }

    // A decorative rule is exempt from 1.4.11, so this is a perceptibility
    // floor and not a WCAG one. Kept apart from the test above so the named
    // threshold there stays honest.
    @Test
    fun theDarkDecorativeRuleStaysVisible() {
        assertThat(contrast(DarkTrainrColors.cardRule, DarkTrainrColors.surfacePage))
            .isAtLeast(1.5)
    }

    // The completed card should mark itself, not shout. Its edge must not read
    // louder than an ordinary card edge.
    @Test
    fun theCompletedCardEdgeIsNoLouderThanANeutralOne() {
        with(DarkTrainrColors) {
            assertThat(contrast(statusDoneEdge, surfaceCard))
                .isLessThan(contrast(cardEdge, surfaceCard) + 0.5)
        }
    }

    // The four grounds every ink tier can land on, which is the method the
    // colour specification set and only half of it was covered.
    @Test
    fun everyDarkInkTierClearsAaOnAllFourGrounds() {
        with(DarkTrainrColors) {
            listOf(surfacePage, surfaceCard, surfaceRaised, surfaceSunken).forEach { ground ->
                assertThat(contrast(onSurface, ground)).isAtLeast(4.5)
                assertThat(contrast(onSurfaceMuted, ground)).isAtLeast(4.5)
                assertThat(contrast(brandStrong, ground)).isAtLeast(4.5)
                assertThat(contrast(dangerInk, ground)).isAtLeast(4.5)
            }
            assertThat(contrast(onBrand, brandStrong)).isAtLeast(4.5)
        }
    }

    // Non-text needs 3:1 wherever it lands, and brand is never a text ground.
    @Test
    fun everyDarkNonTextTokenClearsItsFloorOnAllFourGrounds() {
        with(DarkTrainrColors) {
            listOf(surfacePage, surfaceCard, surfaceRaised, surfaceSunken).forEach { ground ->
                assertThat(contrast(brand, ground)).isAtLeast(3.0)
                assertThat(contrast(outlineControl, ground)).isAtLeast(3.0)
            }
        }
    }

    // The alpha-over-an-assumed-white-page leak: a dark inset panel has to stay
    // lighter than the page under it, never darker like a 50% fill goes.
    @Test
    fun theDarkPanelStaysLighterThanThePageItSitsOn() {
        with(DarkTrainrColors) {
            assertThat(surfacePanel.luminance().toDouble())
                .isGreaterThan(surfacePage.luminance().toDouble())
            assertThat(surfacePanel.luminance().toDouble())
                .isAtLeast(surfaceCard.luminance().toDouble())
            assertThat(contrast(surfacePanel, surfacePage)).isAtLeast(1.2)
        }
    }

    // A black shadow on a near-black page renders nothing but a dirty edge.
    @Test
    fun darkShadowSpotsNeverDraw() {
        with(DarkTrainrColors) {
            assertThat(shadowSpot.alpha).isEqualTo(0f)
            assertThat(shadowSpotSoft.alpha).isEqualTo(0f)
            assertThat(shadowSpotBrand.alpha).isEqualTo(0f)
        }
    }

    // The method, not a spot check: every ink is measured on all four grounds,
    // which is where the placeholder and brand-on-selected holes were hiding.
    @Test
    fun darkInkClearsAaOnAllFourGrounds() {
        with(DarkTrainrColors) {
            val grounds = listOf(surfacePage, surfaceCard, surfaceRaised, surfaceSunken)
            val inks = listOf(onSurface, onSurfaceMuted, brandStrong, dangerInk, statusDoneInk)
            for (ink in inks) {
                for (ground in grounds) {
                    assertThat(contrast(ink, ground)).isAtLeast(4.5)
                }
            }
        }
    }

    @Test
    fun darkNonTextClearsThreeOnEveryGroundItLandsOn() {
        with(DarkTrainrColors) {
            val grounds = listOf(surfacePage, surfaceCard, surfaceRaised, surfaceSunken)
            for (ground in grounds) {
                assertThat(contrast(brand, ground)).isAtLeast(3.0)
                assertThat(contrast(outlineControl, ground)).isAtLeast(3.0)
                assertThat(contrast(focus, ground)).isAtLeast(3.0)
            }
            assertThat(contrast(brandOnSelected, surfaceSelected)).isAtLeast(3.0)
        }
    }

    @Test
    fun darkKnockedOutTextClearsAaOnItsOwnFill() {
        with(DarkTrainrColors) {
            assertThat(contrast(onSurfaceSelected, surfaceSelected)).isAtLeast(4.5)
            assertThat(contrast(onSurfaceEmphasis, surfaceEmphasis)).isAtLeast(4.5)
            assertThat(contrast(onBrand, brandStrong)).isAtLeast(4.5)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val hi = maxOf(a.luminance(), b.luminance()).toDouble()
        val lo = minOf(a.luminance(), b.luminance()).toDouble()
        return (hi + 0.05) / (lo + 0.05)
    }
}
