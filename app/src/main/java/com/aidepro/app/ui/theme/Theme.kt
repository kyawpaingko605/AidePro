package com.aidepro.app.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── AidePro Custom Color Palette ───────────────────────────────────────────
// Primary: Deep Purple / Indigo (IDE feel)
// Secondary: Teal / Cyan (accents)
// Tertiary: Amber (warnings/highlights)

private val AideProDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9B84FF),           // Soft Indigo
    onPrimary = Color(0xFF1A0066),
    primaryContainer = Color(0xFF2D1B8E),
    onPrimaryContainer = Color(0xFFD4BFFF),
    secondary = Color(0xFF4DD0E1),         // Cyan
    onSecondary = Color(0xFF003740),
    secondaryContainer = Color(0xFF004F5A),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFFFFB74D),          // Amber
    onTertiary = Color(0xFF3E2000),
    tertiaryContainer = Color(0xFF5C3200),
    onTertiaryContainer = Color(0xFFFFDDB3),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D0D14),        // Very dark background
    onBackground = Color(0xFFE6E1F9),
    surface = Color(0xFF13131E),           // Dark surface
    onSurface = Color(0xFFE6E1F9),
    surfaceVariant = Color(0xFF1E1E2E),    // Editor background
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF3D3A4A),
    outlineVariant = Color(0xFF2A2840),
    inverseSurface = Color(0xFFE6E1F9),
    inverseOnSurface = Color(0xFF1A1A2E),
    inversePrimary = Color(0xFF4527A0),
    surfaceTint = Color(0xFF9B84FF),
    scrim = Color(0xFF000000),
)

private val AideProLightColorScheme = lightColorScheme(
    primary = Color(0xFF4527A0),           // Deep Purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4BFFF),
    onPrimaryContainer = Color(0xFF1A0066),
    secondary = Color(0xFF006978),         // Teal
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF001F26),
    tertiary = Color(0xFF7B4F00),          // Amber
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF3E2000),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F7FF),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xFFFAF9FF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFECEAF4),
    onSurfaceVariant = Color(0xFF4A4458),
    outline = Color(0xFF7B7589),
    outlineVariant = Color(0xFFCBC4D8),
    inverseSurface = Color(0xFF2F2F3E),
    inverseOnSurface = Color(0xFFF3EFF7),
    inversePrimary = Color(0xFF9B84FF),
    surfaceTint = Color(0xFF4527A0),
    scrim = Color(0xFF000000),
)

@Composable
fun AideProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AideProDarkColorScheme
        else -> AideProLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AideProTypography,
        shapes = AideProShapes,
        content = content
    )
}
