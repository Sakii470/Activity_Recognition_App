package com.example.activityrecognitionapp.domain.chat

sealed interface ConnectionResult {
    object ConnectionEstabilished : ConnectionResult
    data class Error(val message: String) : ConnectionResult
}