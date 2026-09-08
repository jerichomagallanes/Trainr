package com.jericx.trainr.presentation.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Orange500 = Color(0xFFD37200)
val Orange700 = Color(0xFF8B5A2B)
val Orange300 = Color(0xFFDEB887)

val Slate800 = Color(0xFF243036)
val OutlineGray = Color(0xFFB0BEC5)
val TextMuted = Color(0xFF626262)
val DividerGray = Color(0xFFD9D9D9)

val Gray900 = Color(0xFF212121)
val Gray800 = Color(0xFF424242)
val Gray700 = Color(0xFF616161)
val Gray500 = Color(0xFF9E9E9E)
val Gray300 = Color(0xFFE0E0E0)
val Gray200 = Color(0xFFEEEEEE)
val Gray100 = Color(0xFFF5F5F5)

val Red700 = Color(0xFF8B1A1A)
val Blue500 = Color(0xFF5DADE2)

val GreenSuccess = Color(0xFF4CAF50)
val RedError = Color(0xFFE74C3C)
val YellowWarning = Color(0xFFF39C12)

val StatusCompleted = Color(0xFF5F8C32)

@Deprecated(
    "Status colours are per-theme now.",
    ReplaceWith("MaterialTheme.trainrColors.statusActive")
)
val StatusInProgress = Orange500

@Deprecated(
    "Status colours are per-theme now.",
    ReplaceWith("MaterialTheme.trainrColors.statusIdle")
)
val StatusNotStarted = TextMuted

val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF121212)

// Dark values are tuned against the four dark grounds (page/card/raised/sunken).

@Immutable
data class TrainrColors(
    val surfacePage: Color,
    val surfaceCard: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    // Light keeps the #FAFAFA the ReviewScreen panel composited to; alpha over
    // an assumed white page turns a dark panel darker than its own card.
    val surfacePanel: Color,
    val surfaceSelected: Color,
    val onSurfaceSelected: Color,
    val surfaceEmphasis: Color,
    val onSurfaceEmphasis: Color,
    val onSurface: Color,
    // The one figure the design draws in pure black. Folding it into onSurface
    // moves light to #243036.
    val onSurfaceStrong: Color,
    val onSurfaceMuted: Color,
    // Light replaces the composite the field used to render (#626262 at 60% on
    // white, which reads 2.65 and fails AA); dark needs the muted ink at full
    // strength.
    val placeholder: Color,
    val outlineControl: Color,
    val outlineDivider: Color,
    // An unfilled card's hairline: ink-weight in light by design, outline tiers
    // in dark.
    val cardEdge: Color,
    val cardRule: Color,
    val raisedEdge: Color,
    val accentRule: Color,
    val focus: Color,
    val trackEmpty: Color,
    val brand: Color,
    // Brand tint on the selected slab, which inverts in dark: #D37200 is 2.80 there.
    val brandOnSelected: Color,
    // Brand is never a fill under text; text-bearing brand fills use brandStrong.
    val brandStrong: Color,
    // Brand behind a label that WCAG counts as large text (>=14pt bold), which
    // needs 3:1 rather than 4.5:1 and so may stay on the Figma orange.
    val brandLarge: Color,
    val onBrand: Color,
    val brandDisabled: Color,
    val onBrandDisabled: Color,
    val brandStrongDisabled: Color,
    val statusDone: Color,
    val statusDoneInk: Color,
    // Dimmer than statusDoneInk in dark: as a hairline that ink reads 7.12 on a
    // card where the neutral edge is 4.90.
    val statusDoneEdge: Color,
    val statusActive: Color,
    val statusIdle: Color,
    // Pure white, never the themed off-white ink: E8EDEF on statusDone is 4.12.
    val onStatus: Color,
    val danger: Color,
    val dangerInk: Color,
    val onDanger: Color,
    val dotInactive: Color,
    val shadowSpot: Color,
    val shadowSpotSoft: Color,
    val shadowSpotBrand: Color,
    val scrim: Color
)

val LightTrainrColors = TrainrColors(
    surfacePage = Color.White,
    surfaceCard = Color.White,
    surfaceRaised = Color.White,
    surfaceSunken = Gray100,
    surfacePanel = SurfaceLight,
    surfaceSelected = Slate800,
    onSurfaceSelected = Color.White,
    surfaceEmphasis = Slate800,
    onSurfaceEmphasis = Color.White,
    onSurface = Slate800,
    onSurfaceStrong = Color.Black,
    onSurfaceMuted = TextMuted,
    placeholder = Color(0xFF707070),
    outlineControl = Color(0xFF808E95),
    outlineDivider = DividerGray,
    cardEdge = Slate800,
    cardRule = Slate800,
    raisedEdge = Color.Transparent,
    accentRule = Color.Transparent,
    focus = Orange500,
    trackEmpty = OutlineGray,
    brand = Orange500,
    brandOnSelected = Orange500,
    brandStrong = Color(0xFFAB5C00),
    brandLarge = Orange500,
    onBrand = Color.White,
    brandDisabled = Color(0xFFE9B880),
    onBrandDisabled = Color(0xFFF8EAD9),
    brandStrongDisabled = Color(0xFFE09C4D),
    statusDone = Color(0xFF567C2C),
    statusDoneInk = Color(0xFF4F7429),
    statusDoneEdge = StatusCompleted,
    statusActive = Color(0xFFB36000),
    statusIdle = TextMuted,
    onStatus = Color.White,
    danger = RedError,
    dangerInk = Color(0xFFC0392B),
    onDanger = Color.White,
    dotInactive = Color(0xFFBDC1C3),
    shadowSpot = Color.Black,
    shadowSpotSoft = Color.Black.copy(alpha = 0.05f),
    shadowSpotBrand = Orange500.copy(alpha = 0.15f),
    scrim = Color.Black.copy(alpha = 0.3f)
)

val DarkTrainrColors = TrainrColors(
    surfacePage = Color(0xFF101519),
    surfaceCard = Color(0xFF20282E),
    surfaceRaised = Color(0xFF323D44),
    surfaceSunken = Color(0xFF2C363D),
    surfacePanel = Color(0xFF2C363D),
    surfaceSelected = Color(0xFFE4EAEC),
    onSurfaceSelected = Color(0xFF101519),
    surfaceEmphasis = Color(0xFF34515F),
    onSurfaceEmphasis = Color.White,
    onSurface = Color(0xFFE8EDEF),
    onSurfaceStrong = Color(0xFFE8EDEF),
    onSurfaceMuted = Color(0xFFA8B5BF),
    placeholder = Color(0xFFA8B5BF),
    outlineControl = Color(0xFF82979F),
    outlineDivider = Color(0xFF414E57),
    cardEdge = Color(0xFF82979F),
    cardRule = Color(0xFF414E57),
    raisedEdge = Color(0xFF414E57),
    accentRule = Color(0xFFE8963A),
    focus = Color(0xFFFFA23C),
    trackEmpty = Color(0xFF414E57),
    brand = Orange500,
    brandOnSelected = Color(0xFFAB5C00),
    brandStrong = Color(0xFFE8963A),
    brandLarge = Color(0xFFE8963A),
    onBrand = Color(0xFF101519),
    brandDisabled = Color(0xFF485259),
    onBrandDisabled = Color(0xFF858D92),
    brandStrongDisabled = Color(0xFF778085),
    statusDone = Color(0xFF567C2C),
    statusDoneInk = Color(0xFF8BC34A),
    statusDoneEdge = Color(0xFF6E9E3A),
    statusActive = Color(0xFFB36000),
    statusIdle = Color(0xFF687279),
    onStatus = Color.White,
    danger = RedError,
    dangerInk = Color(0xFFFF8573),
    onDanger = Color.White,
    dotInactive = Color(0xFF515659),
    shadowSpot = Color.Transparent,
    shadowSpotSoft = Color.Transparent,
    shadowSpotBrand = Color.Transparent,
    scrim = Color.Black.copy(alpha = 0.7f)
)

val LocalTrainrColors = staticCompositionLocalOf { LightTrainrColors }
