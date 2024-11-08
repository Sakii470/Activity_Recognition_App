package com.example.activityrecognitionapp.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.presentation.states.LoginUiState
import com.example.activityrecognitionapp.presentation.states.UserState
import com.example.activityrecognitionapp.data.network.SupaBaseClient.client
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupabaseAuthViewModel @Inject constructor(
    application: Application,
    private val tokenRepository: TokenRepository,

) : AndroidViewModel(application) {

    // Holds the UI state for login and sign-up screens.
    private val _uiLoginState = MutableStateFlow(LoginUiState())
    val uiLoginState: StateFlow<LoginUiState> = _uiLoginState.asStateFlow()

    /**
     * Updates the user's email in the UI state.
     *
     * @param newEmail The new email entered by the user.
     */
    fun onEmailChange(newEmail: String) {
        _uiLoginState.update { it.copy(userEmail = newEmail) }
    }
    /**
     * Updates the user's password in the UI state.
     *
     * @param newPassword The new password entered by the user.
     */
    fun onPasswordChange(newPassword: String) {
        _uiLoginState.update { it.copy(userPassword = newPassword) }
    }

    /**
     * Handles user login by calling Supabase API and saves the access token.
     * Updates the UI state based on the result of the login attempt.
     */
    fun login() {
        val userEmail = _uiLoginState.value.userEmail
        val userPassword = _uiLoginState.value.userPassword

        viewModelScope.launch {
            _uiLoginState.update { it.copy(userState = UserState.Loading) }
            try {
                client.gotrue.loginWith(Email) {
                    email = userEmail
                    password = userPassword
                }
                saveToken()
                _uiLoginState.update { it.copy(userState = UserState.Success("Logged successfully")) }
            } catch (e: Exception) {
                val errorMessage = e.message?.substringBefore("URL") ?: ""
                _uiLoginState.update { it.copy(userState = UserState.Error(errorMessage)) }
            }
        }
    }

    /**
     * Handles user sign-up by calling Supabase API and saves the access token.
     * Updates the UI state based on the result of the sign-up attempt.
     */
    fun signUp() {
        val userEmail = _uiLoginState.value.userEmail
        val userPassword = _uiLoginState.value.userPassword

        viewModelScope.launch {
            _uiLoginState.update { it.copy(userState = UserState.Loading) }
            try {
                // Attempt to log in using Supabase GoTrue API
                client.gotrue.signUpWith(Email) {
                    email = userEmail
                    password = userPassword
                }
                // Save the access token upon successful login
                saveToken()
                _uiLoginState.update { it.copy(userState = UserState.Success("Registered successfully")) }
            } catch (e: Exception) {
                // Handle login error and update UI state with error message
                val errorMessage = e.message?.substringBefore("URL") ?: ""
                _uiLoginState.update { it.copy(userState = UserState.Error(errorMessage)) }
            }
        }
    }

    /**
     * Saves the current access token to the [TokenRepository].
     */
    private fun saveToken() {
        viewModelScope.launch {
            // Pobierz token dostępu, jeśli jest dostępny
            val accessToken: String? = client.gotrue.currentAccessTokenOrNull()
            // Jeśli accessToken nie jest null, zapisz go w TokenRepository
            accessToken?.let { token ->
                tokenRepository.saveAccessToken(token)
            }
        }
    }

    /**
     * Logs out the user by calling Supabase API and clears the access token.
     * Updates the UI state based on the result of the logout attempt.
     */
    fun logout() {
        viewModelScope.launch {
            _uiLoginState.update { it.copy(userState = UserState.Loading) }
            try {
                client.gotrue.logout()
                tokenRepository.clearAccessToken()
                _uiLoginState.update { it.copy(userState = UserState.Success("Logout sucessfully")) }
            } catch (e: Exception) {
                val errorMessage = e.message ?: ""
                _uiLoginState.update { it.copy(userState = UserState.Error(errorMessage)) }
            }
        }
    }

    /**
     * Checks if the user is already logged in by retrieving the access token from the repository.
     * Updates the UI state accordingly.
     */
    fun isUserLoggedIn() {
        viewModelScope.launch {
            try {
                _uiLoginState.update { it.copy(userState = UserState.Loading) }
                // Pobiera pierwszą wartość z accessTokenFlow
                val token = tokenRepository.accessTokenFlow.firstOrNull()
                if (token.isNullOrEmpty()) {
                    _uiLoginState.update { it.copy(userState = UserState.Success("User not logged in!")) }
                } else {
                    client.gotrue.retrieveUser(token)
                    client.gotrue.refreshCurrentSession()
                    saveToken() // Zapis tokenu, jeśli sesja została odświeżona
                    _uiLoginState.update { it.copy(userState = UserState.Success("User already logged in!")) }
                }
            } catch (e: Exception) {
                val errorMessage = e.message?.substringBefore("URL") ?: "Error occurred"
                _uiLoginState.update {
                    it.copy(userState = UserState.Success(errorMessage))
                }
            }
        }
    }

    /**
     * Resets the [userState] in the UI state to [UserState.Idle].
     * This is useful to prevent repeated navigation or UI updates based on previous state.
     */
    fun resetUserState() {
        _uiLoginState.update { it.copy(userState = UserState.Idle) }  // Ustaw stan na wartość domyślną, np. Idle
    }


}
