package com.colma.colmaApp.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val token: String,
    val usuario: Usuario
)

data class Usuario(
    val id: Int,
    val documento: String,
    val nombre: String,
    val email: String,
    val sexo: String,
    val pais: Pais,
    @SerializedName("centro_salud") val centroSalud: CentroSalud
)

data class Pais(
    val id: Int,
    val nombre: String
)

data class CentroSalud(
    val id: Int,
    val nombre: String
)
