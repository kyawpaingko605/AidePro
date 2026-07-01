package com.aidepro.app

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AideProApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize build tools from assets
        initBuildTools()
    }

    private fun initBuildTools() {
        // Extract native binaries (aapt2, etc.) from assets to internal storage on first run
        val toolsDir = getDir("tools", Context.MODE_PRIVATE)
        if (!toolsDir.exists()) {
            toolsDir.mkdirs()
        }
        // Actual extraction is handled by BuildToolsManager in core:buildsystem module
        Timber.d("Build tools directory: ${toolsDir.absolutePath}")
    }

    companion object {
        lateinit var instance: AideProApplication
            private set

        fun getAppContext(): Context = instance.applicationContext
    }
}
