package com.colma.colmaApp.utils.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.main.ui.MainActivity

class WeeklyUnlockReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "WeeklyUnlockReceiver"
        const val EXTRA_WEEK_NUMBER = "extra_week_number"
        const val EXTRA_IS_TEST = "extra_is_test"
        private const val PREFS_NAME = "session_testing_prefs"
        private const val KEY_TEST_UNLOCKED_WEEKS = "testing_unlocked_weeks"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val weekNumber = intent.getIntExtra(EXTRA_WEEK_NUMBER, 1)
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        Log.d(TAG, "Received weekly unlock notification broadcast for Week $weekNumber (isTest: $isTest)")

        if (isTest) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentUnlocked = prefs.getInt(KEY_TEST_UNLOCKED_WEEKS, 1)
            if (weekNumber > currentUnlocked) {
                prefs.edit().putInt(KEY_TEST_UNLOCKED_WEEKS, weekNumber).apply()
                Log.d(TAG, "Updated testing unlocked weeks in SharedPreferences to $weekNumber")
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationHelper.createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            weekNumber + 20000,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "¡Semana $weekNumber disponible!"
        val message = "Ya puedes acceder a los nuevos contenidos y actividades de la Semana $weekNumber de tu tratamiento."

        val notificationBuilder = NotificationCompat.Builder(context, NotificationHelper.WEEKLY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationId = 30000 + weekNumber
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "Weekly unlock notification posted successfully for Week $weekNumber (ID: $notificationId)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting weekly unlock notification for Week $weekNumber", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting weekly unlock notification for Week $weekNumber", e)
        }
    }
}
