package com.example.schday.theme

import androidx.compose.ui.graphics.Color

// Light Theme Palette
val LightPrimary = Color(0xFF5E6AD2)
val LightSecondary = Color(0xFF5C6079)
val LightTertiary = Color(0xFF7A5369)
val LightBackground = Color(0xFFF8F9FE)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF191B23)
val LightOnPrimary = Color(0xFFFFFFFF)

// Dark Theme Palette
val DarkPrimary = Color(0xFFBEC2FF)
val DarkSecondary = Color(0xFFC4C5DD)
val DarkTertiary = Color(0xFFEBA1C9)
val DarkBackground = Color(0xFF10121A)
val DarkSurface = Color(0xFF1B1D28)
val DarkOnSurface = Color(0xFFE4E5F0)
val DarkOnPrimary = Color(0xFF17206A)

// Morandi Course Pastel Colors (Background & Matching Text)
val MorandiMintBg = Color(0xFFD1E8E2)
val MorandiMintText = Color(0xFF1B4D3E)

val MorandiLavenderBg = Color(0xFFE2D4F0)
val MorandiLavenderText = Color(0xFF4B2E63)

val MorandiRoseBg = Color(0xFFF7D6D0)
val MorandiRoseText = Color(0xFF6B362F)

val MorandiPeachBg = Color(0xFFF7E2C7)
val MorandiPeachText = Color(0xFF663B0E)

val MorandiSkyBg = Color(0xFFC7E2F7)
val MorandiSkyText = Color(0xFF144D6C)

val MorandiButterBg = Color(0xFFF5ECAE)
val MorandiButterText = Color(0xFF5C5212)

val MorandiSageBg = Color(0xFFD5ECD4)
val MorandiSageText = Color(0xFF2C5627)

val MorandiOrchidBg = Color(0xFFEED1E6)
val MorandiOrchidText = Color(0xFF5B2B4E)

// Helper to get matching text color for a course color hex
fun getContrastingTextColor(hex: String, isDarkTheme: Boolean): Color {
    return when (hex.uppercase()) {
        "#D1E8E2" -> MorandiMintText
        "#E2D4F0" -> MorandiLavenderText
        "#F7D6D0" -> MorandiRoseText
        "#F7E2C7" -> MorandiPeachText
        "#C7E2F7" -> MorandiSkyText
        "#F5ECAE" -> MorandiButterText
        "#D5ECD4" -> MorandiSageText
        "#EED1E6" -> MorandiOrchidText
        else -> if (isDarkTheme) Color.White else Color.Black
    }
}

val MorandiColors = listOf(
    "#D1E8E2", // Mint
    "#E2D4F0", // Lavender
    "#F7D6D0", // Rose
    "#F7E2C7", // Peach
    "#C7E2F7", // Sky Blue
    "#F5ECAE", // Butter
    "#D5ECD4", // Sage
    "#EED1E6"  // Orchid
)
