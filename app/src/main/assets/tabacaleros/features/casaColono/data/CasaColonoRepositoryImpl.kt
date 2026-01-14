package com.codeco.tabacaleros.features.casaColono.data

import com.codeco.tabacaleros.features.casaColono.domain.CasaColonoRepository
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.RadicacionesService
import com.codeco.tabacaleros.servicios.internetCommunication.model.Radicacion
import com.codeco.tabacaleros.servicios.internetCommunication.model.RadicacionesRequest
import kotlinx.coroutines.flow.first

class CasaColonoRepositoryImpl(
    private val radicacionesService: RadicacionesService,
    private val sessionManager: SessionManager
) : CasaColonoRepository {
    override suspend fun getRadicaciones(): Result<List<Radicacion>> {
        return try {
            val idContacto = sessionManager.userIdContactoFlow.first()
            if (idContacto != null) {
                val request = RadicacionesRequest(idCategoria = 9000, idContacto = idContacto)
                val response = radicacionesService.getRadicaciones(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(response.body()?.radicaciones ?: emptyList())
                } else {
                    Result.failure(Exception("Get Radicaciones failed: ${response.message()}"))
                }
            } else {
                Result.failure(Exception("User not logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}