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


class SupabaseAuthRepositoryImpl @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val dataRepository: DataRepository,
    private val bluetoothRepository: BluetoothRepository
) : SupabaseAuthRepository {

    private val supabaseClient = SupabaseApiClient.SupabaseClient.Client

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            Log.d("SupabaseAuthRepositoryImpl", "Attempting to login user: $email")
            supabaseClient.gotrue.loginWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d("SupabaseAuthRepositoryImpl", "Login successful for user: $email")
            saveSession()
            saveUserNameFromMetadata()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuthRepositoryImpl", "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        return try {
            Log.d("SupabaseAuthRepository", "Attempting to sign up user: $email with name: $name")
            val userData = buildJsonObject {
                put("display_name", name)
            }
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

    override suspend fun logout(): Result<Unit> {
        return try {
            Log.d("SupabaseAuthRepository", "Attempting to logout user")
            supabaseClient.gotrue.logout()
            tokenRepository.clearTokens()
            dataRepository.clearAllData()
            bluetoothRepository.disconnect()
            Log.d("SupabaseAuthRepository", "Logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuthRepository", "Logout error: ${e.message}", e)
            tokenRepository.clearTokens()
            Result.failure(e)
        }finally {
            bluetoothRepository.disconnect()
            tokenRepository.clearTokens()
            dataRepository.clearAllData()
        }
    }

    override suspend fun refreshSession(): Result<Unit> {
        return try {
            Log.d("SupabaseAuthRepository", "Attempting to refresh session")
            supabaseClient.gotrue.refreshCurrentSession()
            saveSession()
            saveUserNameFromMetadata()
            Log.d("SupabaseAuthRepository", "Session refresh successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuthRepository", "Refresh session error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun isSessionExpired(): Boolean {
        return tokenRepository.isSessionExpired()
    }

    override suspend fun getCurrentUser(): UserInfo? {
        val session = supabaseClient.gotrue.currentSessionOrNull()
        return session?.let {
            supabaseClient.gotrue.retrieveUser(session.accessToken)
        }
    }

    override suspend fun saveSession(session: Session) {
        tokenRepository.saveSession(session)
    }

    override suspend fun saveUserName(name: String) {
        tokenRepository.saveUserName(name)
    }

    override fun getDisplayName(): Flow<String?> {
        return tokenRepository.getDisplayName()
    }

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
