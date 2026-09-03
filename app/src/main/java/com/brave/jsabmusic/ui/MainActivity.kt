package com.brave.jsabmusic.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.brave.jsabmusic.adblock.AdBlockEngine
import com.brave.jsabmusic.bridge.WebInterfaceBridge
import com.brave.jsabmusic.equalizer.EqualizerManager
import com.brave.jsabmusic.service.PlaybackService
import com.brave.jsabmusic.timer.SleepTimerManager
import com.brave.jsabmusic.ui.components.EqualizerSheet
import com.brave.jsabmusic.ui.components.SleepTimerSheet
import com.brave.jsabmusic.ui.theme.AmoledBlack
import com.brave.jsabmusic.ui.theme.JSABMusicTheme
import com.brave.jsabmusic.ui.theme.SaavnTeal
import com.brave.jsabmusic.util.CookieSyncManager
import com.brave.jsabmusic.util.UserAgentManager

class MainActivity : ComponentActivity() {

    private var activeWebView: BackgroundWebView? = null
    private var webBridge: WebInterfaceBridge? = null
    private var playbackService: PlaybackService? = null
    private var isServiceBound = false

    private lateinit var adBlockEngine: AdBlockEngine
    private lateinit var equalizerManager: EqualizerManager
    private lateinit var sleepTimerManager: SleepTimerManager

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlaybackService.LocalBinder
            playbackService = binder.getService()
            isServiceBound = true
            webBridge?.let { playbackService?.setBridge(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Ensure soft keyboard resizes the viewport cleanly for phone/OTP login
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        adBlockEngine = AdBlockEngine(applicationContext)
        equalizerManager = EqualizerManager(applicationContext)
        sleepTimerManager = SleepTimerManager(applicationContext)

        checkNotificationPermission()
        startAndBindPlaybackService()

        setContent {
            JSABMusicTheme {
                MainScreen()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startAndBindPlaybackService() {
        val serviceIntent = Intent(this, PlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @Composable
    private fun MainScreen() {
        var showEqualizer by remember { mutableStateOf(false) }
        var showSleepTimer by remember { mutableStateOf(false) }
        val isTimerRunning by sleepTimerManager.isTimerRunning.collectAsState()

        BackHandler(enabled = activeWebView?.canGoBack() == true) {
            activeWebView?.goBack()
        }

        Scaffold(
            containerColor = AmoledBlack,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Hardened WebView Layer
                AndroidView(
                    factory = { ctx ->
                        createConfiguredWebView(ctx).also {
                            activeWebView = it
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Action Controls (Equalizer & Sleep Timer)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 80.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Equalizer FAB
                    FloatingActionButton(
                        onClick = { showEqualizer = true },
                        containerColor = SaavnTeal,
                        contentColor = AmoledBlack,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Studio Equalizer"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sleep Timer FAB
                    FloatingActionButton(
                        onClick = { showSleepTimer = true },
                        containerColor = if (isTimerRunning) SaavnTeal else AmoledBlack,
                        contentColor = if (isTimerRunning) AmoledBlack else SaavnTeal,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer"
                        )
                    }
                }
            }
        }

        if (showEqualizer) {
            EqualizerSheet(
                equalizerManager = equalizerManager,
                onDismissRequest = { showEqualizer = false }
            )
        }

        if (showSleepTimer) {
            SleepTimerSheet(
                timerManager = sleepTimerManager,
                onDismissRequest = { showSleepTimer = false }
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                CookieSyncManager.flushCookies()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(context: Context): BackgroundWebView {
        return BackgroundWebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true

            val bridge = WebInterfaceBridge(this)
            webBridge = bridge
            playbackService?.setBridge(bridge)
            equalizerManager.setBridge(bridge)
            sleepTimerManager.setBridge(bridge)

            // Setup Cookies & Auth Persistence
            CookieSyncManager.setupCookies(this)

            // Configure Hardened WebSettings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                safeBrowsingEnabled = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                displayZoomControls = false
                builtInZoomControls = false
                setSupportZoom(false)
                offscreenPreRaster = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)

                // Mobile Chrome Samsung S24 FE profile
                UserAgentManager.applyUserAgent(this, context)
            }

            // Register Native JavaScript Bridge
            addJavascriptInterface(bridge, "AndroidBridge")

            // Read shield script
            val scriptContent = try {
                context.assets.open("inject.js").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                ""
            }

            // Pre-DOM Document-Start Injection via WebViewCompat
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) && scriptContent.isNotEmpty()) {
                WebViewCompat.addDocumentStartJavaScript(this, scriptContent, setOf("*"))
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url ?: return null
                    if (adBlockEngine.shouldBlock(url)) {
                        return adBlockEngine.createEmptyResponse()
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (scriptContent.isNotEmpty()) {
                        bridge.injectShieldScript(scriptContent)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (scriptContent.isNotEmpty()) {
                        bridge.injectShieldScript(scriptContent)
                    }
                    CookieSyncManager.flushCookies()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    // Keep browsing within JioSaavn domains
                    if (url.contains("jiosaavn.com") || url.contains("saavn.com") || url.contains("jio.com") || url.contains("google.com")) {
                        return false
                    }
                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {}

            loadUrl("https://www.jiosaavn.com/")
        }
    }

    override fun onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        activeWebView?.destroy()
        super.onDestroy()
    }
}
