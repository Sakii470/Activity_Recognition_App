package com.example.activityrecognitionapp.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Observes the network connectivity status of the device.
 *
 * This class utilizes Android's [ConnectivityManager] to monitor changes in network connectivity
 * and exposes the current connectivity status as a [StateFlow]. It allows other parts of the
 * application to reactively respond to connectivity changes.
 *
 * @param context The application context used to access system services.
 */
class NetworkConnectivityObserver(context: Context) {

    // Initializes the ConnectivityManager to monitor network changes
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // MutableStateFlow to hold the current connectivity status
    private val _isConnected = MutableStateFlow(checkCurrentConnection())

    /**
     * Publicly exposed StateFlow to observe connectivity status.
     * - `true` indicates the device is connected to the internet.
     * - `false` indicates no internet connection.
     */
    val isConnected: StateFlow<Boolean> get() = _isConnected

    // Defines a NetworkCallback to handle connectivity changes
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        /**
         * Called when the device gains network connectivity.
         *
         * @param network The [Network] object representing the new network.
         */
        override fun onAvailable(network: Network) {
            _isConnected.value = true
            Log.d("NetworkConnectivityObserver", "Network available")
        }

        /**
         * Called when the device loses network connectivity.
         *
         * @param network The [Network] object representing the lost network.
         */
        override fun onLost(network: Network) {
            _isConnected.value = false
            Log.d("NetworkConnectivityObserver", "Network lost")
        }
    }

    /**
     * Checks the current network connectivity status.
     *
     * @return `true` if the device is connected to the internet, `false` otherwise.
     */
    private fun checkCurrentConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Starts observing network connectivity changes.
     *
     * Registers the [networkCallback] with the [ConnectivityManager] to receive updates
     * about network availability.
     */
    fun start() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        Log.d("NetworkConnectivityObserver", "Started network connectivity monitoring")
    }

    /**
     * Stops observing network connectivity changes.
     *
     * Unregisters the [networkCallback] from the [ConnectivityManager], ceasing to receive
     * further network updates.
     */
    fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        Log.d("NetworkConnectivityObserver", "Stopped network connectivity monitoring")
    }
}
