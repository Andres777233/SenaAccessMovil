package com.example.sennaccess.data

// Punto único de configuración de Retrofit para toda la app.
// Construye el cliente HTTP (OkHttp) y dos instancias de ApiService: una que apunta
// al backend por USB (mediante adb reverse) y otra por la IP del PC en la red WiFi.

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Celular físico por USB: adb reverse tcp:8000 tcp:8000 redirige 127.0.0.1 del celular al PC
    private const val BASE_URL_USB = "http://127.0.0.1:8000/api/"
    // Celular físico por WiFi (misma red). Si el DHCP cambia la IP del PC, actualizar aquí.
    private const val BASE_URL_WIFI = "http://192.168.1.8:8000/api/"
    // Backend desplegado en Railway (URL pública, HTTPS). CAMBIAR por el dominio real tras el deploy.
    private const val BASE_URL_REMOTE = "https://CAMBIA-POR-TU-DOMINIO.railway.app/api/"

    // 1. Interceptor de logs: registra cada petición y respuesta HTTP con su cuerpo.
    //    El nivel BODY sirve para depurar; en producción convendría reducirlo o quitarlo.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2. Cliente HTTP compartido por ambas rutas. El timeout de conexión es corto (3s)
    //    para que el fallback a la otra ruta sea rápido; el de lectura es más amplio.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    // 3. Construcción de Retrofit: combina la URL base, el cliente OkHttp y el
    //    convertidor Gson (JSON a data classes). Devuelve la implementación de ApiService.
    private fun construir(url: String): ApiService =
        Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

    // 4. Tres instancias de ApiService creadas con lazy: se construyen una sola vez,
    //    en el momento en que se usan por primera vez.
    private val servicioUsb: ApiService by lazy { construir(BASE_URL_USB) }
    private val servicioWifi: ApiService by lazy { construir(BASE_URL_WIFI) }
    private val servicioRemoto: ApiService by lazy { construir(BASE_URL_REMOTE) }

    // Recuerda cuál ruta respondió la última llamada exitosa; la usa raizServidor().
    @Volatile
    private var rutaActivaEsUsb: Boolean = true
    @Volatile
    private var rutaActivaEsRemota: Boolean = false

    // 5. Flujo de fallback: USB -> WiFi (misma red) -> Railway (remoto/HTTPS).
    //    El remoto es el último recurso cuando no hay cable ni misma red que el PC.
    suspend fun <T> conServicio(bloque: suspend (ApiService) -> T): T {
        try {
            val resultado = bloque(servicioUsb)
            rutaActivaEsUsb = true
            rutaActivaEsRemota = false
            return resultado
        } catch (e: IOException) {
            try {
                val resultado = bloque(servicioWifi)
                rutaActivaEsUsb = false
                rutaActivaEsRemota = false
                return resultado
            } catch (e2: IOException) {
                val resultado = bloque(servicioRemoto)
                rutaActivaEsUsb = false
                rutaActivaEsRemota = true
                return resultado
            }
        }
    }

    // 6. Raíz del servidor activo (sin "api/"): sirve para resolver rutas relativas del
    // backend, como las fotos de perfil "/avatars/x.jpg", según la ruta que funcione.
    fun raizServidor(): String {
        val base = when {
            rutaActivaEsRemota -> BASE_URL_REMOTE
            rutaActivaEsUsb -> BASE_URL_USB
            else -> BASE_URL_WIFI
        }
        return base.removeSuffix("api/")
    }
}
