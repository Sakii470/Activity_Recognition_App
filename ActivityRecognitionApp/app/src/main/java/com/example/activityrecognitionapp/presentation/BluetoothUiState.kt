package com.example.activityrecognitionapp.presentation

import com.example.activityrecognitionapp.domain.BluetoothDevice

/**
 * Represents the UI state.
 * This data class holds information about scanned Bluetooth devices,
 * connection status, any error messages, and data received from connected devices.
 */

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    //val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val dataFromBluetooth: String? = "22"
)
