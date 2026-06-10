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
    primary = SavannahGold,
    secondary = ForestGreen,
    tertiary = Terracotta,
    background = SavannahDarkBg,
    surface = SavannahCardDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2EBE5),
    onSurface = Color(0xFFE2EBE5),
    surfaceVariant = Color(0xFF23322C),
    outline = Color(0xFF3B4E45),
    primaryContainer = Color(0xFF2E4037),
    onPrimaryContainer = SavannahGold
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    secondary = SavannahGold,
    tertiary = Terracotta,
    background = SavannahCreamBg,
    surface = SavannahCardLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1B2621),
    onSurface = Color(0xFF1B2621),
    surfaceVariant = Color(0xFFECE4D6),
    outline = Color(0xFF8B8273),
    primaryContainer = Color(0xFFE2EBE5),
    onPrimaryContainer = ForestGreen
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
