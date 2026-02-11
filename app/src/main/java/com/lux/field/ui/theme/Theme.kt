package com.lux.field.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LuxDarkColorScheme = darkColorScheme(
    primary = Zinc100,
    onPrimary = Zinc900,
    primaryContainer = Zinc800,
    onPrimaryContainer = Zinc100,
    secondary = Zinc400,
    onSecondary = Zinc900,
    secondaryContainer = Zinc700,
    onSecondaryContainer = Zinc200,
    tertiary = StatusInProgress,
    onTertiary = Zinc900,
    background = LuxBgBottom,
    onBackground = Zinc100,
    surface = LuxBgMid,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    surfaceContainerLowest = LuxBgBottom,
    surfaceContainerLow = Zinc900,
    surfaceContainer = LuxBgMid,
    surfaceContainerHigh = Zinc800,
    surfaceContainerHighest = Zinc700,
    outline = Zinc700,
    outlineVariant = WhiteAlpha30,
    error = StatusBlocked,
    onError = Zinc100,
)

private val LuxShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun LuxFieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LuxDarkColorScheme,
        typography = LuxTypography,
        shapes = LuxShapes,
        content = content,
    )
}
