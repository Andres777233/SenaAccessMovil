package com.example.sennaccess.ui.ds

// Componentes unificados del Design System: reemplazan los 3 TopBars, los 3
// shells, los 4 contenedores de vidrio, los botones manuales y los buscadores
// sueltos. Todo usa SenaTokens + Type.kt + LocalAppColors. Colores intactos.
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
import androidx.compose.ui.unit.dp
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.OrangeAmber
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.WarningYellow
import com.example.sennaccess.ui.theme.verdeMarca
import com.example.sennaccess.ui.ios.GlassDock
import com.example.sennaccess.ui.ios.GlassDockItem
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.IosGlassDropdownMenu
import com.example.sennaccess.ui.ios.IosGlassTopBar
import com.example.sennaccess.ui.ios.ThemeToggleButton
import com.example.sennaccess.ui.ios.glassSurface

// Título de sección con norma única (antes 11sp ls 1.5/2sp sueltos).
@Composable
fun SenaSectionTitle(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalAppColors.current.textSecondary,
        modifier = modifier.padding(horizontal = SenaSpacing.screenH, vertical = SenaSpacing.xs)
    )
}

// Tarjeta de vidrio única (reemplaza AdminGlassContainer/IosGlassContainer/Card).
@Composable
fun SenaCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .glassSurface(cornerRadius = SenaRadius.lg, elevated = elevated)
            .padding(SenaSpacing.md),
        content = content
    )
}

// Badge de estado único (DENTRO/FUERA/ENTRADA/SALIDA/PIN). Sin hardcodes.
@Composable
fun SenaBadge(texto: String, tipo: BadgeTipo, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val fondo = when (tipo) {
        BadgeTipo.EXITO -> SenaGreen.copy(alpha = 0.16f)
        BadgeTipo.AVISO -> WarningYellow.copy(alpha = 0.16f)
        BadgeTipo.ALERTA -> OrangeAmber.copy(alpha = 0.18f)
        BadgeTipo.ERROR -> ErrorRed.copy(alpha = 0.14f)
        BadgeTipo.NEUTRO -> colors.surfaceVariant.copy(alpha = 0.6f)
    }
    val tinta = when (tipo) {
        BadgeTipo.EXITO -> verdeMarca()
        BadgeTipo.AVISO -> colors.textPrimary
        BadgeTipo.ALERTA -> colors.textPrimary
        BadgeTipo.ERROR -> ErrorRed
        BadgeTipo.NEUTRO -> colors.textSecondary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SenaRadius.sm))
            .background(fondo)
            .padding(horizontal = SenaSpacing.xs, vertical = SenaSpacing.xxs),
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

// Tipos de badge del sistema.
enum class BadgeTipo { EXITO, AVISO, ALERTA, ERROR, NEUTRO }

// Buscador único con 48dp táctil y focus verde (reemplaza los 3 sueltos).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenaSearchField(
    valor: String,
    onValor: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SenaTouch.min),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = colors.textSecondary
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(SenaRadius.md),
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

// Chip de filtro único (reemplaza FilterChip sueltos con 50/6/4dp).
@Composable
fun SenaFilterChip(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier.heightIn(min = SenaTouch.min),
        shape = RoundedCornerShape(SenaRadius.sm),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SenaGreen,
            selectedLabelColor = Color.Black,
            containerColor = LocalAppColors.current.surfaceVariant.copy(alpha = 0.4f),
            labelColor = LocalAppColors.current.textSecondary
        )
    )
}

// TopBar única para los 3 roles (reemplaza Aprendiz/Instructor/AdminTopBar).
@Composable
fun SenaTopBar(
    rol: String,
    noLeidas: Int = 0,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onNotificaciones: (() -> Unit)? = null,
    menu: (@Composable ColumnScope.(cerrar: () -> Unit) -> Unit)? = null,
    onLogout: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    var showMenu by remember { mutableStateOf(false) }
    IosGlassTopBar {
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
            Spacer(modifier = Modifier.width(SenaSpacing.xs))
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
                        modifier = Modifier.size(SenaTouch.min)
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
                                .background(ErrorRed),
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
            ThemeToggleButton(isDark = isDark, onToggleTheme = onToggleTheme)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(SenaTouch.min)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Abrir menú",
                        tint = colors.textPrimary
                    )
                }
                IosGlassDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    menu?.invoke(this) { showMenu = false }
                    DropdownMenuItem(
                        text = { Text("Cerrar sesión", color = ErrorRed) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = ErrorRed
                            )
                        },
                        onClick = { showMenu = false; onLogout() }
                    )
                }
            }
        }
    }
}

// Scaffold único: fondo + TopBar + pull-to-refresh + contenido 16dp + dock.
// Reemplaza los 3 shells copiados con padding 16/16/16/96 mágico.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenaScaffold(
    rol: String,
    dockItems: List<GlassDockItem>,
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
    val colors = LocalAppColors.current
    val selectedKey = dockKeyFor(currentView, dockFallback)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlowSpheres(isDark = isDark)
        Column(modifier = Modifier.fillMaxSize()) {
            SenaTopBar(
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
                                start = SenaSpacing.screenH,
                                end = SenaSpacing.screenH,
                                top = SenaSpacing.screenV,
                                bottom = SenaSpacing.dockClearance
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
                            start = SenaSpacing.screenH,
                            end = SenaSpacing.screenH,
                            top = SenaSpacing.screenV,
                            bottom = SenaSpacing.dockClearance
                        )
                ) {
                    content()
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            GlassDock(
                items = dockItems,
                selectedKey = selectedKey,
                onSelect = onSelectDock
            )
        }
        overlay2Fa?.invoke()
    }
}

// Diálogo único a 28dp (reemplaza AlertDialog 24dp sueltos).
@Composable
fun SenaDialog(
    onDismiss: () -> Unit,
    titulo: String,
    texto: String,
    icono: ImageVector? = null,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    textoCancelar: String = "Cancelar"
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(SenaRadius.pill),
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
                modifier = Modifier.heightIn(min = SenaTouch.min),
                shape = RoundedCornerShape(SenaRadius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SenaGreen,
                    contentColor = Color.Black
                )
            ) {
                Text(textoConfirmar.uppercase(), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = SenaTouch.min)
            ) {
                Text(textoCancelar, color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// Fila vacía simétrica para tablas (reemplaza TableContainer 600dp + h-scroll).
@Composable
fun RowScope.SenaCell(
    texto: String,
    peso: Float,
    encabezado: Boolean = false
) {
    val colors = LocalAppColors.current
    Text(
        text = texto,
        style = if (encabezado) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        color = if (encabezado) colors.textPrimary else colors.textPrimary,
        fontWeight = if (encabezado) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .weight(peso)
            .padding(horizontal = SenaSpacing.xs, vertical = SenaSpacing.xs),
        maxLines = 2
    )
}
