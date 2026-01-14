package com.codeco.tabacaleros.servicios.internetCommunication.network

import com.codeco.tabacaleros.servicios.internetCommunication.model.RadicacionesRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.RadicacionesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RadicacionesApiService {
    @POST("radicaciones")
    suspend fun getRadicaciones(@Body request: RadicacionesRequest): Response<RadicacionesResponse>
}
