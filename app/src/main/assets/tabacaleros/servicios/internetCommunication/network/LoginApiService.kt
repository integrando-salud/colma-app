package com.codeco.tabacaleros.servicios.internetCommunication.network

import com.codeco.tabacaleros.servicios.internetCommunication.model.LoginRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}