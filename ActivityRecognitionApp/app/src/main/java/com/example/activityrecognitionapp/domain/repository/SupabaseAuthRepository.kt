package com.example.activityrecognitionapp.domain.repository

import com.example.activityrecognitionapp.data.model.Session
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Supabase authentication.
 */
interface SupabaseAuthRepository {

    /** Logs in a user. */
    suspend fun login(email: String, password: String): Result<Unit>

    /** Registers a new user. */
    suspend fun signUp(name: String, email: String, password: String): Result<Unit>

    /** Logs out the current user. */
    suspend fun logout(): Result<Unit>

    /** Refreshes the user session. */
    suspend fun refreshSession(): Result<Unit>

    /** Checks if the session has expired. */
    suspend fun isSessionExpired(): Boolean

    /** Gets the current user's info. */
    suspend fun getCurrentUser(): UserInfo?

    /** Saves session data. */
    suspend fun saveSession(session: Session)

    /** Saves the user's name. */
    suspend fun saveUserName(name: String)

    /** Retrieves the display name as a Flow. */
    fun getDisplayName(): Flow<String?>
}
