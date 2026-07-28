package com.colma.colmaApp.utils.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.colma.colmaApp.R

class GoalDeadlineReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GoalDeadlineReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val metaId = intent.getLongExtra("meta_id", -1L)
        val actividad = intent.getStringExtra("actividad") ?: "Actividad de meta"
        val tipoActividad = intent.getStringExtra("tipo_actividad") ?: ""
        val semana = intent.getIntExtra("semana", -1)

        Log.d(TAG, "Received goal notification alarm for goal $metaId: $actividad (Semana $semana)")

        if (metaId == -1L) {
            return
        }

        // Create launch intent
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            metaId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("¡Tu meta está por vencer!")
            .setContentText("Recuerda completar tu meta: $actividad")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Recuerda completar tu meta de la semana $semana: \"$actividad\""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(metaId.toInt(), notificationBuilder.build())
            Log.d(TAG, "Notification posted successfully for goal $metaId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: cannot post notification for goal $metaId. Permission POST_NOTIFICATIONS may be missing.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification for goal $metaId", e)
        }
    }
}
