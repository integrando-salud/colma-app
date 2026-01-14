package com.codeco.tabacaleros.servicios.internetCommunication

import com.codeco.tabacaleros.BuildConfig
import com.codeco.tabacaleros.servicios.internetCommunication.model.LoginRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.LoginResponse
import com.codeco.tabacaleros.servicios.internetCommunication.network.LoginApiService
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginService {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL + BuildConfig.API_DIRECTION)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: LoginApiService = retrofit.create(LoginApiService::class.java)

    suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return apiService.login(request)
    }
}
