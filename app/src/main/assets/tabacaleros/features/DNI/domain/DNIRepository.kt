package com.codeco.tabacaleros.features.DNI.domain

import com.codeco.tabacaleros.features.DNI.model.ProductorInfo
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloResponse

interface DNIRepository {
    suspend fun getProductorInfo(cuil: String): Result<ProductorInfo>
    suspend fun saveProductorInfo(productorInfo: ProductorInfo)
    suspend fun getActividades(idProductor: Int): Result<ActividadesDesarrolloResponse>
}
