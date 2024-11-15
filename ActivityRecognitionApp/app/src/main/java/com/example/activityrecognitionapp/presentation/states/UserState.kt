package com.example.activityrecognitionapp.presentation.states

sealed class UserState {
    object Idle : UserState()
    object Loading: UserState()
    data class Success(val message: String): UserState()
    data class Error(val message: String): UserState()


}