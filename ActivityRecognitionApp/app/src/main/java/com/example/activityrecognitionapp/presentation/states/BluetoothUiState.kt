package com.example.activityrecognitionapp.presentation.states

import android.graphics.Color
import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain

/**
 * Represents the UI state.
 * This data class holds information about scanned Bluetooth devices,
 * connection status, any error messages, and data received from connected devices.
 */

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val dataFromBluetooth: String? = null,
    val connectedDevice: BluetoothDeviceDomain? = null,
    val isBluetoothEnabled: Boolean = false,
)

data class NetworkBannerState(
    val showBanner: Boolean = false,
    val message: String = "",
    val color: Int = Color.BLUE,
)




