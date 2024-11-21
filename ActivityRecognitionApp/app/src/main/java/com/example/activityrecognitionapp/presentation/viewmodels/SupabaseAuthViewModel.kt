package com.example.activityrecognitionapp.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.data.model.Session
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import com.example.activityrecognitionapp.domain.usecase.Authorization.GetCurrentUserUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.IsSessionExpiredUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.LoginUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.LogoutUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.RefreshSessionUseCase
import com.example.activityrecognitionapp.domain.usecase.Authorization.SignUpUseCase
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


@HiltViewModel
class SupabaseAuthViewModel @Inject constructor(
    // application: Application,
    private val tokenRepository: TokenRepository,
    private val dataRepository: DataRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val isSessionExpiredUseCase: IsSessionExpiredUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase

    ) : ViewModel() {

    // Holds the UI state for login and sign-up screens.
    private val _uiLoginState = MutableStateFlow(LoginUiState())
    val uiLoginState: StateFlow<LoginUiState> = _uiLoginState.asStateFlow()


    private val _loginResult = MutableStateFlow(Result.success(Unit))
    val loginResult: StateFlow<Result<Unit>> = _loginResult.asStateFlow()

    // val loginResult = MutableLiveData<Result<Unit>>()

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

    fun onNameChange(newName: String) {
        _uiLoginState.update { it.copy(userName = newName) }
    }

    /**
     * Handles user login by calling Supabase API and saves the access token.
     * Updates the UI state based on the result of the login attempt.
     */

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = loginUseCase(email, password)


            if (result.isSuccess) {
                val user = getCurrentUserUseCase()
                val userName = user?.userMetadata?.get("display_name")?.jsonPrimitive?.content ?: "Unknown"
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Logged successfully"),
                        isLoggedIn = true,
                        userName = userName
                    )
                }
            } else {
                val errorMessage =
                    result.exceptionOrNull()?.message?.substringBefore("URL") ?: "Login failed"
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Error(errorMessage),
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    /**
     * Handles user sign-up by calling Supabase API and saves the access token.
     * Updates the UI state based on the result of the sign-up attempt.
     */


    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            val result = signUpUseCase(name, email, password)

            if (result.isSuccess) {
                _uiLoginState.update { it.copy(userState = UserState.Success("Registered successfully")) }
            } else {
                val errorMessage = result.exceptionOrNull()?.message?.substringBefore("URL")
                    ?: "Registration failed"
                _uiLoginState.update { it.copy(userState = UserState.Error(errorMessage)) }
            }
        }
    }



    /**
     * Saves the current access token and refresh token to the [TokenRepository].
     */
    private suspend fun saveToken() {
        val currentSession = SupabaseApiClient.SupabaseClient.Client.gotrue.currentSessionOrNull()
        currentSession?.let { session ->
            val expiresAt = session.expiresAt
            val newSession = Session(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                expiresAt = expiresAt
            )
            tokenRepository.saveSession(newSession)
        }
    }

    /**
     * Saves the user's display name to the [TokenRepository].
     */
    private suspend fun saveName() {
        try {
            val jwt =
                SupabaseApiClient.SupabaseClient.Client.gotrue.currentSessionOrNull()?.accessToken
            if (jwt != null) {
                val user = SupabaseApiClient.SupabaseClient.Client.gotrue.retrieveUser(jwt)
                user?.userMetadata?.let { metadata ->
                    val displayName = metadata["display_name"]?.jsonPrimitive?.content
                    if (displayName != null) {
                        tokenRepository.saveUserName(displayName)
                        Log.d(
                            "YourViewModel",
                            "Retrieved displayName from uiLoginState: ${uiLoginState.value.userName}"
                        )
                        Log.d("YourViewModel", "Retrieved displayName from DataStore: $displayName")
                    } else {
                        Log.e(
                            "UserMetadata",
                            "`display_name` is not available or not a String in userMetadata."
                        )
                    }
                } ?: run {
                    Log.e("UserMetadata", "userMetadata is null.")
                }
            } else {
                Log.e("Error", "User is not authenticated. JWT token is null.")
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuthViewModel", "Error retrieving user name", e)
        }
    }

    /**
     * Logs out the user by calling Supabase API and clears the access token.
     * Updates the UI state based on the result of the logout attempt.
     */
    fun logout() {
        viewModelScope.launch {
            val result = logoutUseCase()

            if (result.isSuccess) {
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Logout successfully"),
                        isLoggedIn = false,

                    )
                }
                EventBus.sendEvent(Event.Logout)
            } else {
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Logout offline"),
                        isLoggedIn = false
                    )
                }
            }


        }
    }

//    fun logout() {
//        viewModelScope.launch {
//            _uiLoginState.update { it.copy(userState = UserState.Loading) }
//            var logoutMessage = "Logout successfully"
//            try {
//                SupabaseApiClient.SupabaseClient.Client.gotrue.logout()
//                tokenRepository.clearTokens()
//                dataRepository.clearAllData()
//                bluetoothRepository.closeConnection()
//                _uiLoginState.update {
//                    it.copy(
//                        userState = UserState.Success("Logout successfully"),
//                        isLoggedIn = false,
//                    )
//                }
//            } catch (e: Exception) {
//                // Logowanie błędu, ale nie zatrzymujemy procesu wylogowania
//                val errorMessage = e.message ?: "Logout failed"
//                Log.e("SupabaseAuthViewModel", "Logout error: $errorMessage")
//                _uiLoginState.update {
//                    it.copy(
//                        userState = UserState.Success("Logout offline"),
//                        isLoggedIn = false
//                    )
//                }
//            } finally {
//                // Zawsze czyścimy tokeny i dane
//                tokenRepository.clearTokens()
//                dataRepository.clearAllData()
//                bluetoothRepository.closeConnection()
//                _uiLoginState.update {
//                    it.copy(
//                        userState = UserState.Success(logoutMessage),
//                        isLoggedIn = false
//                    )
//                }
//            }
//        }
//    }

    /**
     * Checks if the user is already logged in by retrieving the access token from the repository.
     * Updates the UI state accordingly.
     */
//    fun isUserLoggedIn() {
//        viewModelScope.launch {
//            try {
//                _uiLoginState.update { it.copy(userState = UserState.Loading) }
//                val token = tokenRepository.getAccessToken()
//                if (token.isNullOrEmpty()) {
//                    _uiLoginState.update {
//                        it.copy(
//                            userState = UserState.Success("User not logged in!"),
//                            isLoggedIn = false
//                        )
//                    }
//                } else {
//                    tokenRefresh()
//                }
//            } catch (e: Exception) {
//                val errorMessage = e.message?.substringBefore("URL") ?: "Error occurred"
//                _uiLoginState.update {
//                    it.copy(userState = UserState.Error(errorMessage), isLoggedIn = false)
//                }
//            }
//        }
//    }

    fun checkSessionExpired() {
        viewModelScope.launch {
            val isExpired = isSessionExpiredUseCase()
            if (isExpired) {
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Error("Session Expired"),
                        isLoggedIn = false
                    )
                }
                // Optional: Handle logout or navigate to login screen
            } else {
                val refreshResult = refreshSessionUseCase()
                if (refreshResult.isSuccess) {
                    val user = getCurrentUserUseCase()
                    val userName = user?.userMetadata?.get("display_name")?.jsonPrimitive?.content ?: "Unknown"
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
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Error(errorMessage),
                            isLoggedIn = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Resets the [userState] in the UI state to [UserState.Idle].
     * This is useful to prevent repeated navigation or UI updates based on previous state.
     */
    fun resetUserState() {
        _uiLoginState.update { it.copy(userState = UserState.Idle) }
    }

    /**
     * Observes changes to the user's display name and updates the UI state accordingly.
     */
//    private fun observeDisplayName() {
//        viewModelScope.launch {
//            tokenRepository.getDisplayName().collect { displayName ->
//                Log.d("YourViewModel", "Retrieved displayName from DataStore: $displayName")
//                _uiLoginState.update {
//                    it.copy(userName = displayName ?: "Unknown")
//                }
//                Log.d(
//                    "YourViewModel",
//                    "Retrieved displayName from _uiLoginState: ${_uiLoginState.value.userName}"
//                )
//            }
//        }
//    }

    /**
     * Refreshes the session if it has expired.
     * Updates the UI state accordingly.
     */

    fun tokenRefresh() {
        viewModelScope.launch {
            val result = refreshSessionUseCase()

            if (result.isSuccess) {
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Session refreshed successfully"),
                        isLoggedIn = true
                    )
                }
                Log.d("SupabaseAuthRepository", "Session refresh successful")

            } else {
                val errorMessage = result.exceptionOrNull()?.message?.substringBefore("URL")
                    ?: "Token Refresh failed"
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Error(errorMessage),
                        isLoggedIn = false
                    )

                }

            }
        }
    }
}

//    private suspend fun tokenRefresh() {
//        try {
//            if (tokenRepository.isSessionExpired()) {
//                Log.d("SupabaseAuthViewModel", "Session expired. Refreshing session.")
//                SupabaseApiClient.SupabaseClient.Client.gotrue.refreshCurrentSession()
//                saveToken()
//                saveName()
//                _uiLoginState.update {
//                    it.copy(
//                        userState = UserState.Success("Session refreshed successfully"),
//                        isLoggedIn = true
//                    )
//                }
//            } else {
//                Log.d("SupabaseAuthViewModel", "Session is still valid.")
//                try {
//                    SupabaseApiClient.SupabaseClient.Client.gotrue.retrieveUser(tokenRepository.getAccessToken()!!)
//                    saveName()
//                    _uiLoginState.update {
//                        it.copy(
//                            userState = UserState.Success("User already logged in!"),
//                            isLoggedIn = true
//                        )
//                    }
//                } catch (e: Exception) {
//                    Log.e("SupabaseAuthViewModel", "Failed to retrieve user: ${e.message}")
//                    _uiLoginState.update {
//                        it.copy(
//                            userState = UserState.Error("Failed to retrieve user: ${e.message}"),
//                            isLoggedIn = false
//                        )
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            val errorMessage = e.message?.substringBefore("URL") ?: "Failed to refresh session"
//            Log.e("SupabaseAuthViewModel", errorMessage)
//            _uiLoginState.update {
//                it.copy(
//                    userState = UserState.Error(errorMessage),
//                    isLoggedIn = false
//                )
//            }
//        }
//    }







