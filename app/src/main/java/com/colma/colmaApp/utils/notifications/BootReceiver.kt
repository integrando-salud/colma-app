package com.colma.colmaApp.utils.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, rescheduling pending goal notifications")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dbHelper = DbHelper(context.applicationContext)
                    val activeGoals = dbHelper.getUncompletedFutureMetas()
                    Log.d(TAG, "Found ${activeGoals.size} active goals to reschedule")
                    for (goal in activeGoals) {
                        NotificationHelper.scheduleGoalNotification(context.applicationContext, goal)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling goals on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
