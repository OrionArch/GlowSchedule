package com.example.schday.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class GlowTheme(val displayName: String) {
    ACADEMIC_SERENITY("极简莫兰迪"),
    DEEP_CHARCOAL("摩登暗黑粉彩"),
    AMOLED_POP("纯黑波普"),
    VINTAGE_LIBRARY("复古书卷");

    fun getDisplayName(context: android.content.Context): String = when (this) {
        ACADEMIC_SERENITY -> context.getString(com.example.schday.R.string.theme_academic_serenity)
        DEEP_CHARCOAL -> context.getString(com.example.schday.R.string.theme_deep_charcoal)
        AMOLED_POP -> context.getString(com.example.schday.R.string.theme_amoled_pop)
        VINTAGE_LIBRARY -> context.getString(com.example.schday.R.string.theme_vintage_library)
    }
}

private val SerenityDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outlineVariant = DarkOutlineVariant
)

private val SerenityLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outlineVariant = LightOutlineVariant
)

private val DeepCharcoalColorScheme = darkColorScheme(
    primary = CharcoalPrimary,
    secondary = CharcoalSecondary,
    tertiary = CharcoalTertiary,
    background = CharcoalBackground,
    surface = CharcoalSurface,
    onBackground = CharcoalOnSurface,
    onSurface = CharcoalOnSurface,
    onPrimary = CharcoalOnPrimary,
    primaryContainer = CharcoalPrimaryContainer,
    onPrimaryContainer = CharcoalOnPrimaryContainer,
    secondaryContainer = CharcoalSecondaryContainer,
    onSecondaryContainer = CharcoalOnSecondaryContainer,
    tertiaryContainer = CharcoalTertiaryContainer,
    onTertiaryContainer = CharcoalOnTertiaryContainer,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CharcoalOnSurfaceVariant,
    outline = CharcoalOutline,
    outlineVariant = CharcoalOutlineVariant
)

private val AmoledPopColorScheme = darkColorScheme(
    primary = NeonPrimary,
    secondary = NeonSecondary,
    tertiary = NeonTertiary,
    background = NeonBackground,
    surface = NeonSurface,
    onBackground = NeonOnSurface,
    onSurface = NeonOnSurface,
    onPrimary = NeonOnPrimary,
    primaryContainer = NeonPrimaryContainer,
    onPrimaryContainer = NeonOnPrimaryContainer,
    secondaryContainer = NeonSecondaryContainer,
    onSecondaryContainer = NeonOnSecondaryContainer,
    tertiaryContainer = NeonTertiaryContainer,
    onTertiaryContainer = NeonOnTertiaryContainer,
    surfaceVariant = NeonSurfaceVariant,
    onSurfaceVariant = NeonOnSurfaceVariant,
    outline = NeonOutline,
    outlineVariant = NeonOutlineVariant
)

private val VintageLibraryColorScheme = darkColorScheme(
    primary = VintagePrimary,
    secondary = VintageSecondary,
    tertiary = VintageTertiary,
    background = VintageBackground,
    surface = VintageSurface,
    onBackground = VintageOnSurface,
    onSurface = VintageOnSurface,
    onPrimary = VintageOnPrimary,
    primaryContainer = VintagePrimaryContainer,
    onPrimaryContainer = VintageOnPrimaryContainer,
    secondaryContainer = VintageSecondaryContainer,
    onSecondaryContainer = VintageOnSecondaryContainer,
    tertiaryContainer = VintageTertiaryContainer,
    onTertiaryContainer = VintageOnTertiaryContainer,
    surfaceVariant = VintageSurfaceVariant,
    onSurfaceVariant = VintageOnSurfaceVariant,
    outline = VintageOutline,
    outlineVariant = VintageOutlineVariant
)

@Composable
fun GlowScheduleTheme(
    appTheme: GlowTheme = GlowTheme.DEEP_CHARCOAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        GlowTheme.ACADEMIC_SERENITY -> if (darkTheme) SerenityDarkColorScheme else SerenityLightColorScheme
        GlowTheme.DEEP_CHARCOAL -> DeepCharcoalColorScheme
        GlowTheme.AMOLED_POP -> AmoledPopColorScheme
        GlowTheme.VINTAGE_LIBRARY -> VintageLibraryColorScheme
    }

    val typography = when (appTheme) {
        GlowTheme.ACADEMIC_SERENITY -> SerenityTypography
        GlowTheme.DEEP_CHARCOAL -> CharcoalTypography
        GlowTheme.AMOLED_POP -> AmoledTypography
        GlowTheme.VINTAGE_LIBRARY -> VintageTypography
    }

    val shapes = when (appTheme) {
        GlowTheme.ACADEMIC_SERENITY -> androidx.compose.material3.Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        )
        GlowTheme.DEEP_CHARCOAL -> androidx.compose.material3.Shapes(
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp)
        )
        GlowTheme.AMOLED_POP -> androidx.compose.material3.Shapes(
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp)
        )
        GlowTheme.VINTAGE_LIBRARY -> androidx.compose.material3.Shapes(
            small = RoundedCornerShape(2.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(4.dp)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val isLight = appTheme == GlowTheme.ACADEMIC_SERENITY && !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
