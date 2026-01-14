package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val success: Boolean,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_id_contacto") val userIdContacto: Int,
    val token: String,
    @SerializedName("componentes") val componentes: List<Component>
)