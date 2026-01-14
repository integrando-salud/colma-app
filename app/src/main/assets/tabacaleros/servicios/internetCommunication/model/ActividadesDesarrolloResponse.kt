package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class ActividadesDesarrolloResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("actividades") val actividades: List<ActividadDesarrolloItem>
)
