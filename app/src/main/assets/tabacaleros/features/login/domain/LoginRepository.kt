package com.codeco.tabacaleros.features.login.domain

import com.codeco.tabacaleros.servicios.internetCommunication.model.LoginResponse

/**
 * Repository interface for the login feature.
 */
interface LoginRepository {
    suspend fun login(email: String, password: String): Result<LoginResponse>
    suspend fun saveSession(loginResponse: LoginResponse, email: String)
    suspend fun isLoggedIn(): Boolean
}
