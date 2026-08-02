package com.example.rusoit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColorScheme = darkColorScheme(
    primary = FireRed,
    secondary = FireOrange,
    tertiary = FireYellow,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@OptIn(ExperimentalTvMaterial3Api::class)
private val LightColorScheme = lightColorScheme(
    primary = FireRed,
    secondary = FireOrange,
    tertiary = FireYellow,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RusoitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
