package com.ridesandshares.passenger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF101418)
val Paper = Color(0xFFF7F4EE)
val InkText = Color(0xFF1C1915)
val Muted = Color(0xFF5E584E)
val Rule = Color(0xFFE4DDD2)

@Composable
fun RidesAndSharesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink,
            surface = Paper,
            primary = InkText,
            onPrimary = Paper,
            onSurface = InkText,
        ),
        content = content,
    )
}
