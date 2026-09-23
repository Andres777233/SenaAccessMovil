package com.example.sennaccess.administrador.panel

// Layout reutilizable de las pantallas del rol ADMINISTRADOR.
// Provee el fondo de marca, la barra superior (BarraSuperiorAdmin) y un contenedor de
// contenido; sirve de base para pantallas como accesos y mensajes, que reciben
// callbacks de navegación para volver al panel.

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.EsferasBrillo
import com.example.sennaccess.comun.diseno.MenuDesplegableVidrio
import com.example.sennaccess.comun.diseno.BarraSuperiorVidrio
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.RadioVidrio
@Composable
fun EstructuraAdministrador(
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigate: ((PantallaAdmin) -> Unit)? = null,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ColoresAppLocal.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(0.dp))
    ) {
        EsferasBrillo(isDark = isDark)
        Column(modifier = Modifier.fillMaxSize()) {
            BarraSuperiorAdmin(
                onLogout = onLogout,
                onNavigate = onNavigate,
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )

            Box(
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp, vertical = 16.dp)
            )
            {
                content()
            }
        }

    }
}

@Composable
fun BarraSuperiorAdmin(
    onLogout: () -> Unit,
    onNavigate: ((PantallaAdmin) -> Unit)? = null,
    noLeidas: Int = 0,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val colors = ColoresAppLocal.current

    BarraSuperiorVidrio {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SENA ", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("ACCESS", color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .border(1.dp, VerdeSena.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ADMINISTRADOR",
                    color = verdeMarca(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onNavigate != null) {
                Box {
                    IconButton(
                        onClick = { onNavigate(PantallaAdmin.NOTIFICACIONES) },
                        modifier = Modifier.size(48.dp)
                    ) {
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
                                .background(RojoError),
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
            BotonCambiarTema(isDark = isDark, onToggleTheme = onToggleTheme)
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Abrir menú", tint = colors.textPrimary)
                }
                MenuDesplegableVidrio(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onNavigate != null) {
                        DropdownMenuItem(
                            text = { Text("Perfil", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = verdeMarca()) },
                            onClick = { showMenu = false; onNavigate(PantallaAdmin.PERFIL) }
                        )
                        DropdownMenuItem(
                            text = { Text("Notificaciones", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.Notifications, null, tint = verdeMarca()) },
                            onClick = { showMenu = false; onNavigate(PantallaAdmin.NOTIFICACIONES) }
                        )
                        DropdownMenuItem(
                            text = { Text("Ambientes", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = verdeMarca()) },
                            onClick = { showMenu = false; onNavigate(PantallaAdmin.AMBIENTES) }
                        )
                        DropdownMenuItem(
                            text = { Text("Validar excusa (PIN)", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = verdeMarca()) },
                            onClick = { showMenu = false; onNavigate(PantallaAdmin.VALIDAR_EXCUSA) }
                        )
                        DropdownMenuItem(
                            text = { Text("Escanear QR de invitado", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, tint = verdeMarca()) },
                            onClick = { showMenu = false; onNavigate(PantallaAdmin.ESCANEAR_QR) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Cerrar sesion", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red) },
                        onClick = { showMenu = false; onLogout() }
                    )
                }
            }
        }
    }
}

@Composable
fun ContenedorVidrioAdmin(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = ColoresAppLocal.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .padding(16.dp),
        content = content
    )
}
