package com.example.activityrecognitionapp.domain.repository

import com.example.activityrecognitionapp.data.model.Session
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow

interface SupabaseAuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signUp(name: String, email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun refreshSession(): Result<Unit>

    suspend fun isSessionExpired(): Boolean
    suspend fun getCurrentUser(): UserInfo?
    suspend fun saveSession(session: Session)
    suspend fun saveUserName(name: String)

    fun getDisplayName(): Flow<String?>
}