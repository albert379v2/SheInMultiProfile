package com.sheinmulti.profile.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sheinmulti.profile.R
import com.sheinmulti.profile.databinding.ActivityWebviewBinding
import okhttp3.*
import okhttp3.Credentials
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class WebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebviewBinding
    private var profileId: Long = -1
    private lateinit var profileSuffix: String
    private lateinit var profileName: String
    private lateinit var userAgent: String
    private lateinit var startUrl: String

    // Proxy config
    private var proxyHost: String? = null
    private var proxyPort: Int = -1
    private var proxyUsername: String? = null
    private var proxyPassword: String? = null
    private var proxyType: String = "NONE"

    // OkHttp client para proxy
    private var okHttpClient: OkHttpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profileId = intent.getLongExtra("PROFILE_ID", -1)
        profileSuffix = intent.getStringExtra("PROFILE_SUFFIX") ?: "default"
        profileName = intent.getStringExtra("PROFILE_NAME") ?: "Navegador"
        userAgent = intent.getStringExtra("USER_AGENT") ?: ""
        startUrl = intent.getStringExtra("START_URL") ?: "https://www.google.com"
        proxyHost = intent.getStringExtra("PROXY_HOST")
        proxyPort = intent.getIntExtra("PROXY_PORT", -1)
        proxyUsername = intent.getStringExtra("PROXY_USERNAME")
        proxyPassword = intent.getStringExtra("PROXY_PASSWORD")
        proxyType = intent.getStringExtra("PROXY_TYPE") ?: "NONE"

        // CRÍTICO: Establecer data directory suffix ANTES de cualquier WebView
        // Esto funciona porque WebViewActivity corre en proceso :webview separado
        // y cada vez que se abre un perfil, el proceso se reinicia fresco
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WebView.setDataDirectorySuffix(profileSuffix)
                android.util.Log.d("WebViewActivity", "Data directory suffix set: $profileSuffix")
            } catch (e: IllegalStateException) {
                android.util.Log.w("WebViewActivity", "WebView already initialized, cannot change suffix: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e("WebViewActivity", "Error setting data directory suffix: ${e.message}")
            }
        }

        // Inicializar OkHttp client con proxy si está configurado
        setupProxyClient()

        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupWebView()
        loadStartUrl()
    }

    /**
     * Configura el cliente OkHttp con proxy para interceptar requests del WebView
     */
    private fun setupProxyClient() {
        if (proxyType == "NONE" || proxyHost.isNullOrBlank() || proxyPort <= 0) {
            android.util.Log.d("WebViewActivity", "No proxy configured")
            return
        }

        try {
            val builder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)

            when (proxyType) {
                "HTTP" -> {
                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
                    builder.proxy(proxy)

                    // Autenticación proxy
                    if (!proxyUsername.isNullOrBlank() && !proxyPassword.isNullOrBlank()) {
                        builder.proxyAuthenticator { _, response ->
                            val credential = Credentials.basic(proxyUsername, proxyPassword)
                            response.request.newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build()
                        }
                    }
                    android.util.Log.d("WebViewActivity", "HTTP Proxy configured: $proxyHost:$proxyPort")
                }
                "SOCKS4", "SOCKS5" -> {
                    // Para SOCKS, usamos un proxy SOCKS directo
                    val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxyHost, proxyPort))
                    builder.proxy(proxy)
                    android.util.Log.d("WebViewActivity", "$proxyType Proxy configured: $proxyHost:$proxyPort")
                    // Nota: SOCKS con autenticación requiere configuración adicional del sistema
                }
            }

            okHttpClient = builder.build()
            Toast.makeText(this, "Proxy $proxyType activado: $proxyHost:$proxyPort", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error configurando proxy: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // Cookies: aceptar cookies para este perfil (aislado por proceso)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            /**
             * Intercepta requests y los reenvía a través del proxy OkHttp
             */
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null

                // Si no hay proxy configurado, dejar que WebView maneje el request normalmente
                if (okHttpClient == null) return null

                // Solo interceptar requests HTTP/HTTPS
                if (!url.startsWith("http://") && !url.startsWith("https://")) return null

                return try {
                    interceptRequest(url, request)
                } catch (e: Exception) {
                    android.util.Log.e("WebViewActivity", "Error intercepting request: ${e.message}")
                    null // Fallback a WebView nativo
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
                consoleMessage?.let { android.util.Log.d("WebView", "${it.message()} - Line ${it.lineNumber()}") }
                return true
            }
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                val newWebView = WebView(this@WebViewActivity)
                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            Toast.makeText(this, "Descarga iniciada", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Intercepta un request del WebView y lo reenvía a través del proxy OkHttp
     */
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

        if (!response.isSuccessful) {
            response.close()
            return null
        }

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

    /**
     * Limpia SOLO las cookies y datos de ESTE perfil
     * Gracias al proceso separado (:webview) y data directory suffix,
     * esto solo afecta al perfil actual
     */
    private fun clearProfileCookies() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar Cookies")
            .setMessage("¿Eliminar todas las cookies y datos de sesion de este perfil?")
            .setPositiveButton("Limpiar") { _, _ ->
                // Como cada perfil corre en proceso :webview con su propio data directory suffix,
                // removeAllCookies() solo afecta las cookies de ESTE perfil
                CookieManager.getInstance().removeAllCookies { _ ->
                    CookieManager.getInstance().flush()
                    WebStorage.getInstance().deleteAllData()
                    binding.webView.clearCache(true)
                    binding.webView.clearHistory()
                    runOnUiThread {
                        Toast.makeText(this, "Cookies del perfil eliminadas", Toast.LENGTH_SHORT).show()
                        binding.webView.loadUrl(startUrl)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) binding.webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        binding.webView.stopLoading()
        binding.webView.loadUrl("about:blank")
        super.onDestroy()
    }
}
