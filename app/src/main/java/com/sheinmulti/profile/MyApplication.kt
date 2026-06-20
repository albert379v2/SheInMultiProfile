package com.sheinmulti.profile

import android.app.Application
import android.os.Build
import android.webkit.WebView
import android.util.Log

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"

        // Bandera para evitar inicializar WebView en el proceso principal
        // Solo inicializar en proceso :webview
        fun isWebViewProcess(processName: String?): Boolean {
            return processName?.endsWith(":webview") == true
        }
    }

    override fun onCreate() {
        super.onCreate()

        val processName = getProcessName()
        Log.d(TAG, "Process started: $processName")

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // WebView data directory suffix se configura en WebViewActivity.onCreate()
        // ANTES de instanciar cualquier WebView, ya que cada perfil tiene su propio suffix
        // y el proceso :webview se reinicia para cada perfil
    }

    private fun getProcessName(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            try {
                val pid = android.os.Process.myPid()
                val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
                manager.runningAppProcesses?.find { it.pid == pid }?.processName
            } catch (e: Exception) {
                null
            }
        }
    }
}
