package com.codeco.tabacaleros.servicios.internetCommunication.network

import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ActividadesDesarrolloApiService {
    @POST("actividades_desarrollo")
    suspend fun getActividadesDesarrollo(@Body request: ActividadesDesarrolloRequest): Response<ActividadesDesarrolloResponse>
}
