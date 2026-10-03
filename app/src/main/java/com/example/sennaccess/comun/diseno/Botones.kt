package com.example.sennaccess.comun.diseno

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
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
@Composable
fun BotonPrimarioNeon(
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
            .escalaPresion(pressedScale = 0.97f)
            .shadow(
                8.dp,
                RoundedCornerShape(16.dp),
                spotColor = VerdeSena.copy(alpha = 0.45f)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = VerdeSena,
            contentColor = ColoresAppLocal.current.textOnPrimary,
            disabledContainerColor = VerdeSena.copy(alpha = 0.35f),
            disabledContentColor = ColoresAppLocal.current.textOnPrimary.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 15.dp, horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ColoresAppLocal.current.textOnPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (loading) loadingText(text) else text.uppercase(),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun BotonBordeBrillante(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = ColoresAppLocal.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.escalaPresion(pressedScale = 0.97f),
        border = BorderStroke(1.5.dp, VerdeSena.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textSecondary
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 15.dp, horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = VerdeSena
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun loadingText(text: String): String = text.uppercase()
