package com.example.sennaccess.data

// Punto único de configuración de Retrofit para toda la app.
// La app habla SIEMPRE con el backend desplegado en Railway (HTTPS); sin
// fallbacks locales para no caer en servidores desactualizados.

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Backend desplegado en Railway (URL pública, HTTPS). Único destino de la app.
    private const val BASE_URL_REMOTE = "https://senaaccessweb-production.up.railway.app/api/"

    // 1. Interceptor de logs: registra cada petición y respuesta HTTP con su cuerpo.
    //    El nivel BODY sirve para depurar; en producción convendría reducirlo o quitarlo.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2. Cliente HTTP con tiempos holgados: la conexión tarda hasta 10s (arranque
    //    en frío de Railway) y la lectura hasta 60s (subidas a Cloudinary incluidas).
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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

    // 4. Instancia única del servicio remoto, creada una sola vez (lazy).
    private val servicioRemoto: ApiService by lazy { construir(BASE_URL_REMOTE) }

    // Entrega el servicio para ejecutar una llamada contra Railway.
    suspend fun <T> conServicio(bloque: suspend (ApiService) -> T): T = bloque(servicioRemoto)

    // 6. Raíz del servidor activo (sin "api/"): sirve para resolver rutas relativas
    //    del backend, como las fotos de perfil "/avatars/x.jpg".
    fun raizServidor(): String = BASE_URL_REMOTE.removeSuffix("api/")
}
