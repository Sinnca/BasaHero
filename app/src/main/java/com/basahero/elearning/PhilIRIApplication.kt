package com.basahero.elearning

import android.app.Application
import android.util.Log
import com.basahero.elearning.util.NetworkMonitor

// ─────────────────────────────────────────────────────────────────────────────
// PhilIRIApplication
// Application class — runs before any Activity or Service.
// Register this in AndroidManifest.xml:
//   <application android:name=".PhilIRIApplication" ...>
//
// Responsibilities:
//   1. Start NetworkMonitor so sync workers fire when device goes online
//   2. Any other app-wide initialization goes here
// ─────────────────────────────────────────────────────────────────────────────
class PhilIRIApplication : Application() {

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()

        Log.d("PhilIRIApplication", "App starting...")

        // Start network monitoring — triggers sync workers on reconnect
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startWatching()

        Log.d("PhilIRIApplication", "NetworkMonitor started")
    }

    override fun onTerminate() {
        super.onTerminate()
        networkMonitor.stopWatching()
    }
}