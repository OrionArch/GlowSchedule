package com.example.schday.theme

import androidx.compose.ui.graphics.Color

// Light Theme Palette
val LightPrimary = Color(0xFF54624E)
val LightSecondary = Color(0xFF7B5455)
val LightTertiary = Color(0xFF4F6071)
val LightBackground = Color(0xFFFBF9F4)
val LightSurface = Color(0xFFFBF9F4)
val LightOnSurface = Color(0xFF1B1C19)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFF98A78F)
val LightOnPrimaryContainer = Color(0xFF2F3C2A)
val LightSecondaryContainer = Color(0xFFFDCBCB)
val LightOnSecondaryContainer = Color(0xFF795354)
val LightTertiaryContainer = Color(0xFF93A5B7)
val LightOnTertiaryContainer = Color(0xFF2A3B4A)
val LightSurfaceVariant = Color(0xFFE4E2DD)
val LightOnSurfaceVariant = Color(0xFF444841)
val LightOutlineVariant = Color(0xFFC5C8BE)

// Dark Theme Palette
val DarkPrimary = Color(0xFFBCCBB2)
val DarkSecondary = Color(0xFFECBBBA)
val DarkTertiary = Color(0xFFB6C9DB)
val DarkBackground = Color(0xFF1B1C19)
val DarkSurface = Color(0xFF1B1C19)
val DarkOnSurface = Color(0xFFE4E2DD)
val DarkOnPrimary = Color(0xFF283422)
val DarkPrimaryContainer = Color(0xFF3D4B37)
val DarkOnPrimaryContainer = Color(0xFFD8E7CD)
val DarkSecondaryContainer = Color(0xFF603D3E)
val DarkOnSecondaryContainer = Color(0xFFFFDAD9)
val DarkTertiaryContainer = Color(0xFF374958)
val DarkOnTertiaryContainer = Color(0xFFD2E5F8)
val DarkSurfaceVariant = Color(0xFF444841)
val DarkOnSurfaceVariant = Color(0xFFC5C8BE)
val DarkOutlineVariant = Color(0xFF444841)

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
