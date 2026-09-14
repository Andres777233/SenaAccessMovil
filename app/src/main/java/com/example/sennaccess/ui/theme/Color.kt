package com.example.sennaccess.ui.theme

// Definición de la paleta de colores de SennAccess: verde institucional SENA
// (único color de marca), semánticos de estado y las paletas oscura y clara.
// Todo el app consume estos colores vía LocalAppColors, nunca colores sueltos.
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Verde institucional SENA: color de marca y acento global (tono original).
val SenaGreen = Color(0xFF02D914)
// Variante de marca para fondos claros: mismo tono de marca (decisión de diseño:
// se usa el verde original también en tema claro).
val SenaGreenOnLight = Color(0xFF02D914)
// Verde utilizado para TEXTO de marca en cualquier tema (usa la variante clara
// solo cuando el fondo es claro), mediante verdeMarca().
val ErrorText = Color(0xFFC62828)
// Colores semánticos de estado: advertencias, errores y acentos cálidos.
val WarningYellow = Color(0xFFFFC107)
val ErrorRed = Color(0xFFFF6B6B)
val OrangeAmber = Color(0xFFFFA726)

// Contenedor de todos los colores de la UI, agrupados por rol semántico.
// Cada tema (claro y oscuro) proporciona su propia instancia.
data class AppColors(
    // Fondos de pantalla, superficies y variantes.
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    // Texto: primario, secundario y sobre color primario.
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnPrimary: Color,
    // Bordes y separadores.
    val border: Color,
    val borderLight: Color,
    // Fondos de componentes: tarjetas, campos de entrada y estados de color.
    val cardBackground: Color,
    val inputBackground: Color,
    val successBackground: Color,
    val warningBackground: Color,
    val errorBackground: Color,
    // Barras de navegación y superiores.
    val bottomNavBar: Color,
    val topBarBackground: Color,
    // Iconos y divisores.
    val iconTint: Color,
    val divider: Color,
    // Campos de texto: contenido y etiquetas.
    val inputText: Color,
    val inputLabel: Color,
    // Indicador de la pestaña activa en la navegación.
    val navigationIndicator: Color,
    // Textos de cabecera y subtítulos.
    val headerText: Color,
    val subtitleText: Color,
    // Tarjetas de estadísticas y filas de tablas.
    val statCardBackground: Color,
    val tableRowEven: Color,
    val tableRowOdd: Color,
    val tableHeaderBackground: Color,
    // Scrollbar y capas superpuestas.
    val scrollbarThumb: Color,
    val overlayBackground: Color,
    // Chips y etiquetas.
    val chipBackground: Color,
    val chipText: Color,
    // Botones secundarios.
    val buttonSecondaryBackground: Color,
    val buttonSecondaryText: Color,
)

// Paleta oscura: fondo casi negro, superficies gris-azuladas y acentos en el
// verde SENA vibrante. Favorece el contraste del estilo vidrio.
fun darkAppColors() = AppColors(
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
    successBackground = SenaGreen.copy(alpha = 0.13f),
    warningBackground = WarningYellow.copy(alpha = 0.14f),
    errorBackground = ErrorRed.copy(alpha = 0.14f),
    bottomNavBar = Color.Black.copy(alpha = 0.98f),
    topBarBackground = Color(0xFF07120A),
    iconTint = Color(0xFFF2F4F6),
    divider = Color.White.copy(alpha = 0.08f),
    inputText = Color(0xFFF2F4F6),
    inputLabel = Color(0xFF9BA1A8),
    navigationIndicator = SenaGreen.copy(alpha = 0.22f),
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

// Paleta clara: fondo gris muy suave, superficies blancas y texto oscuro.
// Ajustada para que las tarjetas de vidrio se lean como tarjetas definidas
// y el texto contraste sin esfuerzo.
fun lightAppColors() = AppColors(
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEFF3),
    textPrimary = Color(0xFF14161A),
    textSecondary = Color(0xFF5B6470),
    textOnPrimary = Color(0xFF04100A),
    border = Color.Black.copy(alpha = 0.10f),
    borderLight = SenaGreen.copy(alpha = 0.40f),
    cardBackground = Color.White,
    inputBackground = Color.White,
    successBackground = SenaGreen.copy(alpha = 0.16f),
    warningBackground = WarningYellow.copy(alpha = 0.15f),
    errorBackground = ErrorRed.copy(alpha = 0.15f),
    bottomNavBar = Color.White.copy(alpha = 0.99f),
    topBarBackground = Color(0xFFEAF6EE),
    iconTint = Color(0xFF14161A),
    divider = Color.Black.copy(alpha = 0.08f),
    inputText = Color(0xFF14161A),
    inputLabel = Color(0xFF5B6470),
    navigationIndicator = SenaGreen.copy(alpha = 0.22f),
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

// Verde de marca (tono original 0xFF02D914). Se usa igual en ambos temas por
// decisión de diseño del dueño (el texto de marca en tema claro tiene ~1.8:1).
// Uso: Text(..., color = verdeMarca())
@Composable
fun verdeMarca(): Color {
    val colors = LocalAppColors.current
    return if (colors.background.luminance() > 0.5f) SenaGreenOnLight else SenaGreen
}