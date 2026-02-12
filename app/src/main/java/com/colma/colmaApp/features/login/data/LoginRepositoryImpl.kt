package com.colma.colmaApp.features.login.data

import android.content.Context
import android.util.Log // Import Log
import android.widget.Toast // Import Toast
import com.colma.colmaApp.features.login.domain.LoginRepository
import com.colma.colmaApp.servicios.internetCommunication.LoginService
import com.colma.colmaApp.servicios.internetCommunication.model.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginRepositoryImpl(
    private val context: Context,
    private val loginService: LoginService // Inject LoginService
) : LoginRepository {

    private val sharedPreferences = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    private val TOKEN_KEY = "auth_token"
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
                        sharedPreferences.edit().putString(TOKEN_KEY, loginResponse.token).apply()
                        Log.d(TAG, "Login successful. Token saved.")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Login exitoso!", Toast.LENGTH_SHORT).show()
                        }
                        Result.success(true)
                    } else {
                        val errorMessage = "Respuesta de login inválida: token no encontrado o vacío."
                        Log.e(TAG, errorMessage)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    val errorMessage = "Error en el login: ${response.code()} - $errorBody"
                    Log.e(TAG, errorMessage)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during login: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error de red o interno: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
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
            val isLoggedIn = sharedPreferences.getString(TOKEN_KEY, null)?.isNotEmpty() ?: false
            Log.d(TAG, "isLoggedIn: $isLoggedIn")
            isLoggedIn
        }
    }
}
