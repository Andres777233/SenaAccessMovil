package com.example.sennaccess.datos.red

// Punto único de configuración de Retrofit para toda la app.
// La app habla SIEMPRE con el backend desplegado en Railway (HTTPS); sin
// fallbacks locales para no caer en servidores desactualizados.

import com.example.sennaccess.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.sennaccess.datos.sesion.GestorSesion

object ClienteApi {

    private const val BASE_URL_REMOTE = "https://senaaccessweb-production-4c6e.up.railway.app/api/"

    // Sin cuerpos en el log: BASIC registra URL y código sin exponer
    // contraseñas, tokens ni códigos 2FA aunque el APK sea debug.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val sesionInterceptor = okhttp3.Interceptor { chain ->
        val response = try {
            chain.proceed(chain.request())
        } catch (e: Exception) {
            throw e
        }
        if (response.code == 401 && GestorSesion.token != null) {
            GestorSesion.clear()
        }
        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(sesionInterceptor)
        .build()

    private fun construir(url: String): ServicioApi =
        Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServicioApi::class.java)

    private val servicioRemoto: ServicioApi by lazy { construir(BASE_URL_REMOTE) }

    suspend fun <T> conServicio(bloque: suspend (ServicioApi) -> T): T = bloque(servicioRemoto)

    // Raíz del servidor sin /api, para rutas relativas como fotos.
    fun raizServidor(): String = BASE_URL_REMOTE.removeSuffix("api/")
}
