package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary, // D0BCFF
    secondary = MintSecondary, // B2F2BB
    tertiary = MintTertiary, // 381E72
    background = DarkBackground, // 1C1B1F
    surface = DarkCardBackground, // 2B2930
    onPrimary = Color(0xFF381E72), // Deep purple body text
    onSecondary = Color(0xFF1E3A24), // Deep forest text
    onBackground = Color(0xFFE6E1E5), // Soft white
    onSurface = Color(0xFFE6E1E5), // Soft white
    surfaceVariant = Color(0xFF49454F), // Slate border
    outline = Color(0xFF49454F),
    primaryContainer = Color(0xFF381E72), // Budget card container color
    onPrimaryContainer = Color(0xFFEADDFF) // Budget card container text
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = SlateBackgroundLight,
    surface = CardBackgroundLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    outline = Color(0xFF79747E),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Use our tailored branding color scheme by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
