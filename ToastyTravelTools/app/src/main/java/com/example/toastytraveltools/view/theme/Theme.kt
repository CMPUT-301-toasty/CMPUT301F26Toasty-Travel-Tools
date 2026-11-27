package com.example.toastytraveltools.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Sage,
    onPrimaryContainer = Forest,
    inversePrimary = ForestLight,
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = Sand,
    onSecondaryContainer = Clay,
    tertiary = DeepForest,
    onTertiary = Color.White,
    tertiaryContainer = Sage,
    onTertiaryContainer = DeepForest,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Sage,
    onSurfaceVariant = Moss,
    surfaceTint = Forest,
    inverseSurface = Ink,
    inverseOnSurface = Cream,
    error = Brick,
    onError = Color.White,
    errorContainer = BrickContainer,
    onErrorContainer = Brick,
    outline = Hairline,
    outlineVariant = Sage,
    surfaceBright = Paper,
    surfaceDim = Sage,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Paper,
    surfaceContainer = Paper,
    surfaceContainerHigh = Paper,
    surfaceContainerHighest = Paper,
)

private val DarkColorScheme = darkColorScheme(
    primary = ForestLight,
    onPrimary = ForestOn,
    primaryContainer = Forest,
    onPrimaryContainer = Sage,
    inversePrimary = Forest,
    secondary = ClayLight,
    onSecondary = ClayOn,
    secondaryContainer = SandDark,
    onSecondaryContainer = Sand,
    tertiary = DeepForestLight,
    onTertiary = ForestOn,
    tertiaryContainer = SageDark,
    onTertiaryContainer = DeepForestLight,
    background = NightBackground,
    onBackground = NightInk,
    surface = NightBackground,
    onSurface = NightInk,
    surfaceVariant = SageDark,
    onSurfaceVariant = NightMoss,
    surfaceTint = ForestLight,
    inverseSurface = NightInk,
    inverseOnSurface = NightBackground,
    error = BrickLight,
    onError = BrickOn,
    errorContainer = BrickContainerDark,
    onErrorContainer = BrickOnContainerDark,
    outline = NightHairline,
    outlineVariant = SageDark,
    surfaceBright = NightContainerHighest,
    surfaceDim = NightBackground,
    surfaceContainerLowest = NightContainerLowest,
    surfaceContainerLow = NightContainerLow,
    surfaceContainer = NightContainer,
    surfaceContainerHigh = NightContainerHigh,
    surfaceContainerHighest = NightContainerHighest,
)

@Composable
fun ToastyTravelToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color stays off: the app uses the palette from the Figma design.
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
