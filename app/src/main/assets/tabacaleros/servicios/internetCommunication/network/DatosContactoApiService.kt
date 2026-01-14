package com.codeco.tabacaleros.servicios.internetCommunication.network

import com.codeco.tabacaleros.servicios.internetCommunication.model.DatosContactoRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.DatosContactoResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DatosContactoApiService {
    @POST("datos_contacto_tabacalero")
    suspend fun getDatosContacto(@Body request: DatosContactoRequest): DatosContactoResponse
}
