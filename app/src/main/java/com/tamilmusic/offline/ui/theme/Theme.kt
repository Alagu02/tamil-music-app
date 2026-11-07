package com.tamilmusic.offline.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Purple = Color(0xFF8B5CF6)
val PurpleDeep = Color(0xFF6D28D9)
val Pink = Color(0xFFEC4899)
val AccentBrush = Brush.linearGradient(listOf(Purple, Pink))

private val DarkColors = darkColorScheme(
    primary = Purple, onPrimary = Color.White,
    secondary = Pink, onSecondary = Color.White,
    tertiary = Color(0xFFC084FC),
    background = Color(0xFF0E0A1A), onBackground = Color(0xFFF5F0FF),
    surface = Color(0xFF161126), onSurface = Color(0xFFF5F0FF),
    surfaceVariant = Color(0xFF241C3D), onSurfaceVariant = Color(0xFFB9AFD6),
    primaryContainer = Color(0xFF3B2A6E), onPrimaryContainer = Color(0xFFEADDFF),
    outline = Color(0xFF4B3F72)
)

private val LightColors = lightColorScheme(
    primary = PurpleDeep, onPrimary = Color.White,
    secondary = Pink, onSecondary = Color.White,
    tertiary = Color(0xFF9333EA),
    background = Color(0xFFFBF7FF), onBackground = Color(0xFF1B1530),
    surface = Color.White, onSurface = Color(0xFF1B1530),
    surfaceVariant = Color(0xFFEFE7FB), onSurfaceVariant = Color(0xFF5E5578),
    primaryContainer = Color(0xFFE9DDFF), onPrimaryContainer = Color(0xFF2A1560),
    outline = Color(0xFFB9AFD6)
)

@Composable
fun TamilMusicTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
