package com.example.sennaccess.comun.diseno

// Componentes unificados del Design System: reemplazan los 3 TopBars, los 3
// shells, los 4 contenedores de vidrio, los botones manuales y los buscadores
// sueltos. Todo usa SenaTokens + Type.kt + ColoresAppLocal. Colores intactos.
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.NaranjaAmbar
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.AmarilloAviso
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.BarraNavegacion
import com.example.sennaccess.comun.diseno.ElementoNavegacion
import com.example.sennaccess.comun.diseno.EsferasBrillo
import com.example.sennaccess.comun.diseno.MenuDesplegableVidrio
import com.example.sennaccess.comun.diseno.BarraSuperiorVidrio
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.diseno.superficieVidrio
@Composable
fun TituloSeccionSena(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ColoresAppLocal.current.textSecondary,
        modifier = modifier.padding(horizontal = EspaciadoSena.screenH, vertical = EspaciadoSena.xs)
    )
}

@Composable
fun TarjetaSena(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .superficieVidrio(cornerRadius = RadioSena.lg, elevated = elevated)
            .padding(EspaciadoSena.md),
        content = content
    )
}

@Composable
fun InsigniaSena(texto: String, tipo: TipoInsignia, modifier: Modifier = Modifier) {
    val colors = ColoresAppLocal.current
    val fondo = when (tipo) {
        TipoInsignia.EXITO -> VerdeSena.copy(alpha = 0.16f)
        TipoInsignia.AVISO -> AmarilloAviso.copy(alpha = 0.16f)
        TipoInsignia.ALERTA -> NaranjaAmbar.copy(alpha = 0.18f)
        TipoInsignia.ERROR -> RojoError.copy(alpha = 0.14f)
        TipoInsignia.NEUTRO -> colors.surfaceVariant.copy(alpha = 0.6f)
    }
    val tinta = when (tipo) {
        TipoInsignia.EXITO -> verdeMarca()
        TipoInsignia.AVISO -> colors.textPrimary
        TipoInsignia.ALERTA -> colors.textPrimary
        TipoInsignia.ERROR -> RojoError
        TipoInsignia.NEUTRO -> colors.textSecondary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadioSena.sm))
            .background(fondo)
            .padding(horizontal = EspaciadoSena.xs, vertical = EspaciadoSena.xxs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = tinta,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class TipoInsignia { EXITO, AVISO, ALERTA, ERROR, NEUTRO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscadorSena(
    valor: String,
    onValor: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colors = ColoresAppLocal.current
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ToqueSena.min),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = colors.textSecondary
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(RadioSena.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = verdeMarca(),
            unfocusedBorderColor = colors.divider,
            focusedLabelColor = verdeMarca(),
            cursorColor = verdeMarca(),
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun FiltroSena(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier.heightIn(min = ToqueSena.min),
        shape = RoundedCornerShape(RadioSena.sm),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VerdeSena,
            selectedLabelColor = Color.Black,
            containerColor = ColoresAppLocal.current.surfaceVariant.copy(alpha = 0.4f),
            labelColor = ColoresAppLocal.current.textSecondary
        )
    )
}

@Composable
fun BarraSuperiorSena(
    rol: String,
    noLeidas: Int = 0,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onNotificaciones: (() -> Unit)? = null,
    menu: (@Composable ColumnScope.(cerrar: () -> Unit) -> Unit)? = null,
    onLogout: () -> Unit = {}
) {
    val colors = ColoresAppLocal.current
    var showMenu by remember { mutableStateOf(false) }
    BarraSuperiorVidrio {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SENA ",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Text(
                "ACCESS",
                style = MaterialTheme.typography.titleMedium,
                color = verdeMarca()
            )
            Spacer(modifier = Modifier.width(EspaciadoSena.xs))
            Box(
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = rol.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = verdeMarca()
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onNotificaciones != null) {
                Box {
                    IconButton(
                        onClick = onNotificaciones,
                        modifier = Modifier.size(ToqueSena.min)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Abrir notificaciones",
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
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            BotonCambiarTema(isDark = isDark, onToggleTheme = onToggleTheme)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(ToqueSena.min)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Abrir menú",
                        tint = colors.textPrimary
                    )
                }
                MenuDesplegableVidrio(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    menu?.invoke(this) { showMenu = false }
                    DropdownMenuItem(
                        text = { Text("Cerrar sesión", color = RojoError) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = RojoError
                            )
                        },
                        onClick = { showMenu = false; onLogout() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstructuraSena(
    rol: String,
    dockItems: List<ElementoNavegacion>,
    currentView: String,
    dockFallback: String,
    onSelectDock: (String) -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    noLeidas: Int = 0,
    onNotificaciones: (() -> Unit)? = null,
    menu: (@Composable ColumnScope.(cerrar: () -> Unit) -> Unit)? = null,
    onLogout: () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    overlay2Fa: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ColoresAppLocal.current
    val selectedKey = claveNavegacion(currentView, dockFallback)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        EsferasBrillo(isDark = isDark)
        Column(modifier = Modifier.fillMaxSize()) {
            BarraSuperiorSena(
                rol = rol,
                noLeidas = noLeidas,
                isDark = isDark,
                onToggleTheme = onToggleTheme,
                onNotificaciones = onNotificaciones,
                menu = menu,
                onLogout = onLogout
            )
            if (onRefresh != null) {
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = EspaciadoSena.screenH,
                                end = EspaciadoSena.screenH,
                                top = EspaciadoSena.screenV,
                                bottom = EspaciadoSena.dockClearance
                            )
                    ) {
                        content()
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = EspaciadoSena.screenH,
                            end = EspaciadoSena.screenH,
                            top = EspaciadoSena.screenV,
                            bottom = EspaciadoSena.dockClearance
                        )
                ) {
                    content()
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BarraNavegacion(
                items = dockItems,
                selectedKey = selectedKey,
                onSelect = onSelectDock
            )
        }
        overlay2Fa?.invoke()
    }
}

@Composable
fun DialogoSena(
    onDismiss: () -> Unit,
    titulo: String,
    texto: String,
    icono: ImageVector? = null,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    textoCancelar: String = "Cancelar"
) {
    val colors = ColoresAppLocal.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(RadioSena.pill),
        icon = icono?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    tint = verdeMarca(),
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                modifier = Modifier.heightIn(min = ToqueSena.min),
                shape = RoundedCornerShape(RadioSena.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeSena,
                    contentColor = Color.Black
                )
            ) {
                Text(textoConfirmar.uppercase(), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = ToqueSena.min)
            ) {
                Text(textoCancelar, color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun RowScope.CeldaSena(
    texto: String,
    peso: Float,
    encabezado: Boolean = false
) {
    val colors = ColoresAppLocal.current
    Text(
        text = texto,
        style = if (encabezado) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        color = if (encabezado) colors.textPrimary else colors.textPrimary,
        fontWeight = if (encabezado) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .weight(peso)
            .padding(horizontal = EspaciadoSena.xs, vertical = EspaciadoSena.xs),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
