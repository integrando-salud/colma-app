package com.colma.colmaApp.servicios.internetCommunication.network

import com.colma.colmaApp.servicios.internetCommunication.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface LoginApiService {
    @POST("api/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("documento") documento: String,
        @Field("password") password: String
    ): Response<LoginResponse>
}
