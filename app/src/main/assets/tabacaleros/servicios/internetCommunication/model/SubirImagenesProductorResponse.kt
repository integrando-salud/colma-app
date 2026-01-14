package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class SubirImagenesProductorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("id_contacto") val idContacto: Int,
    @SerializedName("archivos") val archivos: List<Archivo>,
    @SerializedName("url_dni") val urlDni: String?,
    @SerializedName("url_foto") val urlFoto: String?
)
