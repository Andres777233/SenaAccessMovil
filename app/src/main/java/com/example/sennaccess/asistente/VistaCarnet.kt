package com.example.sennaccess.asistente

// Carnet digital del usuario (como el del web): muestra los datos de la
// cuenta con su QR de identificación y permite guardarlo en la galería.

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.autenticacion.invitado.generarImagenQr
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.datos.modelos.Rol
import com.example.sennaccess.datos.sesion.GestorSesion
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun VistaCarnet(perfilEstado: EstadoCarga<UsuarioApi>, onBack: () -> Unit) {
    val colors = ColoresAppLocal.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val usuario = (perfilEstado as? EstadoCarga.Success)?.datos
        ?: UsuarioApi(
            id_usuario = GestorSesion.userId,
            user_name = GestorSesion.userName,
            user_email = GestorSesion.userEmail,
            role = Rol(id_rol = null, rol_name = GestorSesion.userRole)
        )

    var guardando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }

    val contenidoQr = remember(usuario) {
        JSONObject()
            .put("tipo", "SENA_ACCESS")
            .put("id", usuario?.id_usuario)
            .put("nombre", usuario?.nombreCompleto)
            .put("email", usuario?.user_email)
            .put("documento", usuario?.user_identification)
            .put("ficha", usuario?.user_coursenumber)
            .put("rol", usuario?.role?.rol_name)
            .toString()
    }
    val qr = remember(contenidoQr) { generarImagenQr(contenidoQr, 640) }

    fun guardarEnGaleria() {
        val bmp = qr ?: return
        guardando = true
        aviso = null
        scope.launch {
            try {
                val nombre = "carnet_sena_${usuario?.user_identification ?: "usuario"}.jpg"
                val valores = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, nombre)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SenaAccess")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores)
                    ?: throw IllegalStateException("No se pudo crear el archivo")
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                        throw IllegalStateException("No se pudo escribir la imagen")
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    valores.clear()
                    valores.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, valores, null, null)
                }
                guardando = false
                esError = false
                aviso = "Carnet guardado en la galería."
            } catch (e: Exception) {
                guardando = false
                esError = true
                aviso = "No se pudo guardar: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
            }
            Column {
                Text("Mi carnet", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Tu identificación digital", color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .superficiePlana(cornerRadius = RadioSena.lg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(usuario?.nombreCompleto ?: "Sin nombre", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(
                "${usuario?.role?.rol_name ?: "—"} • ${usuario?.user_documento_tipo ?: "CC"} ${usuario?.user_identification ?: "—"}",
                color = colors.textSecondary, fontSize = 13.sp
            )
            if ((usuario?.user_coursenumber ?: 0) > 0) {
                Text("Ficha ${usuario?.user_coursenumber} • ${usuario?.user_program ?: ""}", color = colors.textSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (qr != null) {
                Image(bitmap = qr.asImageBitmap(), contentDescription = "QR del carnet", modifier = Modifier.size(240.dp))
            } else {
                Text("No se pudo generar el QR.", color = RojoError, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(usuario?.user_email ?: "", color = colors.textSecondary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (aviso != null) {
            Text(
                aviso!!,
                color = if (esError) RojoError else verdeMarca(),
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        BotonPrimarioNeon(
            text = "GUARDAR EN GALERÍA",
            icon = Icons.Default.Download,
            onClick = ::guardarEnGaleria,
            loading = guardando,
            enabled = qr != null && !guardando,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
