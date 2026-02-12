package com.colma.colmaApp

import android.app.Application
import com.colma.colmaApp.servicios.session.SessionManager

class ColmaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
