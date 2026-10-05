package com.colma.colmaApp.utils.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import java.text.SimpleDateFormat
import java.util.Locale

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "goal_deadline_channel"
    private const val CHANNEL_NAME = "Recordatorios de Metas"

    const val WEEKLY_CHANNEL_ID = "weekly_unlock_channel"
    private const val WEEKLY_CHANNEL_NAME = "Nuevas Semanas Disponibles"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val goalChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notificaciones para metas próximas a vencer"
            }
            val weeklyChannel = NotificationChannel(WEEKLY_CHANNEL_ID, WEEKLY_CHANNEL_NAME, importance).apply {
                description = "Notificaciones para nuevas semanas habilitadas en tu tratamiento"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(goalChannel)
            notificationManager.createNotificationChannel(weeklyChannel)
            Log.d(TAG, "Notification channels created: $CHANNEL_ID, $WEEKLY_CHANNEL_ID")
        }
    }

    fun scheduleGoalNotification(context: Context, meta: RegistrosSaludModelos.MetaRecord) {
        if (meta.fecha_objetivo.isNullOrBlank()) {
            Log.w(TAG, "Cannot schedule notification: fecha_objetivo is blank for goal ${meta.id_meta}")
            return
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val deadlineDate = sdf.parse(meta.fecha_objetivo) ?: return
            val cal = java.util.Calendar.getInstance().apply {
                time = deadlineDate
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }
            val deadlineMillis = cal.timeInMillis
            val now = System.currentTimeMillis()

            // Calculate alarm time: 24 hours before the deadline
            val alarmTime = deadlineMillis - (24 * 60 * 60 * 1000L)
            val finalAlarmTime = if (alarmTime > now) {
                alarmTime
            } else if (deadlineMillis > now) {
                // If the deadline is in the future but less than 24 hours away, trigger in 10 seconds
                now + 10000L
            } else {
                // Already in the past
                Log.d(TAG, "Deadline is in the past, skipping notification scheduling for goal ${meta.id_meta}")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, GoalDeadlineReceiver::class.java).apply {
                putExtra("meta_id", meta.id_meta)
                putExtra("actividad", meta.actividad)
                putExtra("tipo_actividad", meta.tipo_actividad)
                putExtra("semana", meta.semana)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                meta.id_meta.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            finalAlarmTime,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled exact notification for goal ${meta.id_meta} at $finalAlarmTime")
                    } catch (e: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            finalAlarmTime,
                            pendingIntent
                        )
                        Log.d(TAG, "Fallback scheduled notification for goal ${meta.id_meta} at $finalAlarmTime (SecurityException)")
                    }
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        finalAlarmTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled non-exact notification for goal ${meta.id_meta} at $finalAlarmTime")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    finalAlarmTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact notification for goal ${meta.id_meta} at $finalAlarmTime (pre-S)")
            }

            val toastMsg = if (finalAlarmTime == now + 10000L) {
                "Prueba: Recordatorio programado para dentro de 10 segundos"
            } else {
                val alarmDate = java.util.Date(finalAlarmTime)
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                "Recordatorio programado para: ${format.format(alarmDate)}"
            }
            android.os.Handler(context.mainLooper).post {
                android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification for goal ${meta.id_meta}", e)
            android.os.Handler(context.mainLooper).post {
                android.widget.Toast.makeText(context, "Error al programar recordatorio: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun cancelGoalNotification(context: Context, metaId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, GoalDeadlineReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                metaId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Canceled notification for goal $metaId")
            } else {
                Log.d(TAG, "No pending notification found to cancel for goal $metaId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notification for goal $metaId", e)
        }
    }

    fun scheduleWeeklyUnlock(context: Context, weekNumber: Int, triggerTimeMillis: Long, isTestMode: Boolean = false) {
        try {
            val now = System.currentTimeMillis()
            if (triggerTimeMillis <= now) {
                Log.d(TAG, "Weekly unlock trigger time is in the past for Week $weekNumber, skipping.")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WeeklyUnlockReceiver::class.java).apply {
                putExtra(WeeklyUnlockReceiver.EXTRA_WEEK_NUMBER, weekNumber)
                putExtra(WeeklyUnlockReceiver.EXTRA_IS_TEST, isTestMode)
            }

            val requestCode = if (isTestMode) 40000 + weekNumber else 50000 + weekNumber
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                        Log.d(TAG, "Scheduled exact weekly unlock for Week $weekNumber at $triggerTimeMillis (isTest: $isTestMode)")
                    } catch (e: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                        Log.d(TAG, "Fallback scheduled weekly unlock for Week $weekNumber at $triggerTimeMillis (SecurityException)")
                    }
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    Log.d(TAG, "Scheduled non-exact weekly unlock for Week $weekNumber at $triggerTimeMillis")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                Log.d(TAG, "Scheduled exact weekly unlock for Week $weekNumber at $triggerTimeMillis (pre-S)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling weekly unlock notification for Week $weekNumber", e)
        }
    }

    fun sendWeeklyUnlockNotificationNow(context: Context, weekNumber: Int, isTestMode: Boolean = true) {
        try {
            val intent = Intent(context, WeeklyUnlockReceiver::class.java).apply {
                putExtra(WeeklyUnlockReceiver.EXTRA_WEEK_NUMBER, weekNumber)
                putExtra(WeeklyUnlockReceiver.EXTRA_IS_TEST, isTestMode)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent weekly unlock notification broadcast immediately for Week $weekNumber (isTest: $isTestMode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending weekly unlock notification broadcast for Week $weekNumber", e)
        }
    }

    fun cancelWeeklyTestUnlock(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            for (week in 1..16) {
                val intent = Intent(context, WeeklyUnlockReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    40000 + week,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    Log.d(TAG, "Canceled test weekly unlock alarm for Week $week")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling test weekly unlock alarms", e)
        }
    }

    fun scheduleAllProductionWeeks(context: Context, primerLoginDate: String?) {
        if (primerLoginDate.isNullOrBlank()) return
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        try {
            val regDate = sdf.parse(primerLoginDate) ?: return
            val now = System.currentTimeMillis()
            for (week in 2..16) {
                val cal = java.util.Calendar.getInstance().apply {
                    time = regDate
                    add(java.util.Calendar.DAY_OF_YEAR, (week - 1) * 7)
                    set(java.util.Calendar.HOUR_OF_DAY, 9)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val triggerMillis = cal.timeInMillis
                if (triggerMillis > now) {
                    scheduleWeeklyUnlock(context, week, triggerMillis, isTestMode = false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling all production weeks from date $primerLoginDate", e)
        }
    }
}
