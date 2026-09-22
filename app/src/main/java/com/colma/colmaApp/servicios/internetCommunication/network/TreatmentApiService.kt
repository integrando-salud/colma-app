package com.colma.colmaApp.servicios.internetCommunication.network

import com.colma.colmaApp.servicios.internetCommunication.model.DatosFuncionalesResponse
import com.colma.colmaApp.servicios.internetCommunication.model.ElementosCompletadosResponse
import com.colma.colmaApp.servicios.internetCommunication.model.MultimediaCompletadosResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Url

interface TreatmentApiService {
    @Headers("Accept: application/json")
    @GET("api/tratamiento")
    suspend fun getTratamiento(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @Headers("Accept: application/json")
    @GET
    suspend fun downloadFile(
        @Header("Authorization") token: String,
        @Url fileUrl: String
    ): Response<ResponseBody>

    @Headers("Accept: application/json")
    @GET("api/elementos-completados")
    suspend fun getElementosCompletados(
        @Header("Authorization") token: String
    ): Response<ElementosCompletadosResponse>

    @Headers("Accept: application/json")
    @GET("api/multimedia-completados")
    suspend fun getMultimediaCompletados(
        @Header("Authorization") token: String
    ): Response<MultimediaCompletadosResponse>

    @Headers("Accept: application/json")
    @GET("api/datos-funcionales-tratamiento")
    suspend fun getDatosFuncionalesTratamiento(
        @Header("Authorization") token: String
    ): Response<DatosFuncionalesResponse>
}



