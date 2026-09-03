package com.brave.jsabmusic.util

import android.content.Context
import android.webkit.WebSettings

/**
 * Manages User-Agent configuration to:
 * 1. Eliminate JioSaavn's artificial mobile web listening limit ("Listen with no limits on the JioSaavn App").
 * 2. Emulate an unconstrained modern browser environment for unlimited uninterrupted streaming.
 * 3. Preserve Google/Phone authentication compatibility.
 */
object UserAgentManager {

    private const val DESKTOP_CHROME_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"

    /**
     * Returns a Desktop Chrome user agent string that deactivates JioSaavn's mobile app-wall.
     */
    fun getDesktopUserAgent(): String {
        return DESKTOP_CHROME_UA
    }

    /**
     * Applies the sanitized User-Agent to the provided WebSettings.
     */
    fun applyUserAgent(settings: WebSettings, context: Context) {
        settings.userAgentString = getDesktopUserAgent()
    }
}
