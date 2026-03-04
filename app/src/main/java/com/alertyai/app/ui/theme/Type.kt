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

val UbuntuFont = GoogleFont("Ubuntu")
val MontserratFont = GoogleFont("Montserrat")
val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

val SansFontFamily = FontFamily(
    Font(googleFont = UbuntuFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = UbuntuFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = UbuntuFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = UbuntuFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = UbuntuFont, fontProvider = provider, weight = FontWeight.Medium)
)

val SpaceFontFamily = FontFamily(
    Font(googleFont = MontserratFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = MontserratFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = MontserratFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = MontserratFont, fontProvider = provider, weight = FontWeight.SemiBold)
)

val MonoFontFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider)
)

val AlertyTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SpaceFontFamily,
        fontSize = 32.sp, 
        fontWeight = FontWeight.Medium, 
        letterSpacing = (-1).sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceFontFamily,
        fontSize = 24.sp, 
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceFontFamily,
        fontSize = 20.sp, 
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceFontFamily,
        fontSize = 16.sp, 
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 16.sp, 
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 14.sp, 
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 12.sp, 
        fontWeight = FontWeight.Normal
    ),
    labelSmall = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 10.sp, 
        fontWeight = FontWeight.Medium, 
        letterSpacing = 1.sp
    )
)
