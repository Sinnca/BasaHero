package com.basahero.elearning.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Grade 4 → Blue | Grade 5 → Green | Grade 6 → Orange | Teacher → Blue

val Gray900=Color(0xFF212121);val Gray600=Color(0xFF757575);val Gray200=Color(0xFFEEEEEE)
val Gray50=Color(0xFFFAFAFA);val ErrorRed=Color(0xFFE53935);val ErrorLight=Color(0xFFFFCDD2)

// Grade 4 — Blue
val Grade4ColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlue.copy(alpha = 0.2f),
    onPrimaryContainer = PrimaryBlue,
    secondary = SecondaryMint,
    onSecondary = Color.White,
    secondaryContainer = SecondaryMint.copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = AccentOrange,
    onTertiary = Color.White,
    tertiaryContainer = AccentOrange.copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFFE65100),
    background = SoftWhite,
    onBackground = Color(0xFF1E293B),
    surface = SurfaceWhite,
    onSurface = Color(0xFF1E293B)
)

// Grade 5 — Orange
val Grade5ColorScheme = lightColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    primaryContainer = AccentOrange.copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFFE65100),
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    background = Color(0xFFFFF8F5),
    surface = SurfaceWhite
)

// Grade 6 — Red
val Grade6ColorScheme = lightColorScheme(
    primary = AccentRed,
    onPrimary = Color.White,
    primaryContainer = AccentRed.copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    background = Color(0xFFFFF5F5),
    surface = SurfaceWhite
)

fun colorSchemeForGrade(gradeLevel: Int): ColorScheme = when (gradeLevel) {
    5    -> Grade5ColorScheme
    6    -> Grade6ColorScheme
    else -> Grade4ColorScheme
}

@Composable
fun BasaHeroTheme(gradeLevel: Int = 0, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorSchemeForGrade(gradeLevel),
        typography  = BasaHeroTypography,
        content     = content
    )
}

val BasaHeroTypography = Typography