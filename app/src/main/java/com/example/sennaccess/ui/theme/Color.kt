package com.example.sennaccess.ui.theme

// Definición de la paleta de colores de SennAccess.
// Aquí viven el color de marca (verde SENA), los colores semánticos de estado
// y las paletas clara y oscura agrupadas en AppColors.
import androidx.compose.ui.graphics.Color

// Colores por defecto de la plantilla Material 3. Se conservan por compatibilidad;
// la paleta real de la app se consume a través de AppColors.
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Verde institucional SENA: color primario de la marca. Marca elementos activos,
// acentos, navegación y estados de éxito.
val SenaGreen = Color(0xFF02D914)
// Colores semánticos de estado: advertencias, errores y acentos cálidos.
val WarningYellow = Color(0xFFFFC107)
val ErrorRed = Color(0xFFFF6B6B)
val OrangeAmber = Color(0xFFFFA726)

// Fondo translúcido usado por las superficies de vidrio (glassmorphism).
val GlassBackground = Color(0xFF13161C).copy(alpha = 0.8f)
// Color de texto base para superficies de vidrio oscuras.
val ThemeText = Color.White

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

// Paleta oscura (por defecto de la app): fondo casi negro, superficies gris-azuladas
// y acentos en verde SENA. Favorece el contraste del estilo vidrio.
fun darkAppColors() = AppColors(
    background = Color(0xFF07090D),
    surface = Color(0xFF13161C),
    surfaceVariant = Color(0xFF1A2128),
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.5f),
    textOnPrimary = Color.White,
    border = Color.White.copy(alpha = 0.05f),
    borderLight = Color.White.copy(alpha = 0.2f),
    cardBackground = Color(0xFF13161C).copy(alpha = 0.8f),
    inputBackground = Color(0xFF13161C).copy(alpha = 0.85f),
    successBackground = SenaGreen.copy(alpha = 0.08f),
    warningBackground = WarningYellow.copy(alpha = 0.08f),
    errorBackground = ErrorRed.copy(alpha = 0.08f),
    bottomNavBar = Color.Black.copy(alpha = 0.95f),
    topBarBackground = Color(0xFF0E2013),
    iconTint = Color.White,
    divider = Color.White.copy(alpha = 0.05f),
    inputText = Color.White,
    inputLabel = Color.White.copy(alpha = 0.5f),
    navigationIndicator = SenaGreen.copy(alpha = 0.15f),
    headerText = Color.White,
    subtitleText = Color.White.copy(alpha = 0.5f),
    statCardBackground = Color(0xFF13161C).copy(alpha = 0.8f),
    tableRowEven = Color(0xFF13161C).copy(alpha = 0.5f),
    tableRowOdd = Color(0xFF1A2128).copy(alpha = 0.5f),
    tableHeaderBackground = Color(0xFF0E1417),
    scrollbarThumb = Color.Gray,
    overlayBackground = Color.Black.copy(alpha = 0.5f),
    chipBackground = Color(0xFF13161C).copy(alpha = 0.8f),
    chipText = Color.White,
    buttonSecondaryBackground = Color(0xFF13161C).copy(alpha = 0.8f),
    buttonSecondaryText = Color.White,
)

// Paleta clara: fondo gris claro, superficies blancas y texto oscuro.
// Ajustada para que las tarjetas de vidrio se lean como tarjetas definidas
// (no "cajas blancas fantasma" debajo del texto) y las filas tengan contraste.
fun lightAppColors() = AppColors(
    background = Color(0xFFF0F2F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EAF0),
    textPrimary = Color(0xFF1C1B1F),
    textSecondary = Color(0xFF6B7280),
    textOnPrimary = Color.White,
    border = Color.Black.copy(alpha = 0.13f),
    borderLight = SenaGreen.copy(alpha = 0.35f),
    cardBackground = Color.White,
    inputBackground = Color.White,
    successBackground = SenaGreen.copy(alpha = 0.10f),
    warningBackground = WarningYellow.copy(alpha = 0.12f),
    errorBackground = ErrorRed.copy(alpha = 0.10f),
    bottomNavBar = Color.White.copy(alpha = 0.97f),
    topBarBackground = Color(0xFFE9F5EC),
    iconTint = Color(0xFF1C1B1F),
    divider = Color.Black.copy(alpha = 0.10f),
    inputText = Color(0xFF1C1B1F),
    inputLabel = Color(0xFF6B7280),
    navigationIndicator = SenaGreen.copy(alpha = 0.18f),
    headerText = Color(0xFF1C1B1F),
    subtitleText = Color(0xFF6B7280),
    statCardBackground = Color.White,
    tableRowEven = Color.White,
    tableRowOdd = Color(0xFFF2F4F7),
    tableHeaderBackground = Color(0xFFE6EAF0),
    scrollbarThumb = Color(0xFF9AA0A6),
    overlayBackground = Color.Black.copy(alpha = 0.32f),
    chipBackground = Color.White,
    chipText = Color(0xFF1C1B1F),
    buttonSecondaryBackground = Color.White,
    buttonSecondaryText = Color(0xFF1C1B1F),
)
