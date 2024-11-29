package com.example.activityrecognitionapp.data.repository

import android.util.Log
import com.example.activityrecognitionapp.data.model.Session
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

/**
 * Implementation of [SupabaseAuthRepository] handling authentication operations
 * using Supabase's GoTrue client. Manages user sessions, login, sign-up, logout,
 * and session refreshing. Also interacts with [TokenRepository], [DataRepository],
 * and [BluetoothRepository] to maintain application state.
 */
class SupabaseAuthRepositoryImpl @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val dataRepository: DataRepository,
    private val bluetoothRepository: BluetoothRepository,

) : SupabaseAuthRepository {

    // Instance of Supabase GoTrue client for authentication operations
    private val supabaseClient = SupabaseApiClient.SupabaseClient.Client

    /**
     * Logs in a user with the provided email and password.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return A [Result] indicating success or failure of the login operation.
     */
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            Log.d("SupabaseAuthRepositoryImpl", "Attempting to login user: $email")
            // Initiates login with email and password using Supabase GoTrue
            supabaseClient.gotrue.loginWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d("SupabaseAuthRepositoryImpl", "Login successful for user: $email")
            // Saves session details and user name after successful login
            saveSession()
            saveUserNameFromMetadata()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuthRepositoryImpl", "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Registers a new user with the provided name, email, and password.
     *
     * @param name The user's display name.
     * @param email The user's email address.
     * @param password The user's password.
     * @return A [Result] indicating success or failure of the sign-up operation.
     */
    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        return try {
            Log.d("SupabaseAuthRepository", "Attempting to sign up user: $email with name: $name")
            // Builds user metadata with the display name
            val userData = buildJsonObject {
                put("display_name", name)
            }
            // Initiates sign-up with email, password, and metadata
            supabaseClient.gotrue.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = userData
            }
            Log.d("SupabaseAuthRepository", "Sign up successful for user: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuthRepository", "SignUp error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Logs out the currently authenticated user.
     *
     * @return A [Result] indicating success or failure of the logout operation.
     */
    override suspend fun logout(): Result<Unit> {
        return try {
            Log.d("SupabaseAuthRepository", "Attempting to logout user")
            // Initiates logout using Supabase GoTrue
            supabaseClient.gotrue.logout()
            // Clears tokens and application data post-logout
            tokenRepository.clearTokens()
            dataRepository.clearAllData()
            bluetoothRepository.disconnect()
            Log.d("SupabaseAuthRepository", "Logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuthRepository", "Logout error: ${e.message}", e)
            // Ensures cleanup even if logout fails
            tokenRepository.clearTokens()
            Result.failure(e)
        } finally {
            // Ensures Bluetooth is disconnected and tokens are cleared regardless of outcome
            bluetoothRepository.disconnect()
            tokenRepository.clearTokens()
            dataRepository.clearAllData()
        }
    }

    /**
     * Refreshes the current user session.
     *
     * @return A [Result] indicating success or failure of the session refresh operation.
     */
    override suspend fun refreshSession(): Result<Unit> {
        return try {
            Log.d("SupabaseAuthRepository", "Attempting to refresh session")
            // Refreshes the session using Supabase GoTrue
            supabaseClient.gotrue.refreshCurrentSession()
            // Updates session details and user name after refresh
            saveSession()
            saveUserNameFromMetadata()
            Log.d("SupabaseAuthRepository", "Session refresh successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuthRepository", "Refresh session error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if the current user session has expired.
     *
     * @return `true` if the session has expired, `false` otherwise.
     */
    override suspend fun isSessionExpired(): Boolean {
        return tokenRepository.isSessionExpired()
    }

    /**
     * Retrieves information about the currently authenticated user.
     *
     * @return A [UserInfo] object containing user details or `null` if no user is authenticated.
     */
    override suspend fun getCurrentUser(): UserInfo? {
        val session = supabaseClient.gotrue.currentSessionOrNull()
        return session?.let {
            supabaseClient.gotrue.retrieveUser(session.accessToken)
        }
    }

    /**
     * Saves the provided [Session] details to the [TokenRepository].
     *
     * @param session The session data to be saved.
     */
    override suspend fun saveSession(session: Session) {
        tokenRepository.saveSession(session)
    }

    /**
     * Saves the user's display name to the [TokenRepository].
     *
     * @param name The display name to be saved.
     */
    override suspend fun saveUserName(name: String) {
        tokenRepository.saveUserName(name)
    }

    /**
     * Retrieves the user's display name as a [Flow].
     *
     * @return A [Flow] emitting the user's display name or `null`.
     */
    override fun getDisplayName(): Flow<String?> {
        return tokenRepository.getDisplayName()
    }

    /**
     * Saves the current session details from Supabase to the [TokenRepository].
     */
    private suspend fun saveSession() {
        val currentSession = supabaseClient.gotrue.currentSessionOrNull()
        currentSession?.let { session ->
            val expiresAt = session.expiresAt
            val newSession = Session(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                expiresAt = expiresAt
            )
            saveSession(newSession)
        }
    }

    /**
     * Extracts and saves the user's display name from their metadata.
     */
    private suspend fun saveUserNameFromMetadata() {
        try {
            val user = getCurrentUser()
            user?.userMetadata?.let { metadata ->
                val displayName = metadata["display_name"]?.jsonPrimitive?.content
                if (displayName != null) {
                    saveUserName(displayName)
                } else {
                    Log.e("SupabaseAuthRepository", "`display_name` not found in userMetadata.")
                }
            } ?: run {
                Log.e("SupabaseAuthRepository", "User metadata is null.")
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuthRepository", "Error retrieving user name", e)
        }
    }
}
