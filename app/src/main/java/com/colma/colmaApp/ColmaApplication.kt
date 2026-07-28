package com.colma.colmaApp

import android.app.Application
import com.colma.colmaApp.servicios.session.SessionManager
import com.colma.colmaApp.utils.notifications.NotificationHelper

class ColmaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        NotificationHelper.createNotificationChannel(this)
    }
}
