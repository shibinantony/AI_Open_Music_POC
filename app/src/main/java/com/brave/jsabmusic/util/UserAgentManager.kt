package com.brave.jsabmusic.util

import android.content.Context
import android.webkit.WebSettings

/**
 * Manages User-Agent configuration to:
 * 1. Eliminate mobile app-wall install gates without incurring desktop memory bloat.
 * 2. Emulate a modern Samsung Galaxy Tablet environment (SM-X910, Android 14, Chrome 134).
 * 3. Provide smooth, lightweight continuous streaming with full touch navigation.
 */
object UserAgentManager {

    private const val TABLET_CHROME_UA =
        "Mozilla/5.0 (Linux; Android 14; SM-X910) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.6998.35 Safari/537.36"

    /**
     * Returns an optimized tablet Chrome user agent string.
     */
    fun getOptimizedUserAgent(): String {
        return TABLET_CHROME_UA
    }

    /**
     * Applies the sanitized User-Agent to the provided WebSettings.
     */
    fun applyUserAgent(settings: WebSettings, context: Context) {
        settings.userAgentString = getOptimizedUserAgent()
    }
}
