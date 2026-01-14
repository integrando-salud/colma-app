package com.codeco.tabacaleros.features.censo.domain

import com.codeco.tabacaleros.features.DNI.model.ProductorInfo
import com.codeco.tabacaleros.servicios.internetCommunication.model.Radicacion
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloResponse
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesResponse
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

interface CensoRepository {
    fun getProductorInfo(): Flow<ProductorInfo?>
    fun getUserId(): Flow<Int?>
    fun getSelectedRadicacion(): Flow<Radicacion?>
    suspend fun getActividadesDesarrollo(request: ActividadesDesarrolloRequest): Result<ActividadesDesarrolloResponse>
    suspend fun actualizarActividadesProductor(request: ActividadesRequest): Result<Response<ActividadesResponse>>
    suspend fun clearProducerInfo()
}
