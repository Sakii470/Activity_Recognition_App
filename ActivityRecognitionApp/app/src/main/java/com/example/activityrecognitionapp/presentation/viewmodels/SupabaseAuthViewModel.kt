package com.example.activityrecognitionapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.data.model.Session
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import com.example.activityrecognitionapp.domain.usecases.Authorization.GetCurrentUserUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.IsSessionExpiredUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.LoginUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.LogoutUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.RefreshSessionUseCase
import com.example.activityrecognitionapp.domain.usecases.Authorization.SignUpUseCase
import com.example.activityrecognitionapp.presentation.states.LoginUiState
import com.example.activityrecognitionapp.presentation.states.UserState
import com.example.activityrecognitionapp.utils.Event
import com.example.activityrecognitionapp.utils.EventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.gotrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * ViewModel responsible for handling user authentication and authorization using Supabase.
 * It manages login, sign-up, logout processes, session management, and updates the UI state accordingly.
 *
 * @property tokenRepository Repository for managing token storage and retrieval.
 * @property dataRepository Repository for handling data operations.
 * @property bluetoothRepository Repository for managing Bluetooth-related operations.
 * @property loginUseCase Use case for handling user login.
 * @property signUpUseCase Use case for handling user sign-up.
 * @property logoutUseCase Use case for handling user logout.
 * @property refreshSessionUseCase Use case for refreshing user sessions.
 * @property isSessionExpiredUseCase Use case for checking if the user session has expired.
 * @property getCurrentUserUseCase Use case for retrieving the current user's information.
 */
@HiltViewModel
class SupabaseAuthViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val dataRepository: DataRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val isSessionExpiredUseCase: IsSessionExpiredUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : ViewModel() {

    /**
     * MutableStateFlow holding the UI state for login and sign-up screens.
     * Exposed as an immutable StateFlow to observers.
     */
    private val _uiLoginState = MutableStateFlow(LoginUiState())
    val uiLoginState: StateFlow<LoginUiState> = _uiLoginState.asStateFlow()

    // Pomocnicza metoda (tylko do testów)
    fun setUiLoginState(state: LoginUiState) {
        _uiLoginState.value = state
    }

    /**
     * MutableStateFlow holding the result of the login operation.
     * Exposed as an immutable StateFlow to observers.
     */
    private val _loginResult = MutableStateFlow(Result.success(Unit))
    val loginResult: StateFlow<Result<Unit>> = _loginResult.asStateFlow()

    /**
     * Initialization block that resets the user state and checks if the current session has expired.
     */
    init {
        resetUserState()
        checkSessionExpired()
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

    /**
     * Updates the user's name in the UI state.
     *
     * @param newName The new name entered by the user.
     */
    fun onNameChange(newName: String) {
        _uiLoginState.update { it.copy(userName = newName) }
    }

    /**
     * Handles user login by invoking the [LoginUseCase] with the provided email and password.
     * Upon successful login, retrieves the current user's information and updates the UI state.
     * If login fails, updates the UI state with the corresponding error message.
     *
     * @param email The user's email address.
     * @param password The user's password.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try{
                // Execute the login use case with provided credentials
                val result = loginUseCase(email, password) ?: Result.failure(Exception("Unexpected null result"))

                if (result.isSuccess) {
                    // Retrieve the current user's information after successful login
                    val user = getCurrentUserUseCase()
                    val userName =
                        user?.userMetadata?.get("display_name")?.jsonPrimitive?.content ?: ""

                    // Update the UI state to reflect successful login
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Success("Logged in successfully"),
                            isLoggedIn = true,
                            userName = userName
                        )
                    }
                } else {
                    // Extract and format the error message
                    val errorMessage =
                        result.exceptionOrNull()?.message?.substringBefore("URL") ?: "Login failed"

                    // Update the UI state to reflect login failure
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Error(errorMessage),
                            isLoggedIn = false
                        )
                    }
                }
            }catch (e:Exception){
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Error("Login failed"),
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    /**
     * Handles user sign-up by invoking the [SignUpUseCase] with the provided name, email, and password.
     * Upon successful registration, updates the UI state accordingly.
     * If sign-up fails, updates the UI state with the corresponding error message.
     *
     * @param name The user's full name.
     * @param email The user's email address.
     * @param password The user's password.
     */
    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            // Execute the sign-up use case with provided details
            val result = signUpUseCase(name, email, password)

            if (result.isSuccess) {
                // Update the UI state to reflect successful registration
                _uiLoginState.update {
                    it.copy(userState = UserState.Success("Registered successfully"))
                }
            } else {
                // Extract and format the error message
                val errorMessage = result.exceptionOrNull()?.message?.substringBefore("URL")
                    ?: "Registration failed"

                // Update the UI state to reflect sign-up failure
                _uiLoginState.update {
                    it.copy(userState = UserState.Error(errorMessage))
                }
            }
        }
    }

    /**
     * Saves the current session tokens (access token and refresh token) to the [TokenRepository].
     * This function retrieves the current session from Supabase and persists it for future use.
     */
    private suspend fun saveToken() {
        // Retrieve the current session from Supabase
        val currentSession = SupabaseApiClient.SupabaseClient.Client.gotrue.currentSessionOrNull()
        currentSession?.let { session ->
            val expiresAt = session.expiresAt
            val newSession = Session(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                expiresAt = expiresAt
            )
            // Save the session to the token repository
            tokenRepository.saveSession(newSession)
        }
    }



    /**
     * Logs out the current user by invoking the [LogoutUseCase].
     * Upon successful logout, updates the UI state and sends a logout event.
     * If logout fails, updates the UI state with an appropriate message.
     */
    fun logout() {
        viewModelScope.launch {
            try{
                // Execute the logout use case
                val result = logoutUseCase()

                if (result.isSuccess) {
                    // Update the UI state to reflect successful logout
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Success("Logged out successfully"),
                            isLoggedIn = false,
                        )
                    }
                    // Send a global logout event to notify other components
                    EventBus.sendEvent(Event.Logout)
                } else {
                    // Update the UI state to reflect logout failure (e.g., offline logout)
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Success("Logged out offline"),
                            isLoggedIn = false
                        )
                    }
                }
            }catch(e:Exception){
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Logged out offline"),
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    /**
     * Checks whether the current user session has expired.
     * If the session is expired, updates the UI state with an error and logs out the user.
     * If the session is still valid, attempts to refresh the session and updates the UI state accordingly.
     */
    fun checkSessionExpired() {
        viewModelScope.launch {
            try {
                // Determine if the current session has expired
                val isExpired = isSessionExpiredUseCase.invoke()
                if (isExpired) {
                    // Update the UI state to indicate that the session has expired
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Error("Session Expired"),
                            isLoggedIn = false
                        )
                    }
                } else {
                    // Attempt to refresh the session if it hasn't expired
                    val refreshResult = refreshSessionUseCase.invoke()
                    if (refreshResult.isSuccess) {
                        val user = getCurrentUserUseCase.invoke()
                        val userName =
                            user?.userMetadata?.get("display_name")?.jsonPrimitive?.content ?: "Unknown"

                        // Update the UI state to reflect a successful session refresh
                        _uiLoginState.update {
                            it.copy(
                                userState = UserState.Success("Session refreshed successfully"),
                                isLoggedIn = true,
                                userName = userName
                            )
                        }
                    } else {
                        val errorMessage = refreshResult.exceptionOrNull()?.message?.substringBefore("URL")
                            ?: "Token Refresh failed"

                        // Update the UI state to reflect the failure to refresh the session
                        _uiLoginState.update {
                            it.copy(
                                userState = UserState.Error(errorMessage),
                                isLoggedIn = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle unexpected errors
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Error("An unexpected error occurred: ${e.message}"),
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    /**
     * Resets the [userState] in the UI state to [UserState.Idle].
     * This is useful to prevent repeated navigation or UI updates based on the previous state.
     */
    fun resetUserState() {
        _uiLoginState.update { it.copy(userState = UserState.Idle) }
    }

}
