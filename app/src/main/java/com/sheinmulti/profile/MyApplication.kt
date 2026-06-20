package com.sheinmulti.profile

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.webkit.WebView
import android.util.Log

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"

        const val WEBVIEW_PROCESS_SUFFIX = ":webview"

        fun getProcessName(context: Context): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Application.getProcessName()
            } else {
                try {
                    val pid = Process.myPid()
                    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    manager.runningAppProcesses?.find { it.pid == pid }?.processName
                } catch (e: Exception) {
                    null
                }
            }
        }

        fun isWebViewProcess(context: Context): Boolean {
            return getProcessName(context)?.endsWith(WEBVIEW_PROCESS_SUFFIX) == true
        }
    }

    override fun onCreate() {
        super.onCreate()

        val processName = getProcessName(this) ?: "unknown"
        Log.d(TAG, "Process started: $processName")

        // CRÍTICO: Para Android 9+, si estamos en el proceso :webview,
        // debemos establecer el data directory suffix ANTES de que cualquier 
        // WebView sea instanciado o cualquier método de android.webkit sea llamado.
        // Esto debe hacerse aquí en Application.onCreate(), NO en Activity.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isWebViewProcess(this)) {
            try {
                // El suffix se pasará por Intent, pero usamos un default por si acaso
                val suffix = intentSuffix ?: "webview_default"
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

    // Variable estática para recibir el suffix del Intent antes de que onCreate() se ejecute
    // Esto se establece desde WebViewActivity antes de que el proceso se inicie
    companion object {
        @Volatile
        var intentSuffix: String? = null
    }
}
