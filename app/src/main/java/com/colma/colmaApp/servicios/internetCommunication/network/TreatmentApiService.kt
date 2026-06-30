package com.colma.colmaApp.servicios.internetCommunication.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

import retrofit2.http.Headers

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
}
