package com.example.data.net

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ScanResult(
    val isOnline: Boolean,
    val statusCode: Int,
    val sslValid: Boolean,
    val pingMs: Int,
    val protocol: String,
    val tlsVersion: String,
    val handshakeCipher: String,
    val errorMessage: String? = null
)

object NetworkScanner {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun scanUrl(inputUrl: String): ScanResult {
        var cleanUrl = inputUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        val startTime = System.currentTimeMillis()
        return try {
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AnimeScout/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                val endTime = System.currentTimeMillis()
                val latency = (endTime - startTime).toInt()
                val handshake = response.handshake

                val isSsl = cleanUrl.startsWith("https://")
                val isSslValid = isSsl && handshake != null

                ScanResult(
                    isOnline = response.isSuccessful || response.code in 200..399,
                    statusCode = response.code,
                    sslValid = isSslValid,
                    pingMs = if (latency > 0) latency else 1,
                    protocol = response.protocol.toString(),
                    tlsVersion = handshake?.tlsVersion?.javaName ?: "None",
                    handshakeCipher = handshake?.cipherSuite?.javaName ?: "None",
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val latency = (endTime - startTime).toInt()
            ScanResult(
                isOnline = false,
                statusCode = 0,
                sslValid = false,
                pingMs = latency,
                protocol = "N/A",
                tlsVersion = "N/A",
                handshakeCipher = "N/A",
                errorMessage = e.localizedMessage ?: e.message ?: "Connection timed out"
            )
        }
    }
}
