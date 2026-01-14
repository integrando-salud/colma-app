package com.codeco.tabacaleros.features.home.data

import com.codeco.tabacaleros.features.home.domain.HomeRepository
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.SubirImagenesProductorService
import com.codeco.tabacaleros.servicios.internetCommunication.model.SubirImagenesProductorRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.SubirImagenesProductorResponse
import java.io.File

class HomeRepositoryImpl(
    private val subirImagenesProductorService: SubirImagenesProductorService,
    private val sessionManager: SessionManager
) : HomeRepository {
    override suspend fun uploadImages(
        idContacto: Int,
        dniFile: File,
        producerFile: File
    ): Result<SubirImagenesProductorResponse> {
        return try {
            val request = SubirImagenesProductorRequest(
                idContacto = idContacto,
                idDocumentoDni = 1, // Hardcoded as in MainViewModel
                idDocumentoFoto = 3, // Hardcoded as in MainViewModel
                imagenDni = dniFile,
                imagenFoto = producerFile
            )
            val response = subirImagenesProductorService.subirImagenesProductor(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Upload failed: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
