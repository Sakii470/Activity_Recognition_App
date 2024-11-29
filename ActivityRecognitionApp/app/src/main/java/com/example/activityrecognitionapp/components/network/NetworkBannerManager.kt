package com.example.activityrecognitionapp.components.network

import android.util.Log
import com.example.activityrecognitionapp.data.network.NetworkConnectivityObserver
import com.example.activityrecognitionapp.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manages network connectivity banners based on connection status.
 *
 * Observes network changes and updates UI to show banners for connection status.
 *
 * @param connectivityObserver Observes network connectivity.
 * @param dataRepository Handles data synchronization.
 * @param coroutineScope Coroutine scope for asynchronous tasks.
 */
class NetworkBannerManager(
    private val connectivityObserver: NetworkConnectivityObserver,
    private val dataRepository: DataRepository,
    private val coroutineScope: CoroutineScope
) {

    // Current network availability status
    private val _isNetworkAvailable = MutableStateFlow(connectivityObserver.isConnected.value)
    val isNetworkAvailable: StateFlow<Boolean> get() = _isNetworkAvailable

    // Controls banner visibility
    private val _showNetworkBanner = MutableStateFlow(false)
    val showNetworkBanner: StateFlow<Boolean> get() = _showNetworkBanner

    // Tracks previous network state
    private var previousNetworkState: Boolean? = null

    init {
        startObserving()
    }

    /**
     * Starts observing network connectivity changes.
     */
    private fun startObserving() {
        connectivityObserver.start()
        coroutineScope.launch {
            connectivityObserver.isConnected.collect { isConnected ->
                Log.d("NetworkBannerManager", "Connectivity status: $isConnected")
                if (previousNetworkState == null) {
                    previousNetworkState = isConnected
                    if (!isConnected) {
                        showDisconnectedBanner()
                    }
                } else if (previousNetworkState != isConnected) {
                    previousNetworkState = isConnected
                    if (isConnected) {
                        showReconnectedBanner()
                    } else {
                        showDisconnectedBanner()
                    }
                }
            }
        }
    }

    /**
     * Displays the disconnected network banner.
     */
    private fun showDisconnectedBanner() {
        _isNetworkAvailable.value = false
        _showNetworkBanner.value = true
        Log.d("NetworkBannerManager", "Network disconnected. Banner should be shown.")
    }

    /**
     * Displays the reconnected network banner and synchronizes data.
     */
    private fun showReconnectedBanner() {
        _isNetworkAvailable.value = true
        _showNetworkBanner.value = true
        Log.d("NetworkBannerManager", "Network reconnected. Synchronizing data and showing banner.")

        // Synchronize data
        coroutineScope.launch {
            try {
                dataRepository.syncLocalChanges()
                Log.d("NetworkBannerManager", "Data synchronized after reconnection.")
            } catch (e: Exception) {
                Log.e("NetworkBannerManager", "Error during data synchronization", e)
            }
        }

        // Hide banner after 2 seconds
        coroutineScope.launch {
            kotlinx.coroutines.delay(2000)
            _showNetworkBanner.value = false
            Log.d("NetworkBannerManager", "Network banner dismissed after reconnection.")
        }
    }

    /**
     * Allows manual dismissal of the network banner.
     */
    fun dismissBanner() {
        _showNetworkBanner.value = false
        Log.d("NetworkBannerManager", "Network banner manually dismissed.")
    }

    /**
     * Stops observing network connectivity changes.
     */
    fun stopObserving() {
        connectivityObserver.stop()
    }
}
