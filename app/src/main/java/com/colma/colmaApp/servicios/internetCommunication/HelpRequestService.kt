package com.colma.colmaApp.servicios.internetCommunication

import android.util.Log
import com.colma.colmaApp.BuildConfig
import com.colma.colmaApp.servicios.internetCommunication.model.HelpRequestResponse
import com.colma.colmaApp.servicios.internetCommunication.network.HelpRequestApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HelpRequestService {

    private val TAG = "HelpRequestService"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_HOST)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val apiService: HelpRequestApiService = retrofit.create(HelpRequestApiService::class.java)

    suspend fun requestHelp(token: String): Response<HelpRequestResponse> {
        val bearerToken = "Bearer $token"
        
        return try {
            val rawResponse = apiService.requestHelp(bearerToken)
            
            if (rawResponse.isSuccessful) {
                Response.success(rawResponse.body(), rawResponse.raw())
            } else {
                val errorBodyString = rawResponse.errorBody()?.string() ?: ""
                Log.e(TAG, "Error response body: $errorBodyString")
                
                // We try to parse the error body if possible, or return a default error
                val errorObj = try {
                    gson.fromJson(errorBodyString, HelpRequestResponse::class.java)
                } catch (e: Exception) {
                    HelpRequestResponse(message = "Error al registrar el pedido de ayuda.", error = "Error desconocido")
                }
                
                // Return an error response but with the parsed or default error object as body?
                // Retrofit Response.error requires ResponseBody. We'll return the raw error.
                // It's up to the caller to handle the error parsing if needed.
                Response.error(rawResponse.errorBody()!!, rawResponse.raw())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during requestHelp: ${e.message}")
            throw e
        }
    }
}
