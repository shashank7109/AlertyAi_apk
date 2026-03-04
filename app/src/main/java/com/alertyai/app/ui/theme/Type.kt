package com.alertyai.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.alertyai.app.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFont = GoogleFont("Inter")
val SpaceGroteskFont = GoogleFont("Space Grotesk")
val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

val SansFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = provider),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Black)
)

val SpaceFontFamily = FontFamily(
    Font(googleFont = SpaceGroteskFont, fontProvider = provider),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Black)
)

val MonoFontFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider)
)

val AlertyTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SpaceFontFamily,
        fontSize = 32.sp, 
        fontWeight = FontWeight.Black, 
        letterSpacing = (-1).sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceFontFamily,
        fontSize = 24.sp, 
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceFontFamily,
        fontSize = 20.sp, 
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceFontFamily,
        fontSize = 16.sp, 
        fontWeight = FontWeight.Bold
    ),
    bodyLarge = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 16.sp, 
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 14.sp, 
        fontWeight = FontWeight.Medium
    ),
    bodySmall = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 12.sp, 
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 10.sp, 
        fontWeight = FontWeight.Black, 
        letterSpacing = 1.5.sp
    )
)
