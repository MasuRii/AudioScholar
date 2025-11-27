package edu.cit.audioscholar.network

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ServerConnectionManager {
    private const val TIMEOUT_MS = 3000L

    private const val DEV_URL = "https://mastodon-balanced-randomly.ngrok-free.app/"
    private const val LOCAL_URL = "http://192.168.137.1:8080/"
    private const val PROD_URL = "https://it342-g3-audioscholar-onrender-com.onrender.com/"

    var currentBaseUrl: String = PROD_URL
        private set

    suspend fun determineBestServer(): String = coroutineScope {
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()

        val devCheck = async { checkHealth(client, DEV_URL) }
        val localCheck = async { checkHealth(client, LOCAL_URL) }
        val prodCheck = async { checkHealth(client, PROD_URL) }

        val isDevLive = devCheck.await()
        val isLocalLive = localCheck.await()
        val isProdLive = prodCheck.await()

        currentBaseUrl = when {
            isDevLive -> DEV_URL
            isLocalLive -> LOCAL_URL
            isProdLive -> PROD_URL
            else -> PROD_URL
        }

        return@coroutineScope currentBaseUrl
    }

    private fun checkHealth(client: OkHttpClient, url: String): Boolean {
        return try {
            val request = Request.Builder().url(url).head().build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}