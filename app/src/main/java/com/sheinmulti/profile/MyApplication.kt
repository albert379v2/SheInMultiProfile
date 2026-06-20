package com.sheinmulti.profile

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.webkit.WebView
import android.util.Log

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"
        private const val PREFS_NAME = "webview_profile_prefs"
        private const val KEY_SUFFIX = "current_suffix"
        const val WEBVIEW_PROCESS_SUFFIX = ":webview"

        fun getProcessName(context: Context): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Application.getProcessName()
            } else {
                try {
                    val pid = Process.myPid()
                    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    manager.runningAppProcesses?.find { it.pid == pid }?.processName
                } catch (e: Exception) {
                    null
                }
            }
        }

        fun isWebViewProcess(context: Context): Boolean {
            return getProcessName(context)?.endsWith(WEBVIEW_PROCESS_SUFFIX) == true
        }

        /**
         * Guarda el suffix del perfil en SharedPreferences para que el proceso :webview lo lea
         */
        fun saveProfileSuffix(context: Context, suffix: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SUFFIX, suffix)
                .apply()
        }

        /**
         * Lee el suffix del perfil desde SharedPreferences
         */
        fun getProfileSuffix(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SUFFIX, null)
        }

        /**
         * Limpia el suffix guardado
         */
        fun clearProfileSuffix(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_SUFFIX)
                .apply()
        }
    }

    override fun onCreate() {
        super.onCreate()

        val processName = getProcessName(this) ?: "unknown"
        Log.d(TAG, "Process started: $processName")

        // CRÍTICO: Para Android 9+, establecer el suffix ANTES de instanciar WebView
        // Leemos el suffix desde SharedPreferences que fue guardado por MainActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isWebViewProcess(this)) {
            try {
                val suffix = getProfileSuffix(this) ?: "webview_default"
                WebView.setDataDirectorySuffix(suffix)
                Log.d(TAG, "WebView data directory suffix set: $suffix")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting WebView data directory suffix: ${e.message}")
            }
        }

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
