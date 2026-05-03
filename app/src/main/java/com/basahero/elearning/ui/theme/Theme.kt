package com.basahero.elearning.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Grade 4 → Blue | Grade 5 → Green | Grade 6 → Orange | Teacher → Blue

val Gray900=Color(0xFF212121);val Gray600=Color(0xFF757575);val Gray200=Color(0xFFEEEEEE)
val Gray50=Color(0xFFFAFAFA);val ErrorRed=Color(0xFFE53935);val ErrorLight=Color(0xFFFFCDD2)

// Grade 4 — Blue
val Grade4ColorScheme = lightColorScheme(
    primary=Color(0xFF1565C0), onPrimary=Color.White, primaryContainer=Color(0xFFBBDEFB), onPrimaryContainer=Color(0xFF1565C0),
    secondary=Color(0xFFFFB300), onSecondary=Color.White, secondaryContainer=Color(0xFFFFECB3), onSecondaryContainer=Color(0xFF5F4200),
    tertiary=Color(0xFF43A047), onTertiary=Color.White, tertiaryContainer=Color(0xFFC8E6C9), onTertiaryContainer=Color(0xFF1B4620),
    error=ErrorRed, onError=Color.White, errorContainer=ErrorLight, onErrorContainer=ErrorRed,
    background=Gray50, onBackground=Gray900, surface=Color.White, onSurface=Gray900,
    surfaceVariant=Gray200, onSurfaceVariant=Gray600, outline=Gray600
)

// Grade 5 — Green
val Grade5ColorScheme = lightColorScheme(
    primary=Color(0xFF2E7D32), onPrimary=Color.White, primaryContainer=Color(0xFFA5D6A7), onPrimaryContainer=Color(0xFF1B5E20),
    secondary=Color(0xFF00897B), onSecondary=Color.White, secondaryContainer=Color(0xFFB2DFDB), onSecondaryContainer=Color(0xFF00332E),
    tertiary=Color(0xFF7CB342), onTertiary=Color.White, tertiaryContainer=Color(0xFFDCEDC8), onTertiaryContainer=Color(0xFF2D4A00),
    error=ErrorRed, onError=Color.White, errorContainer=ErrorLight, onErrorContainer=ErrorRed,
    background=Color(0xFFF1F8F1), onBackground=Gray900, surface=Color.White, onSurface=Gray900,
    surfaceVariant=Color(0xFFE0EEE0), onSurfaceVariant=Color(0xFF3A5A3A), outline=Color(0xFF5A7A5A)
)

// Grade 6 — Orange
val Grade6ColorScheme = lightColorScheme(
    primary=Color(0xFFE65100), onPrimary=Color.White, primaryContainer=Color(0xFFFFCCBC), onPrimaryContainer=Color(0xFFBF360C),
    secondary=Color(0xFFFF8F00), onSecondary=Color.White, secondaryContainer=Color(0xFFFFECB3), onSecondaryContainer=Color(0xFF3E2000),
    tertiary=Color(0xFFEF5350), onTertiary=Color.White, tertiaryContainer=Color(0xFFFFCDD2), onTertiaryContainer=Color(0xFF5C0011),
    error=ErrorRed, onError=Color.White, errorContainer=ErrorLight, onErrorContainer=ErrorRed,
    background=Color(0xFFFFF8F5), onBackground=Gray900, surface=Color.White, onSurface=Gray900,
    surfaceVariant=Color(0xFFF5E6E0), onSurfaceVariant=Color(0xFF5A3A2A), outline=Color(0xFF8A5A4A)
)

fun colorSchemeForGrade(gradeLevel: Int): ColorScheme = when (gradeLevel) {
    5    -> Grade5ColorScheme
    6    -> Grade6ColorScheme
    else -> Grade4ColorScheme
}

@Composable
fun PhilIRITheme(gradeLevel: Int = 0, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorSchemeForGrade(gradeLevel),
        typography  = PhilIRITypography,
        content     = content
    )
}

val PhilIRITypography = Typography()