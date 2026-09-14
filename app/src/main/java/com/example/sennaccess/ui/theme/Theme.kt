package com.example.sennaccess.ui.theme

// Tema raíz de la app: provee la paleta SENA (clara u oscura según el modo del
// sistema, sobrescrita por el toggle del usuario) y la tipografía propia.
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Esquemas Material 3 derivados de la identidad SENA. Estos colores alimentan a
// los componentes M3 básicos; las superficies propias usan LocalAppColors.
private val DarkColorScheme = darkColorScheme(
    primary = SenaGreen,
    onPrimary = Color(0xFF04100A),
    secondary = Color(0xFF02D914),
    onSecondary = Color(0xFF04100A),
    background = Color(0xFF05070B),
    onBackground = Color(0xFFF2F4F6),
    surface = Color(0xFF0E1217),
    onSurface = Color(0xFFF2F4F6),
    surfaceVariant = Color(0xFF161D24),
    onSurfaceVariant = Color(0xFF9BA1A8),
    error = ErrorRed,
    onError = Color(0xFF2B0000)
)

private val LightColorScheme = lightColorScheme(
    primary = SenaGreen,
    onPrimary = Color(0xFF04100A),
    secondary = Color(0xFF02D914),
    onSecondary = Color(0xFF04100A),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF14161A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14161A),
    surfaceVariant = Color(0xFFEDEFF3),
    onSurfaceVariant = Color(0xFF5B6470),
    error = ErrorRed,
    onError = Color(0xFFFFFFFF)
)

// Tema raíz: elige el esquema según el modo del sistema (oscilable con el toggle
// de la app) y provee la paleta SENA activa a toda la UI.
@Composable
fun SennaccessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) darkAppColors() else lightAppColors()

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}