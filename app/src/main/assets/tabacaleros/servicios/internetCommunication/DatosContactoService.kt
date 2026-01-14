package com.codeco.tabacaleros.servicios.internetCommunication

import android.content.Context
import com.codeco.tabacaleros.BuildConfig
import com.codeco.tabacaleros.R
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.model.DatosContactoRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.DatosContactoResponse
import com.codeco.tabacaleros.servicios.internetCommunication.network.DatosContactoApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import kotlinx.coroutines.runBlocking

class DatosContactoService(private val context: Context, private val sessionManager: SessionManager) {

    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking { sessionManager.tokenFlow.first() }
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        chain.proceed(request)
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
        try {
            val trustAllCerts = arrayOf<X509TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0])
                .hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private val retrofit: Retrofit

    init {
        val okHttpClientBuilder = getUnsafeOkHttpClient()
            .addInterceptor(authInterceptor)

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + BuildConfig.API_DIRECTION)
            .client(okHttpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: DatosContactoApiService by lazy {
        retrofit.create(DatosContactoApiService::class.java)
    }

    suspend fun getDatosContacto(cuil: String): DatosContactoResponse {
        val request = DatosContactoRequest(cuil)
        return apiService.getDatosContacto(request)
    }
}
