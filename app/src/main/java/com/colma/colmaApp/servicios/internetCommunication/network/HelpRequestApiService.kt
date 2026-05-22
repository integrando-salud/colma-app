package com.colma.colmaApp.servicios.internetCommunication.network

import com.colma.colmaApp.servicios.internetCommunication.model.HelpRequestResponse
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface HelpRequestApiService {
    @Headers("Accept: application/json")
    @POST("api/pedido-ayuda")
    suspend fun requestHelp(
        @Header("Authorization") token: String
    ): Response<HelpRequestResponse>
}
