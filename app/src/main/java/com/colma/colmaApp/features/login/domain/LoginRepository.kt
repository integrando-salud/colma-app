package com.colma.colmaApp.features.login.domain

/**
 * Repository interface for the login feature.
 */
interface LoginRepository {
    suspend fun login(dni: String, password: String): Result<Boolean>
    suspend fun saveSession()
    suspend fun isLoggedIn(): Boolean
}
