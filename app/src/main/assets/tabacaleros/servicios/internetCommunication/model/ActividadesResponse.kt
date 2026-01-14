package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class ActividadesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("productor_id") val productorId: Int,
    @SerializedName("actividades_actualizadas") val actividadesActualizadas: List<Int>,
    @SerializedName("total_actividades") val totalActividades: Int,
    @SerializedName("actividades") val actividades: List<ActividadItem>
)
