package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class DirectoryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            FirebaseApp.initializeApp(this)
            Log.d("DirectoryApp", "FirebaseApp initialized successfully")
            com.example.data.remote.FirebaseAuthManager.init()
        } catch (e: Exception) {
            Log.e("DirectoryApp", "Firebase initialization failed: ${e.message}", e)
        }
    }

    companion object {
        lateinit var instance: DirectoryApplication
            private set
    }
}
