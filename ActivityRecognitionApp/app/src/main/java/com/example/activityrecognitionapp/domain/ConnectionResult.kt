package com.example.activityrecognitionapp.domain

/**
 * Represents the result of a Bluetooth connection attempt.
 * This sealed interface can have two states: a successful connection
 * with data received from the Bluetooth device or an error with a message
 * indicating what went wrong.

 */
sealed interface ConnectionResult {
    data class ConnectionEstablished(val dataFromBluetooth: String, val connectedDevice: BluetoothDeviceDomain?) : ConnectionResult

    data class Error(val message: String) : ConnectionResult
}