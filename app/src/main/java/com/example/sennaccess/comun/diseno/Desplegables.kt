package com.example.sennaccess.comun.diseno

// Desplegable que se abre tocando CUALQUIER parte del campo, no solo la flecha.
// Fix pedido: los menús deben desplegarse al tocar el campo completo.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesplegableSena(
    valor: String,
    opciones: List<String>,
    onElegir: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    val colors = ColoresAppLocal.current
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, color = colors.textSecondary) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { abierto = !abierto },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = colors.textPrimary,
                disabledBorderColor = if (abierto) verdeMarca() else colors.divider,
                disabledLabelColor = colors.textSecondary,
                disabledTrailingIconColor = colors.textSecondary,
                disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        )
        // Capa invisible que captura el toque en todo el campo.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { abierto = !abierto }
        )
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            opciones.forEach { op ->
                DropdownMenuItem(
                    text = { Text(op, color = colors.textPrimary) },
                    onClick = { onElegir(op); abierto = false }
                )
            }
        }
    }
}
