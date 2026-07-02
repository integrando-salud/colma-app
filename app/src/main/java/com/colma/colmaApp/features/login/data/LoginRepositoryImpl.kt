package com.colma.colmaApp.features.login.data

import android.content.Context
import android.util.Log // Import Log
import com.colma.colmaApp.features.login.domain.LoginRepository
import com.colma.colmaApp.utils.NetworkUtils
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.internetCommunication.LoginService
import com.colma.colmaApp.servicios.internetCommunication.model.LoginResponse
import com.colma.colmaApp.servicios.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginRepositoryImpl(
    private val loginService: LoginService, // Inject LoginService
    private val dbHelper: DbHelper,
    private val context: Context
) : LoginRepository {

    private val TAG = "LoginRepositoryImpl" // TAG for logging

    override suspend fun login(dni: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) { // Perform operation on IO dispatcher
            Log.d(TAG, "Attempting login for DNI: $dni")
            val isOnline = NetworkUtils.isInternetAvailable(context)
            if (isOnline) {
                try {
                    val response = loginService.login(documento = dni, password = password)

                    Log.d(TAG, "Login API response received. isSuccessful: ${response.isSuccessful}, Code: ${response.code()}")

                    if (response.isSuccessful) {
                        val loginResponse: LoginResponse? = response.body()
                        Log.d(TAG, "Login response body JSON: ${Gson().toJson(loginResponse)}")
                        if (loginResponse != null && loginResponse.token.isNotEmpty()) {
                            SessionManager.saveSession(loginResponse.token, loginResponse.usuario.id)
                            dbHelper.upsertUsuario(loginResponse.usuario) // Save the user data
                            
                            // Securely save credentials in the local database
                            dbHelper.saveOrUpdateCredential(
                                dni = dni,
                                passwordRaw = password,
                                tokenRaw = loginResponse.token,
                                idUsuario = loginResponse.usuario.id
                            )
                            Log.d(TAG, "Login successful. Token, user ID, user data, and credentials saved.")
                            Result.success(true)
                        } else {
                            val errorMessage = "Respuesta de login inválida: token no encontrado o vacío."
                            Log.e(TAG, errorMessage)
                            Result.failure(Exception(errorMessage))
                        }
                    } else {
                        // Fallback to local credential check for server errors (5xx)
                        if (response.code() >= 500) {
                            Log.w(TAG, "Server error ${response.code()}, trying offline fallback check")
                            dbHelper.verifyCredentialOffline(dni, password).map { (token, userId) ->
                                SessionManager.saveSession(token, userId)
                                true
                            }
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                            val errorMessage = "Error en el login: ${response.code()} - $errorBody"
                            Log.e(TAG, errorMessage)
                            Result.failure(Exception(errorMessage))
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to local credential check for network connectivity errors
                    Log.w(TAG, "Network exception: ${e.message}, trying offline fallback check", e)
                    dbHelper.verifyCredentialOffline(dni, password).map { (token, userId) ->
                        SessionManager.saveSession(token, userId)
                        true
                    }
                }
            } else {
                Log.d(TAG, "Device offline. Verifying credentials against local DB.")
                dbHelper.verifyCredentialOffline(dni, password).map { (token, userId) ->
                    SessionManager.saveSession(token, userId)
                    true
                }
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
