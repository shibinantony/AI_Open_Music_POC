package com.brave.jsabmusic.api

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Standard DES-ECB Media URL Decryption Engine for JioSaavn CDN Streams.
 * Resolves encrypted media tokens into uncompressed 320 kbps AAC/MP4 direct stream URLs.
 */
object MediaUrlResolver {

    // Primary key from jiosaavn-api reference; fallback key for legacy tracks
    private val DES_KEYS = listOf("38346591", "38346536")

    /**
     * Decrypts the encrypted_media_url from JioSaavn API responses and resolves
     * the direct 320 kbps media stream link on saavncdn.com.
     */
    fun resolve320KbpsStreamUrl(encryptedUrl: String, previewUrl: String): String {
        if (encryptedUrl.isNotEmpty()) {
            for (keyStr in DES_KEYS) {
                try {
                    val key = keyStr.toByteArray(Charsets.UTF_8)
                    val keySpec = SecretKeySpec(key, "DES")
                    val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, keySpec)

                    val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
                    val decryptedBytes = cipher.doFinal(decodedBytes)
                    val directUrl = String(decryptedBytes, Charsets.UTF_8).trim()

                    if (directUrl.startsWith("http")) {
                        // Upgrade to pristine 320 kbps uncompressed AAC stream
                        return directUrl
                            .replace("_96.mp4", "_320.mp4")
                            .replace("_96_p.mp4", "_320.mp4")
                            .replace("_160.mp4", "_320.mp4")
                    }
                } catch (e: Exception) {
                    // Try next key or fallback
                }
            }
        }

        // High-Quality fallback from preview URL
        if (previewUrl.isNotEmpty()) {
            return previewUrl
                .replace("preview.saavncdn.com", "aac.saavncdn.com")
                .replace("_96_p.mp4", "_320.mp4")
                .replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
        }

        return ""
    }

    /**
     * Upgrades low-res artwork URLs (e.g. 150x150) to pristine 500x500 high-res.
     */
    fun upgradeArtworkUrl(artworkUrl: String): String {
        if (artworkUrl.isEmpty()) return ""
        return artworkUrl
            .replace("150x150", "500x500")
            .replace("50x50", "500x500")
            .replace("http://", "https://")
    }
}
