package com.codeco.tabacaleros.servicios.internetCommunication.network

import com.codeco.tabacaleros.servicios.internetCommunication.model.SubirImagenesProductorResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface SubirImagenesProductorApiService {
    @Multipart
    @POST("subir_imagenes_productor")
    suspend fun subirImagenesProductor(
        @Part("id_contacto") idContacto: RequestBody,
        @Part("id_documento_dni") idDocumentoDni: RequestBody,
        @Part("id_documento_foto") idDocumentoFoto: RequestBody,
        @Part imagenDni: MultipartBody.Part,
        @Part imagenFoto: MultipartBody.Part
    ): Response<SubirImagenesProductorResponse>
}
