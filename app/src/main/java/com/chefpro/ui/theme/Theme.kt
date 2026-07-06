package com.chefpro.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.chefpro.model.AppColorScheme

private val LightColors = lightColorScheme(
    primary = ChefOrange,
    onPrimary = Color.White,
    primaryContainer = ChefOrangeContainer,
    onPrimaryContainer = ChefOrangeOnContainer,
    secondary = ChefTeal,
    onSecondary = Color.White,
    secondaryContainer = ChefTealContainer,
    onSecondaryContainer = ChefTealOnContainer,
    tertiary = ChefPurple,
    onTertiary = Color.White,
    tertiaryContainer = ChefPurpleContainer,
    onTertiaryContainer = ChefPurpleOnContainer,
    background = LightBackground,
    onBackground = OnLightSurface,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF6D5E58),
    outline = Color(0xFFD7CCC8),
    outlineVariant = Color(0xFFE8DDD8),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedContainer,
    onErrorContainer = Color(0xFF5F0000),
)

private val DarkColors = darkColorScheme(
    primary = ChefOrangeLight,
    onPrimary = Color(0xFF4E1608),
    primaryContainer = Color(0xFF8D2E0F),
    onPrimaryContainer = ChefOrangeContainer,
    secondary = ChefTealLight,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = ChefTealContainer,
    tertiary = ChefPurpleLight,
    onTertiary = Color(0xFF311B92),
    tertiaryContainer = Color(0xFF4527A0),
    onTertiaryContainer = ChefPurpleContainer,
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBCAAA4),
    outline = Color(0xFF4E4540),
    outlineVariant = Color(0xFF3A3330),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5F0000),
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = ErrorRedContainer,
)

@Composable
fun ChefProTheme(
    colorScheme: AppColorScheme = AppColorScheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (colorScheme) {
        AppColorScheme.DARK -> true
        AppColorScheme.LIGHT -> false
        AppColorScheme.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        shapes = ChefShapes,
        content = content,
    )
}
