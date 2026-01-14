package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class RadicacionesRequest(
    @SerializedName("id_categoria") val idCategoria: Int,
    @SerializedName("id_contacto") val idContacto: Int
)
