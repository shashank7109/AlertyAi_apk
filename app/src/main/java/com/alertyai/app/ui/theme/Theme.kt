package com.alertyai.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    secondary          = Secondary,
    tertiary           = Tertiary,
    primaryContainer   = Color(0xFFF4F4F5), // Zinc 100
    background         = Background,
    surface            = Surface,
    surfaceVariant     = SurfaceVariant,
    onBackground       = TextPrimary,
    onSurface          = TextPrimary,
    onSurfaceVariant   = TextSecondary,
    outline            = Divider,
    error              = Error
)

private val DarkColorScheme = darkColorScheme(
    primary            = DarkText, // White text as primary on dark
    onPrimary          = DarkBackground, // Black text on white primary
    secondary          = Secondary,
    tertiary           = Tertiary,
    background         = DarkBackground,
    surface            = DarkSurface,
    surfaceVariant     = DarkSurfaceVar,
    onBackground       = DarkText,
    onSurface          = DarkText,
    onSurfaceVariant   = DarkTextSub,
    outline            = DarkDivider,
    error              = Error
)

@Composable
fun AlertyAITheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography  = AlertyTypography,
        content     = content
    )
}
