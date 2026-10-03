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
import com.example.sennaccess.comun.diseno.MenuPerfilSena
import com.example.sennaccess.comun.diseno.MenuDesplegableVidrio
import com.example.sennaccess.comun.diseno.BarraSuperiorVidrio
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.superficiePlana
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
    // Barra unificada de los 4 roles: marca + chip de rol + notis + menú perfil.
    com.example.sennaccess.comun.diseno.BarraSuperiorSena(
        rol = "Administrador",
        noLeidas = noLeidas,
        isDark = isDark,
        onToggleTheme = onToggleTheme,
        onNotificaciones = onNavigate?.let { nav -> { nav(PantallaAdmin.NOTIFICACIONES) } },
        menu = { cerrar ->
            MenuPerfilSena(
                cerrar = cerrar,
                nombre = com.example.sennaccess.datos.sesion.GestorSesion.userName ?: "Administrador",
                email = com.example.sennaccess.datos.sesion.GestorSesion.userEmail ?: "",
                fotoPath = com.example.sennaccess.datos.sesion.GestorSesion.userPhoto,
                onPerfil = onNavigate?.let { nav -> { nav(PantallaAdmin.PERFIL) } }
            )
        },
        onLogout = onLogout
    )
}

@Composable
fun ContenedorVidrioAdmin(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .superficiePlana(cornerRadius = RadioVidrio)
            .padding(16.dp),
        content = content
    )
}
