package com.colma.colmaApp.servicios.internetCommunication.network

import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SyncApiService {
    @POST("api/sync-data-paciente")
    suspend fun syncData(
        @Header("Authorization") token: String,
        @Body syncDataRequest: SyncDataRequest
    ): Response<ResponseBody>
}
