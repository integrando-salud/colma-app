package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class TabacoActividad(
    @SerializedName("id") val id: Int,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("id_concepto_tipo_clase") val idConceptoTipoClase: Int
)
