package com.colma.colmaApp.features.login.data

import android.util.Log // Import Log
import com.colma.colmaApp.features.login.domain.LoginRepository
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.servicios.internetCommunication.LoginService
import com.colma.colmaApp.servicios.internetCommunication.model.LoginResponse
import com.colma.colmaApp.servicios.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginRepositoryImpl(
    private val loginService: LoginService, // Inject LoginService
    private val dbHelper: DBhelper
) : LoginRepository {

    private val TAG = "LoginRepositoryImpl" // TAG for logging

    override suspend fun login(dni: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) { // Perform network operation on IO dispatcher
            Log.d(TAG, "Attempting login for DNI: $dni")
            try {
                val response = loginService.login(documento = dni, password = password)

                Log.d(TAG, "Login API response received. isSuccessful: ${response.isSuccessful}, Code: ${response.code()}")

                if (response.isSuccessful) {
                    val loginResponse: LoginResponse? = response.body()
                    if (loginResponse != null && loginResponse.token.isNotEmpty()) {
                        SessionManager.saveSession(loginResponse.token, loginResponse.usuario.id)
                        dbHelper.upsertUsuario(loginResponse.usuario) // Save the user data
                        Log.d(TAG, "Login successful. Token, user ID, and user data saved.")
                        Result.success(true)
                    } else {
                        val errorMessage = "Respuesta de login inválida: token no encontrado o vacío."
                        Log.e(TAG, errorMessage)
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    val errorMessage = "Error en el login: ${response.code()} - $errorBody"
                    Log.e(TAG, errorMessage)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during login: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun saveSession() {
        // Session is saved automatically upon successful login by storing the token
        // This function might be used if there's other session-related data to save
        // For now, it remains a dummy or can be removed if not needed for anything else.
    }

    override suspend fun isLoggedIn(): Boolean {
        return withContext(Dispatchers.IO) {
            val isLoggedIn = SessionManager.getAuthToken()?.isNotEmpty() ?: false
            Log.d(TAG, "isLoggedIn: $isLoggedIn")
            isLoggedIn
        }
    }
}
