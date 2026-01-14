package com.codeco.tabacaleros.features.home.domain

import com.codeco.tabacaleros.servicios.internetCommunication.model.SubirImagenesProductorResponse
import java.io.File

interface HomeRepository {
    suspend fun uploadImages(
        idContacto: Int,
        dniFile: File,
        producerFile: File
    ): Result<SubirImagenesProductorResponse>
}
