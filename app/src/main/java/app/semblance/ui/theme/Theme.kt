package app.semblance.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = ConsoleBg,
    primaryContainer = ConsoleSurfaceElevated,
    onPrimaryContainer = AccentGreen,
    secondary = AccentCyan,
    onSecondary = ConsoleBg,
    secondaryContainer = ConsoleSurfaceVariant,
    onSecondaryContainer = AccentCyan,
    tertiary = AccentPurple,
    onTertiary = ConsoleBg,
    background = ConsoleBg,
    onBackground = TextPrimary,
    surface = ConsoleSurface,
    onSurface = TextPrimary,
    surfaceVariant = ConsoleSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ConsoleBorder,
    outlineVariant = ConsoleBorderBright,
    error = AccentRed,
    onError = ConsoleBg
)

@Composable
fun SemblanceTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = ConsoleBg.toArgb()
            window.navigationBarColor = ConsoleBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
