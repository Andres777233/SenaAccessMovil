package com.example.sennaccess.admin

// Layout reutilizable de las pantallas del rol ADMINISTRADOR.
// Provee el fondo de marca, la barra superior (AdminTopBar) y un contenedor de
// contenido; sirve de base para pantallas como accesos y mensajes, que reciben
// callbacks de navegación para volver al panel.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.IosGlassDropdownMenu
import com.example.sennaccess.ui.ios.IosGlassTopBar
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.GlassCornerRadius

@Composable
fun AdminScreenLayout(
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigate: ((AdminScreen) -> Unit)? = null,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    // Paleta de colores según el tema activo (claro/oscuro).
    val colors = LocalAppColors.current

    // Caja raíz: fija el fondo, respeta las barras del sistema y añade un borde sutil.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(0.dp))
    ) {
        // Fondo de marca SENA tenue + esferas de luz, detrás de todo el contenido.
        Image(
            painter = rememberAsyncImagePainter("https://www.sena.edu.co/Style%20Library/alayout/images/pattern.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.15f),
            contentScale = ContentScale.Crop
        )
        GlowSpheres(isDark = isDark)
        // Columna principal: barra superior fija y el contenido pasado por lambda.
        Column(modifier = Modifier.fillMaxSize()) {
            AdminTopBar(
                onLogout = onLogout,
                onNavigate = onNavigate,
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )

            // Área central con padding uniforme donde se renderiza cada pantalla.
            Box(
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp, vertical = 16.dp)
            )
            {
                content()
            }
        }

    }
}

/**
 * Barra superior del ADMINISTRADOR, igual que la de aprendiz/instructor:
 * "SENA ACCESS" + insignia "ADMINISTRADOR", botón de tema y menú hamburguesa
 * con perfil y cerrar sesión.
 */
@Composable
fun AdminTopBar(
    onLogout: () -> Unit,
    onNavigate: ((AdminScreen) -> Unit)? = null,
    noLeidas: Int = 0,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    // Estado local que controla si el menú hamburguesa está abierto.
    var showMenu by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    IosGlassTopBar {
        // Fila izquierda de la barra: marca de la app e insignia del rol.
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Marca: "SENA" en color primario y "ACCESS" resaltado en verde SENA.
            Text("SENA ", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("ACCESS", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Spacer(modifier = Modifier.width(8.dp))

            // Insignia del rol del usuario logueado (solo informativa).
            Box(
                modifier = Modifier
                    .border(1.dp, SenaGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ADMINISTRADOR",
                    color = SenaGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Empuja los controles de la derecha al extremo opuesto de la barra.
        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Campana de notificaciones con badge del número de no leídas.
            if (onNavigate != null) {
                Box {
                    IconButton(onClick = { onNavigate(AdminScreen.NOTIFICACIONES) }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = colors.textPrimary
                        )
                    }
                    if (noLeidas > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(ErrorRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (noLeidas > 99) "99+" else noLeidas.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            // Alternador de tema claro/oscuro (el ícono cambia según el estado actual).
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = colors.textPrimary
                )
            }
            // Menú hamburguesa desplegable: perfil y cierre de sesión.
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Menu, null, tint = colors.textPrimary)
                }
                IosGlassDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // La opción Perfil solo se muestra si se pasó un manejador de navegación.
                    if (onNavigate != null) {
                        DropdownMenuItem(
                            text = { Text("Perfil", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = SenaGreen) },
                            onClick = { showMenu = false; onNavigate(AdminScreen.PERFIL) }
                        )
                        DropdownMenuItem(
                            text = { Text("Notificaciones", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.Notifications, null, tint = SenaGreen) },
                            onClick = { showMenu = false; onNavigate(AdminScreen.NOTIFICACIONES) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Cerrar sesion", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Logout, null, tint = Color.Red) },
                        onClick = { showMenu = false; onLogout() }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminGlassContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    // Columna con fondo de vidrio y padding estándar para agrupar secciones/formularios.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(16.dp),
        content = content
    )
}
