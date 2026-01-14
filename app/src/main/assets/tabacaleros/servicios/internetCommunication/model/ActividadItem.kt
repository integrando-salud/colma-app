package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class ActividadItem(
    @SerializedName("id") val id: Int,
    @SerializedName("telefono_censo_productor") val telefonoCensoProductor: String?,
    @SerializedName("censo_plantas_productor") val censoPlantasProductor: Int?,
    @SerializedName("censo_link_dni_productor") val censoLinkDniProductor: String?,
    @SerializedName("censo_link_foto_productor") val censoLinkFotoProductor: String?
)
