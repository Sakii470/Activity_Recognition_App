package com.example.activityrecognitionapp.domain.chat

sealed interface ConnectionResult {
    data class ConnectionEstabilished(val dataFromBluetooth: String) : ConnectionResult

    data class Error(val message: String) : ConnectionResult
}