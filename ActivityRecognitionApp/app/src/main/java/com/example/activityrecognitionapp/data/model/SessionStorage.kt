package com.example.activityrecognitionapp.data.model

import kotlinx.datetime.Instant

interface SessionStorage {
    suspend fun saveSession(session: Session)
    suspend fun getSession(): Session?
    suspend fun clearSession()
}

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant
)