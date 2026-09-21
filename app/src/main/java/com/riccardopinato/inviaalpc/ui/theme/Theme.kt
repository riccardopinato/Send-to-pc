package com.riccardopinato.inviaalpc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F63F2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9ECFF),
    onPrimaryContainer = Color(0xFF18225E),
    secondary = Color(0xFF008C95),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F5F4),
    onSecondaryContainer = Color(0xFF00373B),
    tertiary = Color(0xFFE05D62),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2E3),
    onTertiaryContainer = Color(0xFF5B171B),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF17181D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17181D),
    surfaceVariant = Color(0xFFEEF0F6),
    onSurfaceVariant = Color(0xFF626774),
    outline = Color(0xFF8B909D),
    outlineVariant = Color(0xFFDCE0EA),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBEC4FF),
    onPrimary = Color(0xFF1E2B8A),
    primaryContainer = Color(0xFF3447C8),
    onPrimaryContainer = Color(0xFFE1E4FF),
    secondary = Color(0xFF7CD8DC),
    onSecondary = Color(0xFF00373B),
    secondaryContainer = Color(0xFF005057),
    onSecondaryContainer = Color(0xFF9DF3F6),
    tertiary = Color(0xFFFFB3B5),
    onTertiary = Color(0xFF68000A),
    tertiaryContainer = Color(0xFF8C272E),
    onTertiaryContainer = Color(0xFFFFDADB),
    background = Color(0xFF111217),
    onBackground = Color(0xFFE4E2E9),
    surface = Color(0xFF17181E),
    onSurface = Color(0xFFE4E2E9),
    surfaceVariant = Color(0xFF272930),
    onSurfaceVariant = Color(0xFFC6C6D0),
    outline = Color(0xFF8F909B),
    outlineVariant = Color(0xFF45464F)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun InviaAlPcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content
    )
}
