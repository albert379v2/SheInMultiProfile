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
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.util.Properties

class WebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebviewBinding
    private var profileId: Long = -1
    private lateinit var profileSuffix: String
    private lateinit var userAgent: String
    private lateinit var startUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profileId = intent.getLongExtra("PROFILE_ID", -1)
        profileSuffix = intent.getStringExtra("PROFILE_SUFFIX") ?: "default"
        userAgent = intent.getStringExtra("USER_AGENT") ?: ""
        startUrl = intent.getStringExtra("START_URL") ?: "https://www.google.com"
        val proxyHost = intent.getStringExtra("PROXY_HOST")
        val proxyPort = intent.getIntExtra("PROXY_PORT", -1)
        val proxyUsername = intent.getStringExtra("PROXY_USERNAME")
        val proxyPassword = intent.getStringExtra("PROXY_PASSWORD")
        val proxyType = intent.getStringExtra("PROXY_TYPE") ?: "NONE"

        // Try to set data directory suffix per profile (may fail if WebView already initialized)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WebView.setDataDirectorySuffix(profileSuffix)
            } catch (e: IllegalStateException) {
                android.util.Log.w("WebViewActivity", "WebView already initialized, cannot change suffix: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e("WebViewActivity", "Error setting data directory suffix: ${e.message}")
            }
        }

        applyProxy(proxyHost, proxyPort, proxyUsername, proxyPassword, proxyType)

        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupWebView()
        loadStartUrl()
    }

    private fun applyProxy(
        proxyHost: String?,
        proxyPort: Int,
        proxyUsername: String?,
        proxyPassword: String?,
        proxyType: String
    ) {
        if (proxyType == "NONE" || proxyHost.isNullOrBlank() || proxyPort <= 0) return

        try {
            val props = Properties(System.getProperties())
            when (proxyType) {
                "HTTP" -> {
                    props.setProperty("http.proxyHost", proxyHost)
                    props.setProperty("http.proxyPort", proxyPort.toString())
                    props.setProperty("https.proxyHost", proxyHost)
                    props.setProperty("https.proxyPort", proxyPort.toString())
                }
                "SOCKS4", "SOCKS5" -> {
                    props.setProperty("socksProxyHost", proxyHost)
                    props.setProperty("socksProxyPort", proxyPort.toString())
                }
            }
            System.setProperties(props)

            if (!proxyUsername.isNullOrBlank() && !proxyPassword.isNullOrBlank()) {
                Authenticator.setDefault(object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication? {
                        return PasswordAuthentication(proxyUsername, proxyPassword.toCharArray())
                    }
                })
            }

<<<<<<< HEAD
=======
            // For Android 10+, proxy is configured via system properties above
            // WebView will automatically use the system proxy settings
>>>>>>> b20682170b5c89205aa2e8dc496c5dc5a2ad74db
            android.util.Log.d("WebViewActivity", "Proxy configured: $proxyType://$proxyHost:$proxyPort")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error aplicando proxy: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra("PROFILE_NAME") ?: "Navegador"
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
            R.id.action_clear_cookies -> { clearCookies(); true }
            R.id.action_go_home -> { binding.webView.loadUrl(startUrl); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearCookies() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar Cookies")
            .setMessage("¿Eliminar todas las cookies y datos de sesion de este perfil?")
            .setPositiveButton("Limpiar") { _, _ ->
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                binding.webView.clearCache(true)
                binding.webView.clearHistory()
                Toast.makeText(this, "Cookies eliminadas", Toast.LENGTH_SHORT).show()
                binding.webView.loadUrl(startUrl)
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
