package com.example.activityrecognitionapp.components

import android.util.Log
import com.example.activityrecognitionapp.data.network.NetworkConnectivityObserver
import com.example.activityrecognitionapp.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NetworkBannerManager(
    private val connectivityObserver: NetworkConnectivityObserver,
    private val dataRepository: DataRepository,
    private val coroutineScope: CoroutineScope
) {

    private val _isNetworkAvailable = MutableStateFlow(connectivityObserver.isConnected.value)
    val isNetworkAvailable: StateFlow<Boolean> get() = _isNetworkAvailable

    private val _showNetworkBanner = MutableStateFlow(false)
    val showNetworkBanner: StateFlow<Boolean> get() = _showNetworkBanner

    private var previousNetworkState: Boolean? = null

    init {
        startObserving()
    }

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

    private fun showDisconnectedBanner() {
        _isNetworkAvailable.value = false
        _showNetworkBanner.value = true
        Log.d("NetworkBannerManager", "Network disconnected. Banner should be shown.")
    }

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

    fun dismissBanner() {
        _showNetworkBanner.value = false
        Log.d("NetworkBannerManager", "Network banner manually dismissed.")
    }

    fun stopObserving() {
        connectivityObserver.stop()
    }
}
