package com.colma.colmaApp.servicios.internetCommunication

import com.colma.colmaApp.BuildConfig
import com.colma.colmaApp.servicios.internetCommunication.network.TreatmentApiService
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class TreatmentService {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor { message ->
            try {
                android.util.Log.d("TreatmentService", message)
            } catch (e: Throwable) {
                println("TreatmentService: $message")
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_HOST)
        .client(okHttpClient)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    private val apiService: TreatmentApiService = retrofit.create(TreatmentApiService::class.java)

    suspend fun getTratamiento(token: String): Response<ResponseBody> {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return apiService.getTratamiento(bearerToken)
    }

    suspend fun downloadFile(token: String, fileUrl: String): Response<ResponseBody> {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return apiService.downloadFile(bearerToken, fileUrl)
    }

    suspend fun getElementosCompletados(token: String): Response<com.colma.colmaApp.servicios.internetCommunication.model.ElementosCompletadosResponse> {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return apiService.getElementosCompletados(bearerToken)
    }

    suspend fun getMultimediaCompletados(token: String): Response<com.colma.colmaApp.servicios.internetCommunication.model.MultimediaCompletadosResponse> {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return apiService.getMultimediaCompletados(bearerToken)
    }

    suspend fun getDatosFuncionalesTratamiento(token: String): Response<com.colma.colmaApp.servicios.internetCommunication.model.DatosFuncionalesResponse> {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return apiService.getDatosFuncionalesTratamiento(bearerToken)
    }
}


