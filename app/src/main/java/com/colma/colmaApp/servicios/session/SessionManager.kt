package com.colma.colmaApp.servicios.session

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF_NAME = "ColmaAppSession"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"

    private var preferences: SharedPreferences? = null

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(token: String, userId: Int) {
        val editor = preferences?.edit()
        editor?.putString(KEY_AUTH_TOKEN, token)
        editor?.putInt(KEY_USER_ID, userId)
        editor?.apply()
    }

    fun getAuthToken(): String? {
        return preferences?.getString(KEY_AUTH_TOKEN, null)
    }

    fun getUserId(): Int? {
        val userId = preferences?.getInt(KEY_USER_ID, -1)
        return if (userId == -1) null else userId
    }

    fun clearSession() {
        val editor = preferences?.edit()
        editor?.remove(KEY_AUTH_TOKEN)
        editor?.remove(KEY_USER_ID)
        editor?.apply()
    }
}
