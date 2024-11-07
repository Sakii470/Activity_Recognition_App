package com.example.activityrecognitionapp.presentation.states

data class LoginUiState(
    val userEmail: String = "",
    val userPassword: String = "",
    val userState: UserState = UserState.Idle
)
