package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class Archivo(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("id") val id: Int,
    @SerializedName("url") val url: String,
    @SerializedName("accion") val accion: String
)
