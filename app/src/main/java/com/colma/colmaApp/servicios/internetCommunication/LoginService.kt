package com.colma.colmaApp.servicios.internetCommunication

import com.colma.colmaApp.BuildConfig // Import BuildConfig
import com.colma.colmaApp.servicios.internetCommunication.network.LoginApiService
import com.colma.colmaApp.servicios.internetCommunication.model.LoginResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory // Assuming GsonConverterFactory is used

class LoginService {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_HOST) // Use the API_HOST from BuildConfig
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: LoginApiService = retrofit.create(LoginApiService::class.java)

    suspend fun login(documento: String, password: String): Response<LoginResponse> {
        return apiService.login(documento, password)
    }
}
