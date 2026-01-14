package com.codeco.tabacaleros.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class DatosContactoRequest(
    @SerializedName("cuil") val cuil: String
)
