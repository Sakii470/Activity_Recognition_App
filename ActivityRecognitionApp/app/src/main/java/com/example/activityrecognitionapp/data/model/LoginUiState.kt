package com.example.activityrecognitionapp.data.model

data class LoginUiState(
    val userEmail: String = "",
    val userPassword: String = "",
    val userState: UserState = UserState.Idle
)
