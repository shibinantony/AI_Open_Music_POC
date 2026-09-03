package com.brave.jsabmusic.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory Brave-grade request interceptor and domain filter list evaluator for JioSaavn.
 * Intercepts and drops ad network calls, audio-ad media, and telemetry beacons.
 */
class AdBlockEngine(private val context: Context) {

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val blockedUrlPatterns = mutableListOf<String>()

    @Volatile
    private var isInitialized = false

    init {
        loadFilterList()
    }

    private fun loadFilterList() {
        try {
            context.assets.open("adblock_filter.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            if (trimmed.contains("/")) {
                                blockedUrlPatterns.add(trimmed.lowercase())
                            } else {
                                blockedDomains.add(trimmed.lowercase())
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
            isInitialized = true
        } catch (e: Exception) {
            blockedDomains.addAll(
                setOf(
                    "jioads.com",
                    "adservice.jio.com",
                    "ads.jio.com",
                    "googleads.g.doubleclick.net",
                    "pagead2.googlesyndication.com",
                    "inmobi.com",
                    "taboola.com"
                )
            )
            isInitialized = true
        }
    }

    fun shouldBlock(uri: Uri): Boolean {
        if (!isInitialized) return false

        val host = uri.host?.lowercase() ?: return false
        val fullUrl = uri.toString().lowercase()

        // Critical Whitelist: Essential JioSaavn media streaming and CDN servers
        if (isWhitelisted(host)) {
            // Drop ad queries even on whitelisted hosts
            for (pattern in blockedUrlPatterns) {
                if (fullUrl.contains(pattern)) {
                    return true
                }
            }
            return false
        }

        // Direct domain match
        if (blockedDomains.contains(host)) {
            return true
        }

        // Subdomain matching
        for (blocked in blockedDomains) {
            if (host.endsWith(".$blocked")) {
                return true
            }
        }

        // Pattern matching
        for (pattern in blockedUrlPatterns) {
            if (fullUrl.contains(pattern)) {
                return true
            }
        }

        return false
    }

    private fun isWhitelisted(host: String): Boolean {
        return host.endsWith("jiosaavn.com") ||
                host.endsWith("saavn.com") ||
                host.endsWith("saavncdn.com") ||
                host.endsWith("jio.com")
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            204,
            "No Content",
            mapOf("Cache-Control" to "no-store"),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
