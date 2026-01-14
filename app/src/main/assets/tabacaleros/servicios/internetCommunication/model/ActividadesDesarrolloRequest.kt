package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class ActividadesDesarrolloRequest(
    @SerializedName("id_productor") val idProductor: Int,
    @SerializedName("id_categoria") val idCategoria: Int
)
