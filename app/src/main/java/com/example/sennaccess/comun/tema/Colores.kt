package com.example.sennaccess.comun.tema

// Definición de la paleta de colores de SennAccess: verde institucional SENA
// (único color de marca), semánticos de estado y las paletas oscura y clara.
// Todo el app consume estos colores vía ColoresAppLocal, nunca colores sueltos.
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Verde institucional SENA: color de marca y acento global (tono original).
val VerdeSena = Color(0xFF02D914)
val VerdeSenaClaro = Color(0xFF02D914)
// Tinta verde oscura solo para texto/iconos sobre fondo claro (contraste).
val VerdeSenaTinta = Color(0xFF0B7A1E)
val TextoError = Color(0xFFC62828)
val AmarilloAviso = Color(0xFFFFC107)
val RojoError = Color(0xFFFF6B6B)
val NaranjaAmbar = Color(0xFFFFA726)

data class ColoresApp(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnPrimary: Color,
    val border: Color,
    val borderLight: Color,
    val cardBackground: Color,
    val inputBackground: Color,
    val successBackground: Color,
    val warningBackground: Color,
    val errorBackground: Color,
    val bottomNavBar: Color,
    val topBarBackground: Color,
    val iconTint: Color,
    val divider: Color,
    val inputText: Color,
    val inputLabel: Color,
    val navigationIndicator: Color,
    val headerText: Color,
    val subtitleText: Color,
    val statCardBackground: Color,
    val tableRowEven: Color,
    val tableRowOdd: Color,
    val tableHeaderBackground: Color,
    val scrollbarThumb: Color,
    val overlayBackground: Color,
    val chipBackground: Color,
    val chipText: Color,
    val buttonSecondaryBackground: Color,
    val buttonSecondaryText: Color,
)

fun coloresOscuros() = ColoresApp(
    background = Color(0xFF05070B),
    surface = Color(0xFF0E1217),
    surfaceVariant = Color(0xFF161D24),
    textPrimary = Color(0xFFF2F4F6),
    textSecondary = Color(0xFF9BA1A8),
    textOnPrimary = Color(0xFF04100A),
    border = Color.White.copy(alpha = 0.08f),
    borderLight = Color.White.copy(alpha = 0.22f),
    cardBackground = Color(0xFF0E1217).copy(alpha = 0.92f),
    inputBackground = Color(0xFF0E1217).copy(alpha = 0.96f),
    successBackground = VerdeSena.copy(alpha = 0.13f),
    warningBackground = AmarilloAviso.copy(alpha = 0.14f),
    errorBackground = RojoError.copy(alpha = 0.14f),
    bottomNavBar = Color.Black.copy(alpha = 0.98f),
    topBarBackground = Color(0xFF07120A),
    iconTint = Color(0xFFF2F4F6),
    divider = Color.White.copy(alpha = 0.08f),
    inputText = Color(0xFFF2F4F6),
    inputLabel = Color(0xFF9BA1A8),
    navigationIndicator = VerdeSena.copy(alpha = 0.22f),
    headerText = Color(0xFFF2F4F6),
    subtitleText = Color(0xFF9BA1A8),
    statCardBackground = Color(0xFF0E1217).copy(alpha = 0.92f),
    tableRowEven = Color(0xFF0E1217).copy(alpha = 0.6f),
    tableRowOdd = Color(0xFF161D24).copy(alpha = 0.6f),
    tableHeaderBackground = Color(0xFF0B1215),
    scrollbarThumb = Color(0xFF5A5A5A),
    overlayBackground = Color.Black.copy(alpha = 0.6f),
    chipBackground = Color(0xFF161D24).copy(alpha = 0.92f),
    chipText = Color(0xFFF2F4F6),
    buttonSecondaryBackground = Color(0xFF161D24).copy(alpha = 0.92f),
    buttonSecondaryText = Color(0xFFF2F4F6),
)

fun coloresClaros() = ColoresApp(
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEFF3),
    textPrimary = Color(0xFF14161A),
    textSecondary = Color(0xFF5B6470),
    textOnPrimary = Color(0xFF04100A),
    border = Color.Black.copy(alpha = 0.10f),
    borderLight = VerdeSena.copy(alpha = 0.40f),
    cardBackground = Color.White,
    inputBackground = Color.White,
    successBackground = VerdeSena.copy(alpha = 0.16f),
    warningBackground = AmarilloAviso.copy(alpha = 0.15f),
    errorBackground = RojoError.copy(alpha = 0.15f),
    bottomNavBar = Color.White.copy(alpha = 0.99f),
    topBarBackground = Color(0xFFEAF6EE),
    iconTint = Color(0xFF14161A),
    divider = Color.Black.copy(alpha = 0.08f),
    inputText = Color(0xFF14161A),
    inputLabel = Color(0xFF5B6470),
    navigationIndicator = VerdeSena.copy(alpha = 0.22f),
    headerText = Color(0xFF14161A),
    subtitleText = Color(0xFF5B6470),
    statCardBackground = Color.White,
    tableRowEven = Color.White,
    tableRowOdd = Color(0xFFF2F4F7),
    tableHeaderBackground = Color(0xFFE6EAF0),
    scrollbarThumb = Color(0xFF9AA0A6),
    overlayBackground = Color.Black.copy(alpha = 0.32f),
    chipBackground = Color.White,
    chipText = Color(0xFF14161A),
    buttonSecondaryBackground = Color.White,
    buttonSecondaryText = Color(0xFF14161A),
)

@Composable
fun verdeMarca(): Color {
    val colors = ColoresAppLocal.current
    // En claro se usa la tinta oscura para texto/iconos; los rellenos de botón
    // siguen usando VerdeSena directo con texto negro (sin cambios de marca).
    return if (colors.background.luminance() > 0.5f) VerdeSenaTinta else VerdeSena
}
