package com.colma.colmaApp.servicios.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object SessionManager {

    private const val TAG = "SessionManager"
    private const val PREF_NAME = "ColmaAppSession"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_LAST_APP_VERSION = "last_app_version"

    private var preferences: SharedPreferences? = null

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        checkAppUpdate(context)
    }

    private fun checkAppUpdate(context: Context) {
        val lastVersion = preferences?.getLong(KEY_LAST_APP_VERSION, -1) ?: -1
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            if (lastVersion != -1L && currentVersionCode != lastVersion) {
                Log.i(TAG, "App version changed from $lastVersion to $currentVersionCode. Performing complete data reset...")
                resetAllAppData(context)
            } else if (lastVersion == -1L) {
                if (getAuthToken() != null) {
                    Log.i(TAG, "Existing session detected with no version recorded. Performing complete data reset...")
                    resetAllAppData(context)
                }
            }

            preferences?.edit()?.putLong(KEY_LAST_APP_VERSION, currentVersionCode)?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAppUpdate: ${e.message}", e)
        }
    }

    fun resetAllAppData(context: Context) {
        try {
            // 1. Clear session preferences
            clearSession()

            // 2. Clear testing preferences
            context.getSharedPreferences("session_testing_prefs", Context.MODE_PRIVATE).edit().clear().apply()

            // 3. Delete SQLite database
            context.deleteDatabase("colmadb.db")

            // 4. Delete downloaded treatment files and multimedia in filesDir
            context.filesDir?.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }

            // 5. Delete cacheDir files
            context.cacheDir?.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }

            // 6. Cancel any testing notifications/alarms
            try {
                com.colma.colmaApp.utils.notifications.NotificationHelper.cancelWeeklyTestUnlock(context)
            } catch (ignored: Throwable) {}

            Log.i(TAG, "All application data, database and downloaded files successfully reset.")
        } catch (e: Exception) {
            Log.e(TAG, "Error while resetting application data: ${e.message}", e)
        }
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

