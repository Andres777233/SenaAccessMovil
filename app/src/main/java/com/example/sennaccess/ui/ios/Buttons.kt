package com.example.sennaccess.ui.ios

// Botones base de la app con estilo vidrio SENA. Centralizan la apariencia de
// las acciones primarias y secundarias: estados pressed/disabled/loading con
// micro-interacción consistente en toda la aplicación.
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

// Botón principal verde SENA con glow. Soporta estado de carga (loading) que
// reemplaza el texto por un spinner y bloquea la pulsación.
@Composable
fun PrimaryNeonButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .pressScale(pressedScale = 0.97f)
            .shadow(
                15.dp,
                RoundedCornerShape(28.dp),
                spotColor = SenaGreen.copy(alpha = 0.6f)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = SenaGreen,
            contentColor = LocalAppColors.current.textOnPrimary,
            disabledContainerColor = SenaGreen.copy(alpha = 0.35f),
            disabledContentColor = LocalAppColors.current.textOnPrimary.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = LocalAppColors.current.textOnPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (loading) loadingText(text) else text.uppercase(),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// Botón secundario con borde verde transparente; mantiene la identidad visual
// sin robar protagonismo al botón primario.
@Composable
fun GlowOutlinedButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalAppColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.pressScale(pressedScale = 0.97f),
        border = BorderStroke(2.dp, SenaGreen.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textSecondary
        ),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SenaGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Texto que se muestra sobre el spinner de carga, sin gritar.
private fun loadingText(text: String): String = text.uppercase()