package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class Component(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String
)
