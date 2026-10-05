package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Colors
val LightPrimary = Color(0xFF6B4E18)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFF7E2B4)
val LightOnPrimaryContainer = Color(0xFF261900)
val LightBackground = Color(0xFFFAF9F6)
val LightOnBackground = Color(0xFF1C1B1F)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1C1B1F)
val LightSurfaceVariant = Color(0xFFEBE6DD)
val LightOnSurfaceVariant = Color(0xFF4C4639)
val LightOutline = Color(0xFF7E7667)

// Dark Variant: Golden Warm Dark
val DarkGoldPrimary = Color(0xFFE5B84B)
val DarkGoldOnPrimary = Color(0xFF3F2E00)
val DarkGoldPrimaryContainer = Color(0xFF5B4300)
val DarkGoldOnPrimaryContainer = Color(0xFFFFDF9E)
val DarkGoldBackground = Color(0xFF141311)
val DarkGoldOnBackground = Color(0xFFE8E2D9)
val DarkGoldSurface = Color(0xFF1F1D19)
val DarkGoldOnSurface = Color(0xFFE8E2D9)
val DarkGoldSurfaceVariant = Color(0xFF2E2A24)
val DarkGoldOnSurfaceVariant = Color(0xFFD2C7B8)
val DarkGoldOutline = Color(0xFF988F80)

// Dark Variant: Night Blue
val DarkBluePrimary = Color(0xFF70B6F6)
val DarkBlueOnPrimary = Color(0xFF003258)
val DarkBluePrimaryContainer = Color(0xFF00497D)
val DarkBlueOnPrimaryContainer = Color(0xFFD1E4FF)
val DarkBlueBackground = Color(0xFF0B1320)
val DarkBlueOnBackground = Color(0xFFE2E8F0)
val DarkBlueSurface = Color(0xFF131D2E)
val DarkBlueOnSurface = Color(0xFFE2E8F0)
val DarkBlueSurfaceVariant = Color(0xFF1E293B)
val DarkBlueOnSurfaceVariant = Color(0xFFCBD5E1)
val DarkBlueOutline = Color(0xFF64748B)

// Accent and Diff Highlights
val DiffAddedColor = Color(0x3322C55E)
val DiffRemovedColor = Color(0x33EF4444)
val HighlightNoteColor = Color(0x40F59E0B)

data class HighlightColorOption(
    val id: String,
    val name: String,
    val hex: String,
    val color: Color
)

val HighlightColors = listOf(
    HighlightColorOption("yellow", "Żółty", "#FFF59D", Color(0xFFFFF59D)),
    HighlightColorOption("green", "Zielony", "#C8E6C9", Color(0xFFC8E6C9)),
    HighlightColorOption("blue", "Niebieski", "#BBDEFB", Color(0xFFBBDEFB)),
    HighlightColorOption("pink", "Różowy", "#F8BBD0", Color(0xFFF8BBD0)),
    HighlightColorOption("orange", "Pomarańczowy", "#FFE0B2", Color(0xFFFFE0B2)),
    HighlightColorOption("purple", "Fioletowy", "#E1BEE7", Color(0xFFE1BEE7))
)
