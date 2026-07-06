package com.chefpro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.chefpro.model.AppColorScheme

private val LightColors = lightColorScheme(
    primary = ChefAccent,
    onPrimary = Color.White,
    secondary = ChefAccentDark,
)

private val DarkColors = darkColorScheme(
    primary = ChefAccent,
    onPrimary = Color.White,
    secondary = ChefAccentDark,
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
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
