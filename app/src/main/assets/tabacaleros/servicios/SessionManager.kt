package com.codeco.tabacaleros.servicios

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codeco.tabacaleros.features.DNI.model.ProductorInfo
import com.codeco.tabacaleros.servicios.internetCommunication.model.Component
import com.codeco.tabacaleros.servicios.internetCommunication.model.Radicacion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// DataStore delegate
private val Context.dataStore by preferencesDataStore("session_prefs")

class SessionManager(private val context: Context) {
    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_ID_CONTACTO = stringPreferencesKey("user_id_contacto")
        private val KEY_PRODUCER_INFO = stringPreferencesKey("producer_info")
        private val KEY_USER_COMPONENTS = stringPreferencesKey("user_components")
        private val KEY_SELECTED_RADICACION = stringPreferencesKey("selected_radicacion")
    }

    private val ds = context.dataStore
    private val gson = Gson()

    val tokenFlow: Flow<String?> = ds.data.map { prefs -> prefs[KEY_AUTH_TOKEN] }
        .distinctUntilChanged()

    val emailFlow: Flow<String?> = ds.data.map { prefs -> prefs[KEY_USER_EMAIL] }
        .distinctUntilChanged()

    val userIdFlow: Flow<Int?> = ds.data.map { prefs -> prefs[KEY_USER_ID]?.toIntOrNull() }
        .distinctUntilChanged()

    val userIdContactoFlow: Flow<Int?> = ds.data.map { prefs -> prefs[KEY_USER_ID_CONTACTO]?.toIntOrNull() }
        .distinctUntilChanged()

    val producerInfoFlow: Flow<ProductorInfo?> = ds.data.map { prefs ->
        prefs[KEY_PRODUCER_INFO]?.let {
            gson.fromJson(it, ProductorInfo::class.java)
        }
    }.distinctUntilChanged()

    val componentsFlow: Flow<List<Component>?> = ds.data.map { prefs ->
        prefs[KEY_USER_COMPONENTS]?.let {
            val type = object : TypeToken<List<Component>>() {}.type
            gson.fromJson<List<Component>>(it, type)
        }
    }.distinctUntilChanged()

    val selectedRadicacionFlow: Flow<Radicacion?> = ds.data.map { prefs ->
        prefs[KEY_SELECTED_RADICACION]?.let {
            gson.fromJson(it, Radicacion::class.java)
        }
    }.distinctUntilChanged()

    val isLoggedInFlow: Flow<Boolean> = tokenFlow.map { !it.isNullOrBlank() }
        .distinctUntilChanged()

    suspend fun saveSession(token: String, email: String, userId: Int, userIdContacto: Int, componentes: List<Component>) {
        ds.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_ID] = userId.toString() // DataStore prefers String for complex types or if you want to avoid type issues
            prefs[KEY_USER_ID_CONTACTO] = userIdContacto.toString()
            prefs[KEY_USER_COMPONENTS] = gson.toJson(componentes)
        }
    }

    suspend fun saveProducerInfo(productorInfo: ProductorInfo) {
        ds.edit { prefs ->
            val json = gson.toJson(productorInfo)
            prefs[KEY_PRODUCER_INFO] = json
        }
    }

    suspend fun saveSelectedRadicacion(radicacion: Radicacion) {
        ds.edit { prefs ->
            val json = gson.toJson(radicacion)
            prefs[KEY_SELECTED_RADICACION] = json
        }
    }

    suspend fun clearSession() {
        ds.edit { prefs -> prefs.clear() }
    }

    suspend fun clearProducerInfo() {
        ds.edit { prefs ->
            prefs.remove(KEY_PRODUCER_INFO)
        }
    }

    suspend fun clearSelectedRadicacion() {
        ds.edit { prefs ->
            prefs.remove(KEY_SELECTED_RADICACION)
        }
    }
}