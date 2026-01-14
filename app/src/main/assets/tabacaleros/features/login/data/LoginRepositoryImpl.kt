package com.codeco.tabacaleros.features.login.data

import com.codeco.tabacaleros.features.login.domain.LoginRepository
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.LoginService
import com.codeco.tabacaleros.servicios.internetCommunication.model.LoginRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.LoginResponse
import kotlinx.coroutines.flow.first

class LoginRepositoryImpl(
    private val loginService: LoginService,
    private val sessionManager: SessionManager
) : LoginRepository {

    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(name = email, password = password)
            val response = loginService.login(request)

            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null && loginResponse.success) {
                    Result.success(loginResponse)
                } else {
                    Result.failure(Exception("Login failed: ${response.message()}"))
                }
            } else {
                Result.failure(Exception("Login HTTP error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveSession(loginResponse: LoginResponse, email: String) {
        sessionManager.saveSession(
            token = loginResponse.token,
            email = email,
            userId = loginResponse.userId,
            userIdContacto = loginResponse.userIdContacto,
            componentes = loginResponse.componentes
        )
    }

    override suspend fun isLoggedIn(): Boolean {
        return !sessionManager.tokenFlow.first().isNullOrBlank()
    }
}
