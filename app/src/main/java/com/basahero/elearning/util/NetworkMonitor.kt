package com.basahero.elearning.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.basahero.elearning.worker.SyncProgressWorker
import com.basahero.elearning.worker.SyncStudentWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─────────────────────────────────────────────────────────────────────────────
// NetworkMonitor
// Watches connectivity changes. When device comes ONLINE, automatically
// enqueues all sync workers with jitter delay.
// ─────────────────────────────────────────────────────────────────────────────
class NetworkMonitor(private val context: Context) {

    private val tag = "NetworkMonitor"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(isCurrentlyConnected())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Log.d(tag, "Network available — device is online")
            _isOnline.value = true

            // Trigger syncs when Wi-Fi connects!
            SyncProgressWorker.enqueue(context)
            SyncStudentWorker.enqueue(context)

            // Note: When you finish Phase 5 (Speech), uncomment the line below:
            // SyncPronunciationWorker.enqueue(context)

            Log.d(tag, "All sync workers enqueued with jitter delay")
        }

        override fun onLost(network: Network) {
            Log.d(tag, "Network lost — device is offline")
            _isOnline.value = false
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
        }
    }

    fun startWatching() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            Log.d(tag, "NetworkMonitor started")
        } catch (e: Exception) {
            Log.e(tag, "Failed to register network callback: ${e.message}")
        }
    }

    fun stopWatching() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(tag, "NetworkMonitor stopped")
        } catch (e: Exception) {
            Log.e(tag, "Failed to unregister network callback: ${e.message}")
        }
    }

    private fun isCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}