package org.anymodel.chat.core.ui.theme

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
import org.anymodel.chat.settings.domain.ThemeMode

private val Primary = Color(0xFF3B5BDB)
private val PrimaryDark = Color(0xFFB4C0FF)

private val LightColors = lightColorScheme(
    primary = Primary,
    secondary = Color(0xFF5C6BC0),
    tertiary = Color(0xFF00897B)
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    secondary = Color(0xFFB0B9F0),
    tertiary = Color(0xFF80CBC4)
)

@Composable
fun AnyModelTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
