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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
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
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.perfil.FotoPerfil
@Composable
fun TituloSeccionSena(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
        color = verdeMarca(),
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = EspaciadoSena.screenH, vertical = EspaciadoSena.xs)
    )
}

// Cabecera de pantalla senior: eyebrow + título + subtítulo + acción.
// Reubica el título a la izquierda con jerarquía clara; reemplaza el
// uso repetido de CabeceraPlegable en listados y formularios.
@Composable
fun CabeceraPantalla(
    eyebrow: String,
    titulo: String,
    subtitulo: String? = null,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EspaciadoSena.screenH, vertical = EspaciadoSena.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = verdeMarca(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitulo != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (accion != null) {
            Spacer(modifier = Modifier.width(EspaciadoSena.sm))
            accion()
        }
    }
}

// Grupo con contador: título de sección + píldora de conteo + contenido.
// Ordena los listados largos en bloques escaneables.
@Composable
fun GrupoSeccion(
    titulo: String,
    conteo: Int? = null,
    modifier: Modifier = Modifier,
    accionTexto: String? = null,
    onAccion: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val colors = ColoresAppLocal.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EspaciadoSena.screenH, vertical = EspaciadoSena.xs)
        ) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                color = verdeMarca(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (conteo != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(VerdeSena.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$conteo",
                        color = verdeMarca(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (accionTexto != null && onAccion != null) {
                Spacer(modifier = Modifier.width(EspaciadoSena.xs))
                androidx.compose.material3.TextButton(onClick = onAccion) {
                    Text(accionTexto, color = verdeMarca(), fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EspaciadoSena.screenH)
                .superficiePlana(cornerRadius = RadioSena.lg)
                .padding(vertical = 4.dp),
            content = contenido
        )
    }
}

// Fila de navegación senior: icono + textos + accesorio a la derecha.
// Unifica filas de ajustes, menús y accesos directos con divisor fino.
@Composable
fun FilaNavegacionSena(
    titulo: String,
    subtitulo: String? = null,
    icono: ImageVector? = null,
    accesorio: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ColoresAppLocal.current
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .escalaPresion(pressedScale = 0.98f, interactionSource = interaction)
            .clip(RoundedCornerShape(RadioSena.md))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VerdeSena.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (accesorio != null) {
            Spacer(modifier = Modifier.width(8.dp))
            accesorio()
        }
    }
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
        TipoInsignia.EXITO -> VerdeSena.copy(alpha = 0.15f)
        TipoInsignia.AVISO -> AmarilloAviso.copy(alpha = 0.16f)
        TipoInsignia.ALERTA -> NaranjaAmbar.copy(alpha = 0.18f)
        TipoInsignia.ERROR -> RojoError.copy(alpha = 0.13f)
        TipoInsignia.NEUTRO -> colors.surfaceVariant.copy(alpha = 0.7f)
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
            .clip(RoundedCornerShape(100.dp))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = tinta,
            fontWeight = FontWeight.Bold,
            maxLines = 1
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
            .heightIn(min = 52.dp),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = colors.textSecondary
            )
        },
        trailingIcon = if (valor.isNotEmpty()) {
            {
                IconButton(onClick = { onValor("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Limpiar búsqueda",
                        tint = colors.textSecondary
                    )
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(RadioSena.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = verdeMarca(),
            unfocusedBorderColor = colors.divider,
            focusedLabelColor = verdeMarca(),
            cursorColor = verdeMarca(),
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface
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
        label = { Text(texto, style = MaterialTheme.typography.labelMedium, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium) },
        modifier = modifier.heightIn(min = 38.dp),
        shape = RoundedCornerShape(100.dp),
        border = if (seleccionado) null else androidx.compose.foundation.BorderStroke(1.dp, ColoresAppLocal.current.divider),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VerdeSena,
            selectedLabelColor = Color.Black,
            containerColor = ColoresAppLocal.current.surface,
            labelColor = ColoresAppLocal.current.textSecondary
        )
    )
}

// Menú hamburguesa compartido por los 4 roles: hero informativo (sin acción)
// + una única fila Perfil y Cerrar sesión. Un solo camino al perfil.
@Composable
fun ColumnScope.MenuPerfilSena(
    cerrar: () -> Unit,
    nombre: String,
    email: String,
    fotoPath: String?,
    onPerfil: (() -> Unit)? = null,
    onEditarPerfil: (() -> Unit)? = null
) {
    val colors = ColoresAppLocal.current
    // Hero de cuenta: solo informa quién está dentro, no navega.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadioSena.md))
            .background(VerdeSena.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        FotoPerfil(fotoPath = fotoPath, nombre = nombre, tamano = 56.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MI CUENTA",
                color = verdeMarca(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = nombre,
                color = colors.textPrimary,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider(color = colors.divider)
    Spacer(modifier = Modifier.height(6.dp))
    // Único camino al perfil: respeta el destino de edición cuando existe.
    val irPerfil = onEditarPerfil ?: onPerfil
    if (irPerfil != null) {
        DropdownMenuItem(
            text = {
                Column {
                    Text("Perfil", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Ver y editar tu información", color = colors.textSecondary, fontSize = 12.sp)
                }
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VerdeSena.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = verdeMarca(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            },
            onClick = { cerrar(); irPerfil() },
            modifier = Modifier.heightIn(min = 60.dp)
        )
    }
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(VerdeSena),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SENA ",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "ACCESS",
                        style = MaterialTheme.typography.titleSmall,
                        color = verdeMarca(),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(VerdeSena.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = rol.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = verdeMarca(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onNotificaciones != null) {
                Box {
                    IconButton(
                        onClick = onNotificaciones,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Abrir notificaciones",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (noLeidas > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(RojoError)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (noLeidas > 99) "99+" else noLeidas.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            BotonCambiarTema(isDark = isDark, onToggleTheme = onToggleTheme, modifier = Modifier.size(44.dp))
            Box {
                // Botón hamburguesa profesional: pastilla con borde y punto verde.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Abrir menú",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(VerdeSena)
                    )
                }
                MenuDesplegableVidrio(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    menu?.invoke(this) { showMenu = false }
                    Spacer(modifier = Modifier.height(2.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(modifier = Modifier.height(2.dp))
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Cerrar sesión", color = RojoError, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Salir de este dispositivo", color = colors.textSecondary, fontSize = 12.sp)
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(RojoError.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = RojoError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        onClick = { showMenu = false; onLogout() },
                        modifier = Modifier.heightIn(min = 60.dp)
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
        containerColor = colors.surface,
        shape = RoundedCornerShape(RadioSena.lg),
        icon = icono?.let {
            {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(VerdeSena.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        it,
                        contentDescription = null,
                        tint = verdeMarca(),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
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
                shape = RoundedCornerShape(RadioSena.md),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeSena,
                    contentColor = Color.Black
                )
            ) {
                Text(textoConfirmar.uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
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

// Tarjeta protagonista: número grande + etiqueta + icono lateral.
// Una por pantalla, siempre en vidrio elevado; el resto va en plano.
@Composable
fun TarjetaHeroSena(
    valor: String,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    detalle: String? = null
) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = modifier
            .superficieVidrio(cornerRadius = RadioSena.lg, elevated = true)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(VerdeSena)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = etiqueta.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                color = verdeMarca(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(VerdeSena.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(26.dp))
        }
    }
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
