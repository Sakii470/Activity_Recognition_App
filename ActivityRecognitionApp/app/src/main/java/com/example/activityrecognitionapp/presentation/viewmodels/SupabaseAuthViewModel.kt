package com.example.activityrecognitionapp.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.data.model.Session
import com.example.activityrecognitionapp.data.network.SupabaseClient
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.BluetoothController
import com.example.activityrecognitionapp.presentation.states.LoginUiState
import com.example.activityrecognitionapp.presentation.states.UserState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject


@HiltViewModel
class SupabaseAuthViewModel @Inject constructor(
    // application: Application,
    private val tokenRepository: TokenRepository,
    private val dataRepository: DataRepository,
    private val bluetoothController: BluetoothController

) : ViewModel() {

    // Holds the UI state for login and sign-up screens.
    private val _uiLoginState = MutableStateFlow(LoginUiState())
    val uiLoginState: StateFlow<LoginUiState> = _uiLoginState.asStateFlow()

    init {
        isUserLoggedIn()
        observeDisplayName()
        resetUserState()
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
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Logged successfully"),
                        isLoggedIn = true
                    )
                }
            } catch (e: Exception) {
                val errorMessage = e.message?.substringBefore("URL") ?: "Login failed"
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
                // Attempt to sign up using Supabase GoTrue API
                SupabaseClient.Client.gotrue.signUpWith(Email) {
                    email = userEmail
                    password = userPassword
                    data = userData
                }
                _uiLoginState.update { it.copy(userState = UserState.Success("Registered successfully")) }
            } catch (e: Exception) {
                val errorMessage = e.message?.substringBefore("URL") ?: "Registration failed"
                _uiLoginState.update { it.copy(userState = UserState.Error(errorMessage)) }
            }
        }
    }

    /**
     * Saves the current access token and refresh token to the [TokenRepository].
     */
    private suspend fun saveToken() {
        val currentSession = SupabaseClient.Client.gotrue.currentSessionOrNull()
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
            val jwt = SupabaseClient.Client.gotrue.currentSessionOrNull()?.accessToken
            if (jwt != null) {
                val user = SupabaseClient.Client.gotrue.retrieveUser(jwt)
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
            _uiLoginState.update { it.copy(userState = UserState.Loading) }
            var logoutMessage = "Logout successfully"
            try {
                SupabaseClient.Client.gotrue.logout()
                tokenRepository.clearTokens()
                dataRepository.clearAllData()
                bluetoothController.closeConnection()
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Logout successfully"),
                        isLoggedIn = false,
                    )
                }
            } catch (e: Exception) {
                // Logowanie błędu, ale nie zatrzymujemy procesu wylogowania
                val errorMessage = e.message ?: "Logout failed"
                Log.e("SupabaseAuthViewModel", "Logout error: $errorMessage")
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Logout offline"),
                        isLoggedIn = false
                    )
                }
            } finally {
                // Zawsze czyścimy tokeny i dane
                tokenRepository.clearTokens()
                dataRepository.clearAllData()
                bluetoothController.closeConnection()
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success(logoutMessage),
                        isLoggedIn = false
                    )
                }
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
                val token = tokenRepository.getAccessToken()
                if (token.isNullOrEmpty()) {
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Success("User not logged in!"),
                            isLoggedIn = false
                        )
                    }
                } else {
                    tokenRefresh()
                }
            } catch (e: Exception) {
                val errorMessage = e.message?.substringBefore("URL") ?: "Error occurred"
                _uiLoginState.update {
                    it.copy(userState = UserState.Error(errorMessage), isLoggedIn = false)
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
    private fun observeDisplayName() {
        viewModelScope.launch {
            tokenRepository.getDisplayName().collect { displayName ->
                Log.d("YourViewModel", "Retrieved displayName from DataStore: $displayName")
                _uiLoginState.update {
                    it.copy(userName = displayName ?: "Unknown")
                }
                Log.d(
                    "YourViewModel",
                    "Retrieved displayName from _uiLoginState: ${_uiLoginState.value.userName}"
                )
            }
        }
    }

    /**
     * Refreshes the session if it has expired.
     * Updates the UI state accordingly.
     */
    private suspend fun tokenRefresh() {
        try {
            if (tokenRepository.isSessionExpired()) {
                Log.d("SupabaseAuthViewModel", "Session expired. Refreshing session.")
                SupabaseClient.Client.gotrue.refreshCurrentSession()
                saveToken()
                saveName()
                _uiLoginState.update {
                    it.copy(
                        userState = UserState.Success("Session refreshed successfully"),
                        isLoggedIn = true
                    )
                }
            } else {
                Log.d("SupabaseAuthViewModel", "Session is still valid.")
                try {
                    SupabaseClient.Client.gotrue.retrieveUser(tokenRepository.getAccessToken()!!)
                    saveName()
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Success("User already logged in!"),
                            isLoggedIn = true
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseAuthViewModel", "Failed to retrieve user: ${e.message}")
                    _uiLoginState.update {
                        it.copy(
                            userState = UserState.Error("Failed to retrieve user: ${e.message}"),
                            isLoggedIn = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            val errorMessage = e.message?.substringBefore("URL") ?: "Failed to refresh session"
            Log.e("SupabaseAuthViewModel", errorMessage)
            _uiLoginState.update {
                it.copy(
                    userState = UserState.Error(errorMessage),
                    isLoggedIn = false
                )
            }
        }
    }

//    // Holds the UI state for login and sign-up screens.
//    private val _uiLoginState = MutableStateFlow(LoginUiState())
//    val uiLoginState: StateFlow<LoginUiState> = _uiLoginState.asStateFlow()
//
//
//    init {
//        isUserLoggedIn()
//        observeDisplayName()
//        resetUserState()
//    }
//
//    /**
//     * Updates the user's email in the UI state.
//     *
//     * @param newEmail The new email entered by the user.
//     */
//    fun onEmailChange(newEmail: String) {
//        _uiLoginState.update { it.copy(userEmail = newEmail) }
//    }
//
//    /**
//     * Updates the user's password in the UI state.
//     *
//     * @param newPassword The new password entered by the user.
//     */
//    fun onPasswordChange(newPassword: String) {
//        _uiLoginState.update { it.copy(userPassword = newPassword) }
//    }
//
//    fun onNameChange(newName: String) {
//        _uiLoginState.update { it.copy(userName = newName) }
//    }
//
//    /**
//     * Handles user login by calling Supabase API and saves the access token.
//     * Updates the UI state based on the result of the login attempt.
//     */
//    fun login() {
//        val userEmail = _uiLoginState.value.userEmail
//        val userPassword = _uiLoginState.value.userPassword
//
//        viewModelScope.launch {
//            _uiLoginState.update { it.copy(userState = UserState.Loading) }
//            try {
//                SupabaseClient.Client.gotrue.loginWith(Email) {
//                    email = userEmail
//                    password = userPassword
//                }
//                saveToken()
//                saveName()
//                _uiLoginState.update {
//                    it.copy(
//                        userState = UserState.Success("Logged successfully"),
//                        isLoggedIn = true
//                    )
//                }
//            } catch (e: Exception) {
//                val errorMessage = e.message?.substringBefore("URL") ?: ""
//                _uiLoginState.update {
//                    it.copy(
//                        userState = UserState.Error(errorMessage),
//                        isLoggedIn = false
//                    )
//                }
//            }
//        }
//    }
//
//    /**
//     * Handles user sign-up by calling Supabase API and saves the access token.
//     * Updates the UI state based on the result of the sign-up attempt.
//     */
//    fun signUp() {
//        val userName = _uiLoginState.value.userName
//        val userEmail = _uiLoginState.value.userEmail
//        val userPassword = _uiLoginState.value.userPassword
//
//        viewModelScope.launch {
//            _uiLoginState.update { it.copy(userState = UserState.Loading) }
//            try {
//                val userData = buildJsonObject {
//                    put("display_name", userName)
//                }
//                // Attempt to log in using Supabase GoTrue API
//                SupabaseClient.Client.gotrue.signUpWith(Email) {
//                    email = userEmail
//                    password = userPassword
//                    data = userData
//                }
//                // Save the access token upon successful signup
//                //saveToken()
//                _uiLoginState.update { it.copy(userState = UserState.Success("Registered successfully")) }
//            } catch (e: Exception) {
//                // Handle login error and update UI state with error message
//                val errorMessage = e.message?.substringBefore("URL") ?: ""
//                _uiLoginState.update { it.copy(userState = UserState.Error(errorMessage)) }
//            }
//        }
//    }
//
//    /**
//     * Saves the current access token to the [TokenRepository].
//     */
////    private fun saveToken() {
////        viewModelScope.launch {
////            // Pobierz token dostępu, jeśli jest dostępny
////            val accessToken: String? = SupabaseClient.Client.gotrue.currentAccessTokenOrNull()
////            // Jeśli accessToken nie jest null, zapisz go w TokenRepository
////            accessToken?.let { token ->
////                tokenRepository.saveAccessToken(token)
////            }
////        }
////    }
//
//    /**
//     * Saves the current access token and refresh token to the [TokenRepository].
//     */
//    private suspend fun saveToken() {
//        // Pobierz sesję
//        val currentSession = SupabaseClient.Client.gotrue.currentSessionOrNull()
//        currentSession?.let { session ->
//            tokenRepository.saveAccessToken(session.accessToken)
//            tokenRepository.saveRefreshToken(session.refreshToken)
//        }
//    }
//
//
//        private fun saveName() {
//            viewModelScope.launch {
//                try {
//
//                    val jwt = SupabaseClient.Client.gotrue.currentSessionOrNull()?.accessToken
//                    if (jwt != null) {
//                        val user = SupabaseClient.Client.gotrue.retrieveUser(jwt)
//                        user?.userMetadata?.let { metadata ->
//                            for ((key, value) in metadata) {
//                            }
//
//                            val displayName = (metadata["display_name"])?.jsonPrimitive?.content
//                            if (displayName != null) {
//
//                                tokenRepository.saveUserName(displayName)
//                                Log.d(
//                                    "YourViewModel",
//                                    "Retrieved displayName from uiLoginState: ${uiLoginState.value.userName}"
//                                )
//                                Log.d(
//                                    "YourViewModel",
//                                    "Retrieved displayName from DataStore: $displayName"
//                                )
//                                Log.d("display_name", displayName)
//
//                            } else {
//                                Log.e(
//                                    "UserMetadata",
//                                    "`display_name` is not available or not a String in userMetadata."
//                                )
//                            }
//                        } ?: run {
//                            Log.e("UserMetadata", "userMetadata is null.")
//                        }
//                    } else {
//                        // Obsłuż przypadek, gdy token jest null, np. użytkownik nie jest zalogowany
//                        Log.e("Error", "User is not authenticated. JWT token is null.")
//                    }
//                } catch (e: Exception) {
//                    Log.e("TokenRepository", "Error retrieving user name", e)
//                }
//            }
//        }
//
//        /**
//         * Logs out the user by calling Supabase API and clears the access token.
//         * Updates the UI state based on the result of the logout attempt.
//         */
//        fun logout() {
//            viewModelScope.launch {
//                _uiLoginState.update { it.copy(userState = UserState.Loading) }
//                try {
//                    SupabaseClient.Client.gotrue.logout()
//                    // tokenRepository.clearAccessToken()
//                    tokenRepository.clearTokens()
//                    dataRepository.clearAllData()
//                    _uiLoginState.update {
//                        it.copy(
//                            userState = UserState.Success("Logout sucessfully"),
//                            isLoggedIn = false
//                        )
//                    }
//                } catch (e: Exception) {
//                    val errorMessage = e.message ?: ""
//                    _uiLoginState.update { it.copy(userState = UserState.Error(errorMessage)) }
//                }
//            }
//        }
//
//
//        /**
//         * Checks if the user is already logged in by retrieving the access token from the repository.
//         * Updates the UI state accordingly.
//         */
//        fun isUserLoggedIn() {
//            viewModelScope.launch {
//                try {
//                    _uiLoginState.update { it.copy(userState = UserState.Loading) }
//                    // Pobiera pierwszą wartość z accessTokenFlow
//                   // val token = tokenRepository.accessTokenFlow.firstOrNull()
//                    val token = tokenRepository.getAccessToken()
//                    if (token.isNullOrEmpty()) {
//                        _uiLoginState.update {
//                            it.copy(
//                                userState = UserState.Success("User not logged in!"),
//                                isLoggedIn = false
//                            )
//                        }
//                    } else {
//                        tokenRefresh()
//                    }
//                } catch (e: Exception) {
//                    val errorMessage = e.message?.substringBefore("URL") ?: "Error occurred"
//                    _uiLoginState.update {
//                        it.copy(userState = UserState.Success(errorMessage), isLoggedIn = false)
//                    }
//                }
//            }
//        }
//
//        /**
//         * Resets the [userState] in the UI state to [UserState.Idle].
//         * This is useful to prevent repeated navigation or UI updates based on previous state.
//         */
//        fun resetUserState() {
//            _uiLoginState.update { it.copy(userState = UserState.Idle) }  // Ustaw stan na wartość domyślną, np. Idle
//        }
//
//
//        private fun observeDisplayName() {
//            viewModelScope.launch {
//                tokenRepository.getDisplayName().collect { displayName ->
//                    Log.d("YourViewModel", "Retrieved displayName from DataStore: $displayName")
//                    _uiLoginState.update {
//                        it.copy(userName = displayName ?: "Unknown") // Ustaw wartość lub domyślną
//                    }
//                    Log.d(
//                        "YourViewModel",
//                        "Retrieved displayName from _uiLoginState: ${_uiLoginState.value.userName}"
//                    )
//                }
//            }
//        }
//
//    /**
//     * Odświeża sesję, jeśli jest przeterminowana.
//     * Aktualizuje stan UI odpowiednio.
//     */
//    private suspend fun tokenRefresh() {
//        try {
//            // Sprawdź, czy sesja jest przeterminowana
//            if (tokenRepository.isSessionExpired()) {
//                Log.d("SupabaseAuthViewModel", "Session expired. Refreshing session.")
//                // Odśwież sesję
//                SupabaseClient.Client.gotrue.refreshCurrentSession()
//                saveToken() // Zapisz nowy token
//                saveName() // Zapisz nazwę użytkownika
//                _uiLoginState.update {
//                    it.copy(
//                        userState = UserState.Success("Session refreshed successfully"),
//                        isLoggedIn = true
//                    )
//                }
//            } else {
//                Log.d("SupabaseAuthViewModel", "Session is still valid.")
//                try {
//                    // Sesja jest ważna, potwierdź, że użytkownik jest zalogowany
//                    SupabaseClient.Client.gotrue.retrieveUser(tokenRepository.getAccessToken()!!)
//                    saveName() // Zapisz nazwę użytkownika
//                    _uiLoginState.update {
//                        it.copy(
//                            userState = UserState.Success("User already logged in!"),
//                            isLoggedIn = true
//                        )
//                    }
//                } catch (e: Exception) {
//                    // Nie udało się pobrać danych użytkownika
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
//            // Obsłuż błędy podczas odświeżania sesji
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
}





