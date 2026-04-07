package com.colma.colmaApp.servicios.internetCommunication

import com.colma.colmaApp.BuildConfig
import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest
import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataResponse
import com.colma.colmaApp.servicios.internetCommunication.network.SyncApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

class SyncService {

    private val TAG = "SyncService"

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

    private val apiService: SyncApiService = retrofit.create(SyncApiService::class.java)

    suspend fun syncData(token: String, syncDataRequest: SyncDataRequest): Response<SyncDataResponse> {
        val bearerToken = "Bearer $token"
        
        return try {
            val rawResponse = apiService.syncData(bearerToken, syncDataRequest)
            
            if (rawResponse.isSuccessful) {
                val bodyString = rawResponse.body()?.string() ?: ""
                Log.d(TAG, "Raw response body: $bodyString")
                
                // Try to parse as SyncDataResponse object
                val responseObj = try {
                    gson.fromJson(bodyString, SyncDataResponse::class.java)
                } catch (e: Exception) {
                    // If it's not a JSON object, treat the whole string as the message
                    // remove quotes if present
                    val cleanMessage = bodyString.trim().removeSurrounding("\"")
                    SyncDataResponse(message = cleanMessage)
                }
                
                Response.success(responseObj, rawResponse.raw())
            } else {
                // Return original error response but we need to map the type
                Response.error(rawResponse.errorBody()!!, rawResponse.raw())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during syncData: ${e.message}")
            throw e
        }
    }
}
