package com.example.activityrecognitionapp.presentation.states

data class LoginUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userPassword: String = "",
    val userState: UserState = UserState.Idle,
    val isLoggedIn: Boolean = false,
)
