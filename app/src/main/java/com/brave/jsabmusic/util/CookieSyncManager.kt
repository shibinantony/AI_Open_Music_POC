package com.brave.jsabmusic.util

import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ensures user sessions, phone login tokens, and JioSaavn library state
 * persist across app restarts and process lifecycles.
 */
object CookieSyncManager {

    fun setupCookies(webView: WebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
    }

    fun flushCookies() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CookieManager.getInstance().flush()
            } catch (e: Exception) {
                // Ignore transient sync failures
            }
        }
    }
}
