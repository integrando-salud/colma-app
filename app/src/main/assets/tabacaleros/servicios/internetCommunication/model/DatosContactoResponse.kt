package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class DatosContactoResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("id_productor") val idProductor: Int,
    @SerializedName("cuil") val cuil: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("fechanacimiento") val fechaNacimiento: String?,
    @SerializedName("sexo") val sexo: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("url_dni") val urlDni: String?,
    @SerializedName("url_foto") val urlFoto: String?
)
