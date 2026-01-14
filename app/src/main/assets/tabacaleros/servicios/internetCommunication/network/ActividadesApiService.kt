package com.codeco.tabacaleros.servicios.internetCommunication.network

import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ActividadesApiService {
    @POST("actualizar_actividades_productor")
    suspend fun actualizarActividadesProductor(@Body request: ActividadesRequest): Response<ActividadesResponse>
}
