package com.colma.colmaApp.servicios.internetCommunication

import com.colma.colmaApp.BuildConfig
import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest
import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataResponse
import com.colma.colmaApp.servicios.internetCommunication.network.SyncApiService
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SyncService {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_HOST)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: SyncApiService = retrofit.create(SyncApiService::class.java)

    suspend fun syncData(token: String, syncDataRequest: SyncDataRequest): Response<SyncDataResponse> {
        // The token should be passed as "Bearer <token>"
        val bearerToken = "Bearer $token"
        return apiService.syncData(bearerToken, syncDataRequest)
    }
}
