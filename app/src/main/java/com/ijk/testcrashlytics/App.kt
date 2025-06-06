package com.ijk.testcrashlytics

import android.app.Application

class App: Application() {

    override fun onCreate() {
        super.onCreate()
//        CustomCrashHandler.setup()
//        throw Exception("Test2 Crashlytics Exception")
    }
}