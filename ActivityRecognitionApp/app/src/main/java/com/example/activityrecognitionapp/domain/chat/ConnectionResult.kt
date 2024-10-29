package com.example.activityrecognitionapp.domain.chat

/**
 * Represents the result of a Bluetooth connection attempt.
 * This sealed interface can have two states: a successful connection
 * with data received from the Bluetooth device or an error with a message
 * indicating what went wrong.

 */
sealed interface ConnectionResult {
    data class ConnectionEstabilished(val dataFromBluetooth: String) : ConnectionResult

    data class Error(val message: String) : ConnectionResult
}