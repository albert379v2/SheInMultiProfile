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
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        // Data directory suffix must be set BEFORE any WebView is instantiated
        // This is handled per-profile in WebViewActivity with try-catch
    }
}
