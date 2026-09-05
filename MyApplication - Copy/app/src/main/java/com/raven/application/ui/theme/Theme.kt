package com.raven.application.ui.theme

import android.app.Activity
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

private val TacticalColorScheme = darkColorScheme(
    primary = CyberGreen,
    secondary = SecondaryDark,
    tertiary = NeonOrange,
    background = TacticalCarbon,
    surface = TacticalSurface,
    onPrimary = Color.Black,
    onSecondary = FrostWhite,
    onTertiary = Color.Black,
    onBackground = FrostWhite,
    onSurface = FrostWhite,
    error = WarningRed,
    onError = Color.Black,
    errorContainer = WarningRed.copy(alpha = 0.2f),
    onErrorContainer = WarningRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008021), // Darker green for light mode legibility
    secondary = Color(0xFF505050),
    tertiary = Color(0xFFBF4716)
)


@Composable
fun RavenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> TacticalColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}