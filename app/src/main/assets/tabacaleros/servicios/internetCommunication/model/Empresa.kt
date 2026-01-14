package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class Empresa(
    @SerializedName("id") val id: Int,
    @SerializedName("apellidoynombre") val apellidoynombre: String
)
