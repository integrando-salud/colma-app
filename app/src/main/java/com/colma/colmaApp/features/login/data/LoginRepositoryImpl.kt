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
            
            // Mock test login for QA and UI verification of corrected treatment payloads
            if ((dni.trim() == "43527946" || dni.trim() == "usuarioRandom") && password.trim() == "soyunusuario") {
                Log.d(TAG, "Test credentials entered: logging in with mock DBT+HTA Argentina user.")
                val mockUser = com.colma.colmaApp.servicios.internetCommunication.model.Usuario(
                    id = 9999,
                    documento = "43527946",
                    nombre = "Usuario DBT+HTA Prueba",
                    email = "test_dbt_hta@colma.app",
                    sexo = "M",
                    pais = com.colma.colmaApp.servicios.internetCommunication.model.Pais(id = 1, nombre = "Argentina"),
                    centroSalud = com.colma.colmaApp.servicios.internetCommunication.model.CentroSalud(id = 1, nombre = "Centro de Salud de Prueba"),
                    primerLogin = null
                )
                val mockToken = "mock_test_token_ar_dbt_hta"
                SessionManager.saveSession(mockToken, mockUser.id)
                dbHelper.upsertUsuario(mockUser)
                dbHelper.saveOrUpdateCredential(
                    dni = "43527946",
                    passwordRaw = password,
                    tokenRaw = mockToken,
                    idUsuario = mockUser.id
                )
                return@withContext Result.success(true)
            } else if (dni.trim() == "43527947" && password.trim() == "soyunusuario") {
                Log.d(TAG, "Test credentials entered: logging in with mock DBT Argentina user.")
                val mockUser = com.colma.colmaApp.servicios.internetCommunication.model.Usuario(
                    id = 9998,
                    documento = "43527947",
                    nombre = "Usuario Diabetes Prueba",
                    email = "test_dbt@colma.app",
                    sexo = "M",
                    pais = com.colma.colmaApp.servicios.internetCommunication.model.Pais(id = 1, nombre = "Argentina"),
                    centroSalud = com.colma.colmaApp.servicios.internetCommunication.model.CentroSalud(id = 1, nombre = "Centro de Salud de Prueba"),
                    primerLogin = null
                )
                val mockToken = "mock_test_token_ar_dbt"
                SessionManager.saveSession(mockToken, mockUser.id)
                dbHelper.upsertUsuario(mockUser)
                dbHelper.saveOrUpdateCredential(
                    dni = "43527947",
                    passwordRaw = password,
                    tokenRaw = mockToken,
                    idUsuario = mockUser.id
                )
                return@withContext Result.success(true)
            } else if (dni.trim() == "43527948" && password.trim() == "soyunusuario") {
                Log.d(TAG, "Test credentials entered: logging in with mock HTA Argentina user.")
                val mockUser = com.colma.colmaApp.servicios.internetCommunication.model.Usuario(
                    id = 9997,
                    documento = "43527948",
                    nombre = "Usuario Hipertensión Prueba",
                    email = "test_hta@colma.app",
                    sexo = "M",
                    pais = com.colma.colmaApp.servicios.internetCommunication.model.Pais(id = 1, nombre = "Argentina"),
                    centroSalud = com.colma.colmaApp.servicios.internetCommunication.model.CentroSalud(id = 1, nombre = "Centro de Salud de Prueba"),
                    primerLogin = null
                )
                val mockToken = "mock_test_token_ar_hta"
                SessionManager.saveSession(mockToken, mockUser.id)
                dbHelper.upsertUsuario(mockUser)
                dbHelper.saveOrUpdateCredential(
                    dni = "43527948",
                    passwordRaw = password,
                    tokenRaw = mockToken,
                    idUsuario = mockUser.id
                )
                return@withContext Result.success(true)
            } else if (dni.trim() == "43527949" && password.trim() == "soyunusuario") {
                Log.d(TAG, "Test credentials entered: logging in with mock DBT+HTA Perú user.")
                val mockUser = com.colma.colmaApp.servicios.internetCommunication.model.Usuario(
                    id = 9996,
                    documento = "43527949",
                    nombre = "Usuario DBT+HTA Perú Prueba",
                    email = "test_pe_dbt_hta@colma.app",
                    sexo = "M",
                    pais = com.colma.colmaApp.servicios.internetCommunication.model.Pais(id = 2, nombre = "Perú"),
                    centroSalud = com.colma.colmaApp.servicios.internetCommunication.model.CentroSalud(id = 2, nombre = "Centro de Salud de Prueba Perú"),
                    primerLogin = null
                )
                val mockToken = "mock_test_token_pe_dbt_hta"
                SessionManager.saveSession(mockToken, mockUser.id)
                dbHelper.upsertUsuario(mockUser)
                dbHelper.saveOrUpdateCredential(
                    dni = "43527949",
                    passwordRaw = password,
                    tokenRaw = mockToken,
                    idUsuario = mockUser.id
                )
                return@withContext Result.success(true)
            } else if (dni.trim() == "43527950" && password.trim() == "soyunusuario") {
                Log.d(TAG, "Test credentials entered: logging in with mock DBT Perú user.")
                val mockUser = com.colma.colmaApp.servicios.internetCommunication.model.Usuario(
                    id = 9995,
                    documento = "43527950",
                    nombre = "Usuario Diabetes Perú Prueba",
                    email = "test_pe_dbt@colma.app",
                    sexo = "M",
                    pais = com.colma.colmaApp.servicios.internetCommunication.model.Pais(id = 2, nombre = "Perú"),
                    centroSalud = com.colma.colmaApp.servicios.internetCommunication.model.CentroSalud(id = 2, nombre = "Centro de Salud de Prueba Perú"),
                    primerLogin = null
                )
                val mockToken = "mock_test_token_pe_dbt"
                SessionManager.saveSession(mockToken, mockUser.id)
                dbHelper.upsertUsuario(mockUser)
                dbHelper.saveOrUpdateCredential(
                    dni = "43527950",
                    passwordRaw = password,
                    tokenRaw = mockToken,
                    idUsuario = mockUser.id
                )
                return@withContext Result.success(true)
            }

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
                            val offlineResult = dbHelper.verifyCredentialOffline(dni, password)
                            if (offlineResult.isSuccess) {
                                val (token, userId) = offlineResult.getOrThrow()
                                SessionManager.saveSession(token, userId)
                                Result.success(true)
                            } else {
                                Result.failure(Exception("SERVER_ERROR: El servidor de Colma no se encuentra disponible (código ${response.code()}). Por favor, intenta más tarde."))
                            }
                        } else if (response.code() in 400..499) {
                            val errorBody = response.errorBody()?.string() ?: ""
                            Log.e(TAG, "Client error ${response.code()}: $errorBody")
                            Result.failure(Exception("INVALID_CREDENTIALS: El usuario o la contraseña ingresados no son correctos. Por favor, verifica tus datos."))
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
                    val offlineResult = dbHelper.verifyCredentialOffline(dni, password)
                    if (offlineResult.isSuccess) {
                        val (token, userId) = offlineResult.getOrThrow()
                        SessionManager.saveSession(token, userId)
                        Result.success(true)
                    } else {
                        if (dbHelper.getSavedCredentialForDni(dni) == null) {
                            Result.failure(Exception("NO_INTERNET_FIRST_LOGIN: No fue posible conectar con el servidor. Comprueba tu conexión a internet para iniciar sesión."))
                        } else {
                            Result.failure(Exception("INVALID_CREDENTIALS: La contraseña ingresada es incorrecta o no fue posible validar su cuenta sin conexión."))
                        }
                    }
                }
            } else {
                Log.d(TAG, "Device offline. Verifying credentials against local DB.")
                val offlineResult = dbHelper.verifyCredentialOffline(dni, password)
                if (offlineResult.isSuccess) {
                    val (token, userId) = offlineResult.getOrThrow()
                    SessionManager.saveSession(token, userId)
                    Result.success(true)
                } else {
                    if (dbHelper.getSavedCredentialForDni(dni) == null) {
                        Result.failure(Exception("NO_INTERNET_FIRST_LOGIN: No tienes conexión a internet. Para iniciar sesión por primera vez en este dispositivo necesitas conexión a la red."))
                    } else {
                        Result.failure(Exception("INVALID_CREDENTIALS: La contraseña ingresada es incorrecta."))
                    }
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
