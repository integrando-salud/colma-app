package com.colma.colmaApp.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class HelpRequestResponse(
    val message: String,
    val error: String? = null
)
