package com.example.sennaccess.ui.ios

// Componentes base de las pantallas de autenticación (splash, landing, login,
// registro, recuperación y restablecimiento). Centralizan el fondo de vidrio,
// el botón de tema, los campos de texto accesibles y las cajitas de error para
// que todas las pantallas de auth tengan el mismo lenguaje visual.
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.R
import com.example.sennaccess.ui.campoVisible
import com.example.sennaccess.ui.theme.ErrorText
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

// Fondo común de las pantallas de auth: color del tema + luces ambientales de
// vidrio + respeto a las barras del sistema. Todo el contenido va dentro.
@Composable
fun AuthBackdrop(
    isDark: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        GlowSpheres(isDark = isDark)
        content()
    }
}

// Logo SENA oficial (recurso local) con el tamaño solicitado.
@Composable
fun AuthLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_sena),
        contentDescription = "Logo SENA",
        modifier = modifier
    )
}

// Botón que alterna tema claro/oscuro; siempre con descripción accesible.
@Composable
fun ThemeToggleButton(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    IconButton(
        onClick = onToggleTheme,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = if (isDark) "Cambiar a modo claro" else "Cambiar a modo oscuro",
            tint = colors.textPrimary
        )
    }
}

// Campo de texto de las pantallas de auth: teclado e imeAction correctos por
// tipo, autofill para gestores de contraseñas, error visual accesible y, en el
// caso de contraseña, toggle mostrar/ocultar con descripción accesible.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = LocalAppColors.current
    val focusManager = LocalFocusManager.current
    var showPassword by remember { mutableStateOf(false) }
    val transformation =
        if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .campoVisible(),
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = {
            if (leadingIcon != null) {
                Icon(imageVector = leadingIcon, contentDescription = null)
            }
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(
                    onClick = { showPassword = !showPassword }
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            }
        } else null,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        visualTransformation = transformation,
        isError = isError,
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        shape = RoundedCornerShape(16.dp),
        colors = campoAuthColors()
    )
}

// Cajita de error semántica para feedback de formularios y de servidor.
@Composable
fun ErrorBox(texto: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.errorBackground, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = texto,
            color = ErrorText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Colores de los campos de texto de auth según el tema activo.
@Composable
private fun campoAuthColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = LocalAppColors.current.inputBackground,
    unfocusedContainerColor = LocalAppColors.current.inputBackground,
    disabledContainerColor = LocalAppColors.current.inputBackground.copy(alpha = 0.6f),
    focusedTextColor = LocalAppColors.current.inputText,
    unfocusedTextColor = LocalAppColors.current.inputText,
    disabledTextColor = LocalAppColors.current.inputText.copy(alpha = 0.5f),
    focusedLabelColor = LocalAppColors.current.inputLabel,
    unfocusedLabelColor = LocalAppColors.current.inputLabel,
    disabledLabelColor = LocalAppColors.current.inputLabel.copy(alpha = 0.5f),
    focusedIndicatorColor = SenaGreen,
    unfocusedIndicatorColor = LocalAppColors.current.border,
    errorIndicatorColor = ErrorText,
    focusedLeadingIconColor = LocalAppColors.current.inputLabel,
    unfocusedLeadingIconColor = LocalAppColors.current.inputLabel,
    focusedTrailingIconColor = LocalAppColors.current.inputLabel,
    unfocusedTrailingIconColor = LocalAppColors.current.inputLabel,
    cursorColor = SenaGreen,
    errorCursorColor = ErrorText,
    errorLabelColor = ErrorText,
    errorSupportingTextColor = ErrorText,
    errorLeadingIconColor = ErrorText,
    errorTrailingIconColor = ErrorText
)