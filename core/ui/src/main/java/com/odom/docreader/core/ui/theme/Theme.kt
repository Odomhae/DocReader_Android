package com.odom.docreader.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = BrandTealLight,
    onPrimaryContainer = OnBrandTealLight,
    secondary = MutedTeal,
    onSecondary = Color.White,
    secondaryContainer = MutedTealLight,
    onSecondaryContainer = OnMutedTealLight,
    tertiary = SlateBlue,
    background = NeutralSurface,
    onBackground = NeutralOnSurface,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    surfaceVariant = NeutralSurfaceContainerHighest,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    surfaceTint = BrandTeal,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
    surfaceBright = NeutralSurface,
    surfaceDim = NeutralSurfaceDim,
    surfaceContainerLowest = NeutralSurfaceContainerLowest,
    surfaceContainerLow = NeutralSurfaceContainerLow,
    surfaceContainer = NeutralSurfaceContainer,
    surfaceContainerHigh = NeutralSurfaceContainerHigh,
    surfaceContainerHighest = NeutralSurfaceContainerHighest,
)

// No dynamic color: the app keeps one brand color on every device, and a wallpaper-derived
// palette could land on the colors the home list uses to tell document formats apart.
@Composable
fun DocReaderTheme(fontScaleMultiplier: Float = 1f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    // Multiplied rather than replaced, so a user who enlarged text in system settings keeps that.
    val scaledDensity = remember(density, fontScaleMultiplier) {
        Density(density.density, density.fontScale * fontScaleMultiplier)
    }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
