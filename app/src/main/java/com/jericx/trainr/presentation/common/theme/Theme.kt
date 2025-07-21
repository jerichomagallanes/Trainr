package com.jericx.trainr.presentation.common.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Orange500,
    onPrimary = Color.Black,
    primaryContainer = Orange700,
    onPrimaryContainer = Gray100,

    secondary = Blue500,
    onSecondary = Color.Black,
    secondaryContainer = Blue500.copy(alpha = 0.3f),
    onSecondaryContainer = Color.White,

    tertiary = Red700,
    onTertiary = Color.White,

    background = SurfaceDark,
    onBackground = Gray100,

    surface = Gray900,
    onSurface = Gray100,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,

    error = RedError,
    onError = Color.White,

    outline = Gray700,
    outlineVariant = Gray800,

    scrim = Color.Black.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = Color.White,
    primaryContainer = Orange300,
    onPrimaryContainer = Gray900,

    secondary = Blue500,
    onSecondary = Color.White,
    secondaryContainer = Blue500.copy(alpha = 0.2f),
    onSecondaryContainer = Gray900,

    tertiary = Red700,
    onTertiary = Color.White,
    tertiaryContainer = Red700.copy(alpha = 0.2f),
    onTertiaryContainer = Gray900,

    background = Color.White,
    onBackground = Gray900,

    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,

    error = RedError,
    errorContainer = RedError.copy(alpha = 0.1f),
    onError = Color.White,
    onErrorContainer = RedError,

    outline = Gray300,
    outlineVariant = Gray300.copy(alpha = 0.5f),

    scrim = Color.Black.copy(alpha = 0.3f)
)

@Composable
fun TrainrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

val MaterialTheme.success: Color
    @Composable
    get() = GreenSuccess

val MaterialTheme.warning: Color
    @Composable
    get() = YellowWarning