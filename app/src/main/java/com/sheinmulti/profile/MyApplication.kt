package com.sheinmulti.profile

import android.app.Application
import android.webkit.WebView
import android.util.Log

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Application started")

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
