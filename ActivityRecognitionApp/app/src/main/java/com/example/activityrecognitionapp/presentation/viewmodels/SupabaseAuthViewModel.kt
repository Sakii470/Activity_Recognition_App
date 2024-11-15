package com.example.activityrecognitionapp.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.data.network.SupabaseClient
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.presentation.states.LoginUiState
import com.example.activityrecognitionapp.presentation.states.UserState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject


@HiltViewModel
class SupabaseAuthViewModel @Inject constructor(
   // application: Application,
    private val tokenRepository: TokenRepository,

) : ViewModel() {

    // Holds the UI state for login and sign-up screens.
    private val _uiLoginState = MutableStateFlow(LoginUiState())
    val uiLoginState: StateFlow<LoginUiState> = _uiLoginState.asStateFlow()

    init{
        isUserLoggedIn()
    }

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

    fun onNameChange(newName: String) {
        _uiLoginState.update { it.copy(userName = newName) }
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
                SupabaseClient.Client.gotrue.loginWith(Email) {
                    email = userEmail
                    password = userPassword
                }
                saveToken()
                saveName()
                _uiLoginState.update { it.copy(userState = UserState.Success("Logged successfully"), isLoggedIn = true) }
            } catch (e: Exception) {
                val errorMessage = e.message?.substringBefore("URL") ?: ""
                _uiLoginState.update { it.copy(userState = UserState.Error(errorMessage), isLoggedIn = false) }
            }
        }
    }

    /**
     * Handles user sign-up by calling Supabase API and saves the access token.
     * Updates the UI state based on the result of the sign-up attempt.
     */
    fun signUp() {
        val userName = _uiLoginState.value.userName
        val userEmail = _uiLoginState.value.userEmail
        val userPassword = _uiLoginState.value.userPassword

        viewModelScope.launch {
            _uiLoginState.update { it.copy(userState = UserState.Loading) }
            try {
                val userData = buildJsonObject {
                    put("display_name", userName)
                }
                // Attempt to log in using Supabase GoTrue API
                SupabaseClient.Client.gotrue.signUpWith(Email) {
                    email = userEmail
                    password = userPassword
                    data = userData
                }
                // Save the access token upon successful signup
                //saveToken()
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
            val accessToken: String? = SupabaseClient.Client.gotrue.currentAccessTokenOrNull()
            // Jeśli accessToken nie jest null, zapisz go w TokenRepository
            accessToken?.let { token ->
                tokenRepository.saveAccessToken(token)
            }
        }
    }

    private fun saveName() {
        viewModelScope.launch {
            try {
                val jwt = SupabaseClient.Client.gotrue.currentSessionOrNull()?.accessToken
                if (jwt != null) {
                    val user = SupabaseClient.Client.gotrue.retrieveUser(jwt)
                    // Pobierz imię z `user_metadata`, jeśli jest dostępne
                    val name = user?.userMetadata?.get("display_name") as? String
                    name?.let { userName ->

                        _uiLoginState.update { it.copy(userName = userName) }
                    }
                } else {
                    // Obsłuż przypadek, gdy token jest null, np. użytkownik nie jest zalogowany
                    Log.e("Error", "User is not authenticated. JWT token is null.")
                }
            } catch (e: Exception) {
                Log.e("TokenRepository", "Error retrieving user name", e)
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
                SupabaseClient.Client.gotrue.logout()
                tokenRepository.clearAccessToken()
                _uiLoginState.update { it.copy(userState = UserState.Success("Logout sucessfully"), isLoggedIn = false) }
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
                    _uiLoginState.update { it.copy(userState = UserState.Success("User not logged in!"), isLoggedIn = false) }
                } else {
                    SupabaseClient.Client.gotrue.retrieveUser(token)
                    SupabaseClient.Client.gotrue.refreshCurrentSession()
                    saveToken() // Zapis tokenu, jeśli sesja została odświeżona
                    _uiLoginState.update { it.copy(userState = UserState.Success("User already logged in!"), isLoggedIn = true)
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message?.substringBefore("URL") ?: "Error occurred"
                _uiLoginState.update {
                    it.copy(userState = UserState.Success(errorMessage), isLoggedIn = false)
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
