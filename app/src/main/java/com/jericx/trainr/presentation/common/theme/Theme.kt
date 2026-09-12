package com.jericx.trainr.presentation.common.theme

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkTrainrColors.brandStrong,
    onPrimary = DarkTrainrColors.onBrand,
    primaryContainer = Orange700,
    onPrimaryContainer = Gray100,

    secondary = Blue500,
    onSecondary = Color.Black,
    onSecondaryContainer = Color.White,

    tertiary = Red700,
    onTertiary = Color.White,

    background = DarkTrainrColors.surfacePage,
    surfaceContainerLowest = DarkTrainrColors.surfacePage,
    surfaceContainerLow = DarkTrainrColors.surfaceCard,
    surfaceContainer = DarkTrainrColors.surfaceCard,
    surfaceContainerHigh = DarkTrainrColors.surfaceCard,
    surfaceContainerHighest = DarkTrainrColors.surfaceSunken,
    outline = DarkTrainrColors.outlineControl,
    outlineVariant = DarkTrainrColors.outlineDivider,
    onBackground = DarkTrainrColors.onSurface,

    surface = DarkTrainrColors.surfacePage,
    onSurface = DarkTrainrColors.onSurface,
    surfaceVariant = DarkTrainrColors.surfaceSunken,
    onSurfaceVariant = DarkTrainrColors.onSurfaceMuted,

    error = DarkTrainrColors.danger,
    onError = DarkTrainrColors.onDanger,

    scrim = DarkTrainrColors.scrim
)

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = Color.White,
    primaryContainer = Orange300,
    onPrimaryContainer = Gray900,

    secondary = Blue500,
    onSecondary = Color.White,
    onSecondaryContainer = Gray900,

    tertiary = Red700,
    onTertiary = Color.White,
    onTertiaryContainer = Gray900,

    background = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Gray100,
    outline = OutlineGray,
    outlineVariant = DividerGray,
    onBackground = Slate800,

    surface = Color.White,
    onSurface = Slate800,
    surfaceVariant = Gray100,
    onSurfaceVariant = TextMuted,

    error = RedError,
    onError = Color.White,
    onErrorContainer = RedError,


    scrim = Color.Black.copy(alpha = 0.3f)
)

private val LocalTrainrDarkTheme = staticCompositionLocalOf { false }

@Composable
fun TrainrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            WindowCompat.setDecorFitsSystemWindows(window, false)

            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.let {
                it.isAppearanceLightStatusBars = !darkTheme
                it.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalTrainrColors provides if (darkTheme) DarkTrainrColors else LightTrainrColors,
        LocalTrainrDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

val MaterialTheme.trainrColors: TrainrColors
    @Composable
    @ReadOnlyComposable
    get() = LocalTrainrColors.current

// res/drawable-night alone is not enough: below API 31 the night qualifier
// follows the phone, not the in-app appearance preference.
@Composable
fun themedPainter(@DrawableRes light: Int, @DrawableRes night: Int): Painter =
    painterResource(if (LocalTrainrDarkTheme.current) night else light)
