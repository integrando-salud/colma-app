package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class ActividadesRequest(
    @SerializedName("actividades") val actividades: String
)
