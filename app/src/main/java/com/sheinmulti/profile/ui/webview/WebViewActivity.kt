package com.sheinmulti.profile.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sheinmulti.profile.R
import com.sheinmulti.profile.data.local.ProfileCookieManager
import com.sheinmulti.profile.databinding.ActivityWebviewBinding
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.Credentials
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class WebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebviewBinding
    private var profileId: Long = -1
    private lateinit var profileName: String
    private lateinit var userAgent: String
    private lateinit var startUrl: String
    private lateinit var cookieManager: ProfileCookieManager

    // Proxy config
    private data class ProxyConfig(
        val host: String,
        val port: Int,
        val username: String?,
        val password: String?,
        val type: String
    )
    private var proxyConfig: ProxyConfig? = null
    private var okHttpClient: OkHttpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profileId = intent.getLongExtra("PROFILE_ID", -1)
        profileName = intent.getStringExtra("PROFILE_NAME") ?: "Navegador"
        userAgent = intent.getStringExtra("USER_AGENT") ?: ""
        startUrl = intent.getStringExtra("START_URL") ?: "https://www.google.com"

        cookieManager = ProfileCookieManager(this)

        // Cargar configuración de proxy
        val proxyHost = intent.getStringExtra("PROXY_HOST")
        val proxyPort = intent.getIntExtra("PROXY_PORT", -1)
        val proxyUsername = intent.getStringExtra("PROXY_USERNAME")
        val proxyPassword = intent.getStringExtra("PROXY_PASSWORD")
        val proxyType = intent.getStringExtra("PROXY_TYPE") ?: "NONE"

        if (proxyType != "NONE" && !proxyHost.isNullOrBlank() && proxyPort > 0) {
            proxyConfig = ProxyConfig(
                host = proxyHost, port = proxyPort,
                username = proxyUsername, password = proxyPassword, type = proxyType
            )
            setupProxyClient()
        }

        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupWebView()

        // Cargar cookies del perfil ANTES de cargar la URL
        lifecycleScope.launch {
            cookieManager.loadProfileCookies(profileId)
            loadStartUrl()
        }
    }

    private fun setupProxyClient() {
        val config = proxyConfig ?: return
        try {
            val builder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)

            when (config.type) {
                "HTTP" -> {
                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(config.host, config.port))
                    builder.proxy(proxy)
                    val username = config.username
                    val password = config.password
                    if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                        builder.proxyAuthenticator { _, response ->
                            val credential = Credentials.basic(username, password)
                            response.request.newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build()
                        }
                    }
                }
                "SOCKS4", "SOCKS5" -> {
                    val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(config.host, config.port))
                    builder.proxy(proxy)
                }
            }
            okHttpClient = builder.build()
            Toast.makeText(this, "Proxy ${config.type}: ${config.host}:${config.port}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error proxy: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = profileName
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
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

        if (userAgent.isNotBlank()) settings.userAgentString = userAgent

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                if (okHttpClient == null) return null
                if (!url.startsWith("http://") && !url.startsWith("https://")) return null
                return try {
                    interceptRequest(url, request)
                } catch (e: Exception) {
                    null
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
                supportActionBar?.subtitle = url
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@WebViewActivity, "Error cargando pagina", Toast.LENGTH_SHORT).show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
            }
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { android.util.Log.d("WebView", "${it.message()}") }
                return true
            }
        }
    }

    private fun interceptRequest(url: String, request: WebResourceRequest): WebResourceResponse? {
        val client = okHttpClient ?: return null
        val okRequest = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent.takeIf { it.isNotBlank() } ?: request.requestHeaders["User-Agent"] ?: "")
            .apply {
                request.requestHeaders.forEach { (key, value) ->
                    if (key.equals("User-Agent", ignoreCase = true)) return@forEach
                    header(key, value)
                }
            }
            .build()

        val response = client.newCall(okRequest).execute()
        if (!response.isSuccessful) { response.close(); return null }

        val contentType = response.header("Content-Type") ?: "text/html"
        val mimeType = contentType.split(";")[0].trim()
        val charset = contentType.split("charset=").getOrNull(1)?.trim() ?: "UTF-8"
        val inputStream = response.body?.byteStream() ?: return null

        return WebResourceResponse(mimeType, charset, inputStream)
    }

    private fun loadStartUrl() {
        binding.webView.loadUrl(startUrl)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_webview, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_refresh -> { binding.webView.reload(); true }
            R.id.action_clear_cookies -> { clearProfileCookies(); true }
            R.id.action_go_home -> { binding.webView.loadUrl(startUrl); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearProfileCookies() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar Cookies")
            .setMessage("¿Eliminar cookies de este perfil?")
            .setPositiveButton("Limpiar") { _, _ ->
                lifecycleScope.launch {
                    cookieManager.clearProfileCookies(profileId)
                    binding.webView.clearCache(true)
                    binding.webView.clearHistory()
                    Toast.makeText(this@WebViewActivity, "Cookies eliminadas", Toast.LENGTH_SHORT).show()
                    binding.webView.loadUrl(startUrl)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        // Guardar cookies al pausar la actividad
        lifecycleScope.launch {
            cookieManager.saveProfileCookies(profileId)
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) binding.webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        lifecycleScope.launch {
            cookieManager.saveProfileCookies(profileId)
        }
        binding.webView.stopLoading()
        binding.webView.loadUrl("about:blank")
        super.onDestroy()
    }
}
