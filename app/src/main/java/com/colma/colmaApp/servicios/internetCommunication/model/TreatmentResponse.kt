package com.colma.colmaApp.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class ServerTreatmentResponse(
    @SerializedName("diagnostico") val diagnostico: String? = null,
    @SerializedName("tratamiento") val tratamiento: ServerTreatment? = null
)

data class ServerTreatment(
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("semanas") val semanas: List<ServerSemana>? = null
)

data class ServerSemana(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("sesiones") val sesiones: List<ServerSesion>? = null
)

data class ServerSesion(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("content") val content: List<ServerContentItem>? = null
)

data class ServerContentItem(
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("link") val link: String? = null
)
