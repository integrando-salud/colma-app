package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class RadicacionesResponse(
    val success: Boolean,
    @SerializedName("id_entidad") val idEntidad: Int,
    val radicaciones: List<Radicacion>
)

data class Radicacion(
    val id: Int,
    val descripcion: String,
    @SerializedName("direccion_calle") val direccionCalle: String
)
