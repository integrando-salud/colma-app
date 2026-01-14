package com.codeco.tabacaleros.servicios.internetCommunication

import com.codeco.tabacaleros.BuildConfig
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloResponse
import com.codeco.tabacaleros.servicios.internetCommunication.network.ActividadesDesarrolloApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ActividadesDesarrolloService(private val sessionManager: SessionManager) {

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = runBlocking { sessionManager.tokenFlow.first() }

        val requestBuilder = originalRequest.newBuilder()
        token?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL + BuildConfig.API_DIRECTION)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: ActividadesDesarrolloApiService = retrofit.create(ActividadesDesarrolloApiService::class.java)

    suspend fun getActividadesDesarrollo(request: ActividadesDesarrolloRequest): Response<ActividadesDesarrolloResponse> {
        return apiService.getActividadesDesarrollo(request)
    }
}
