package com.kanjilens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE91E63),        // Pink accent for main actions
    onPrimary = Color.White,
    secondary = Color(0xFF4CAF50),       // Green for JLPT badges
    surface = Color(0xFF1A1A2E),         // Dark navy background
    onSurface = Color(0xFFE0E0E0),       // Light text
    background = Color(0xFF0F0F1A),      // Darker background
    onBackground = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF252540),  // Card backgrounds
    onSurfaceVariant = Color(0xFFB0B0B0),
)

@Composable
fun KanjiLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
