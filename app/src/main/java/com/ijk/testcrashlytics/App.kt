package com.ijk.testcrashlytics

import android.app.Application
import com.google.firebase.BuildConfig

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        BuildConfig.DEBUG // Ensure BuildConfig is initialized
//        CustomCrashHandler.setup()
//        throw Exception("Test2 Crashlytics Exception")
    }
}