package com.codeco.tabacaleros.servicios.internetCommunication

import com.codeco.tabacaleros.BuildConfig
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.model.SubirImagenesProductorRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.SubirImagenesProductorResponse
import com.codeco.tabacaleros.servicios.internetCommunication.network.SubirImagenesProductorApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SubirImagenesProductorService(private val sessionManager: SessionManager) {

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = runBlocking { sessionManager.tokenFlow.first() }

        val requestBuilder = originalRequest.newBuilder()
        token?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL + BuildConfig.API_DIRECTION)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: SubirImagenesProductorApiService = retrofit.create(SubirImagenesProductorApiService::class.java)

    suspend fun subirImagenesProductor(request: SubirImagenesProductorRequest): Response<SubirImagenesProductorResponse> {
        val idContactoBody = request.idContacto.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val idDocumentoDniBody = request.idDocumentoDni.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val idDocumentoFotoBody = request.idDocumentoFoto.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val imagenDniBody = request.imagenDni.asRequestBody("image/*".toMediaTypeOrNull())
        val imagenDniPart = MultipartBody.Part.createFormData("imagen_dni", request.imagenDni.name, imagenDniBody)

        val imagenFotoBody = request.imagenFoto.asRequestBody("image/*".toMediaTypeOrNull())
        val imagenFotoPart = MultipartBody.Part.createFormData("imagen_foto", request.imagenFoto.name, imagenFotoBody)

        return apiService.subirImagenesProductor(
            idContactoBody,
            idDocumentoDniBody,
            idDocumentoFotoBody,
            imagenDniPart,
            imagenFotoPart
        )
    }
}
