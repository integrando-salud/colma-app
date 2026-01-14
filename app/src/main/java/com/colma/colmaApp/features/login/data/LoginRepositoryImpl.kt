package com.colma.colmaApp.features.login.data

import android.content.Context
import com.colma.colmaApp.features.login.domain.LoginRepository
import kotlinx.coroutines.delay

class LoginRepositoryImpl(private val context: Context) : LoginRepository {

    override suspend fun login(dni: String, password: String): Result<Boolean> {
        return try {
            // Simulate network/database delay
            delay(1500)

            // Dummy validation: for now, any non-empty credentials are valid
            if (dni.isNotEmpty() && password.isNotEmpty()) {
                Result.success(true)
            } else {
                Result.failure(Exception("DNI y contraseña no pueden estar vacíos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveSession() {
        // Dummy implementation
        // In a real app, you would save a token or session flag to SharedPreferences here
        delay(100) // simulate I/O
    }

    override suspend fun isLoggedIn(): Boolean {
        // Dummy implementation
        // In a real app, you would check SharedPreferences for a valid session
        return false
    }
}
