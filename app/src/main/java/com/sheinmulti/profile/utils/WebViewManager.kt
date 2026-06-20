package com.sheinmulti.profile.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import com.sheinmulti.profile.data.model.BrowserProfile
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI

class WebViewManager(private val context: Context) {
    private val activeWebViews = mutableMapOf<Long, WebView>()

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebViewForProfile(profile: BrowserProfile): WebView {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(profile.dataDirectorySuffix)
        }

        val webView = WebView(context)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        if (profile.userAgent.isNotBlank()) {
            settings.userAgentString = profile.userAgent
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    android.util.Log.d("WebViewConsole", "${it.message()} -- Line ${it.lineNumber()}")
                }
                return true
            }
        }

        profile.proxyHost?.let { host ->
            profile.proxyPort?.let { port ->
                configureProxy(host, port, profile.proxyType)
            }
        }

        activeWebViews[profile.id] = webView
        return webView
    }

    private fun configureProxy(host: String, port: Int, type: String) {
        try {
            val proxyType = when (type.uppercase()) {
                "SOCKS4", "SOCKS5" -> Proxy.Type.SOCKS
                else -> Proxy.Type.HTTP
            }
            val proxyAddress = InetSocketAddress(host, port)
            val proxy = Proxy(proxyType, proxyAddress)
            ProxySelector.setDefault(object : ProxySelector() {
                override fun select(uri: URI?): List<Proxy> = listOf(proxy)
                override fun connectFailed(uri: URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getWebView(profileId: Long): WebView? = activeWebViews[profileId]

    fun destroyWebView(profileId: Long) {
        activeWebViews[profileId]?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
        activeWebViews.remove(profileId)
    }

    fun destroyAll() {
        activeWebViews.keys.toList().forEach { destroyWebView(it) }
    }
}
