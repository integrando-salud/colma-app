package com.codeco.tabacaleros.servicios.internetCommunication.model

import java.io.File

data class SubirImagenesProductorRequest(
    val idContacto: Int,
    val idDocumentoDni: Int,
    val idDocumentoFoto: Int,
    val imagenDni: File,
    val imagenFoto: File
)
