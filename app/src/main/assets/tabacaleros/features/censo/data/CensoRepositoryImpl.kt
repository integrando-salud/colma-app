package com.codeco.tabacaleros.features.censo.data

import android.util.Log
import com.codeco.tabacaleros.features.DNI.model.ProductorInfo
import com.codeco.tabacaleros.features.censo.domain.CensoRepository
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.ActividadesDesarrolloService
import com.codeco.tabacaleros.servicios.internetCommunication.ActividadesService
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloResponse
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesResponse
import com.codeco.tabacaleros.servicios.internetCommunication.model.Radicacion
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

class CensoRepositoryImpl(
    private val actividadesDesarrolloService: ActividadesDesarrolloService,
    private val actividadesService: ActividadesService,
    private val sessionManager: SessionManager
) : CensoRepository {

    override fun getProductorInfo(): Flow<ProductorInfo?> {
        return sessionManager.producerInfoFlow
    }

    override fun getUserId(): Flow<Int?> {
        return sessionManager.userIdFlow
    }

    override fun getSelectedRadicacion(): Flow<Radicacion?> {
        return sessionManager.selectedRadicacionFlow
    }

    override suspend fun getActividadesDesarrollo(request: ActividadesDesarrolloRequest): Result<ActividadesDesarrolloResponse> {
        return try {
            val response = actividadesDesarrolloService.getActividadesDesarrollo(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("Error fetching data: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Log.e("CensoRepositoryImpl", "Error fetching data: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun actualizarActividadesProductor(request: ActividadesRequest): Result<Response<ActividadesResponse>> {
        return try {
            val response = actividadesService.actualizarActividadesProductor(request)
            if (response.isSuccessful) {
                Result.success(response)
            } else {
                Result.failure(Exception("Error al actualizar actividades: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Log.e("CensoRepositoryImpl", "Excepción al actualizar actividades: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun clearProducerInfo() {
        sessionManager.clearProducerInfo()
    }
}
