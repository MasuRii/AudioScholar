package edu.cit.audioscholar.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ServerConnectionManager {
    private const val TIMEOUT_MS = 10000L

    private const val DEV_URL = "https://mastodon-balanced-randomly.ngrok-free.app/"
    private const val LOCAL_URL = "http://192.168.137.1:8080/"
    private const val PROD_URL = "https://it342-g3-audioscholar-onrender-com.onrender.com/"

    var currentBaseUrl: String = PROD_URL
        private set

    private var isInitialized = false

    suspend fun determineBestServer(): String = coroutineScope {
        if (isInitialized) {
            Log.d("ServerCheck", "Already initialized. Keeping: $currentBaseUrl")
            return@coroutineScope currentBaseUrl
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        // Use Dispatchers.IO to avoid NetworkOnMainThreadException
        val devCheck = async(Dispatchers.IO) { checkHealth(client, DEV_URL, "DEV") }
        val localCheck = async(Dispatchers.IO) { checkHealth(client, LOCAL_URL, "LOCAL") }
        val prodCheck = async(Dispatchers.IO) { checkHealth(client, PROD_URL, "PROD") }

        val isDevLive = devCheck.await()
        val isLocalLive = localCheck.await()
        val isProdLive = prodCheck.await()

        Log.d("ServerCheck", "Results -> DEV: $isDevLive, LOCAL: $isLocalLive, PROD: $isProdLive")

        currentBaseUrl = when {
            isDevLive -> DEV_URL
            isLocalLive -> LOCAL_URL
            isProdLive -> PROD_URL
            else -> PROD_URL
        }

        isInitialized = true
        Log.d("ServerCheck", "Initialized. Selected: $currentBaseUrl")

        return@coroutineScope currentBaseUrl
    }

    private fun checkHealth(client: OkHttpClient, url: String, tag: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("ngrok-skip-browser-warning", "true")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                // Accept 2xx (Success), 3xx (Redirect), 404 (Not Found),
                // 401/403 (Unauthorized/Forbidden) - all imply server is reachable
                val isAlive = response.isSuccessful ||
                        code in 300..399 ||
                        code == 404 ||
                        code == 401 ||
                        code == 403
                Log.d("ServerCheck", "[$tag] $url -> Code: $code, Alive: $isAlive")
                isAlive
            }
        } catch (e: Exception) {
            Log.e("ServerCheck", "[$tag] Failed to connect to $url: ${e.message}")
            false
        }
    }
}