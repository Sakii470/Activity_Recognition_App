package com.example.activityrecognitionapp.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import com.example.activityrecognitionapp.data.model.Session
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.gotrue.gotrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing authentication tokens and user session data.
 * Utilizes Android DataStore for persistent storage of tokens and user information.
 */
@Singleton
class TokenRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Extension property to create a DataStore instance for preferences
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

    companion object {
        // Keys for storing data in DataStore
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val EXPIRES_AT_KEY = stringPreferencesKey("expires_at")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    /**
     * Saves the user's display name to DataStore.
     *
     * @param name The display name to be saved.
     */
    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    /**
     * Retrieves the user's display name as a Flow.
     *
     * @return A Flow emitting the user's display name or null if not set.
     */
    fun getDisplayName(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME_KEY]
        }
    }

    /**
     * Flow that emits the current access token.
     *
     * @return A Flow emitting the access token string or null if not available.
     */
    val accessTokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }

    /**
     * Clears the stored access token and user name from DataStore.
     */
    suspend fun clearAccessToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(USER_NAME_KEY)
        }
    }

    /**
     * Retrieves the current user's unique identifier.
     *
     * @return The user ID as a String or null if no user is authenticated.
     */
    fun getUserId(): String? {
        return SupabaseApiClient.SupabaseClient.Client.gotrue.currentSessionOrNull()?.user?.id
    }

    /**
     * Saves the session details, including access and refresh tokens, to DataStore.
     *
     * @param session The Session object containing token information.
     */
    suspend fun saveSession(session: Session) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = session.accessToken
            preferences[REFRESH_TOKEN_KEY] = session.refreshToken
            preferences[EXPIRES_AT_KEY] = session.expiresAt.toString()
        }
    }

    /**
     * Retrieves the current session details from DataStore.
     *
     * @return A Session object with access and refresh tokens, or null if incomplete.
     */
    suspend fun getSession(): Session? {
        val preferences = context.dataStore.data.first()
        val accessToken = preferences[ACCESS_TOKEN_KEY]
        val refreshToken = preferences[REFRESH_TOKEN_KEY]
        val expiresAtString = preferences[EXPIRES_AT_KEY]
        val expiresAt = expiresAtString?.let { Instant.parse(it) }

        return if (accessToken != null && refreshToken != null && expiresAt != null) {
            Session(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = expiresAt
            )
        } else {
            null
        }
    }

    /**
     * Clears all session data from DataStore, effectively logging out the user.
     */
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Updates and saves the access token in the current session.
     *
     * @param token The new access token to be saved.
     */
    suspend fun saveAccessToken(token: String) {
        val session = getSession()?.copy(accessToken = token) ?: Session(
            accessToken = token,
            refreshToken = "",
            expiresAt = Clock.System.now()
        )
        saveSession(session)
    }

    /**
     * Updates and saves the refresh token in the current session.
     *
     * @param token The new refresh token to be saved.
     */
    suspend fun saveRefreshToken(token: String) {
        val session = getSession()?.copy(refreshToken = token) ?: Session(
            accessToken = "",
            refreshToken = token,
            expiresAt = Clock.System.now()
        )
        saveSession(session)
    }

    /**
     * Retrieves the current access token from the session.
     *
     * @return The access token string or null if not available.
     */
    suspend fun getAccessToken(): String? {
        return getSession()?.accessToken
    }

    /**
     * Retrieves the current refresh token from the session.
     *
     * @return The refresh token string or null if not available.
     */
    suspend fun getRefreshToken(): String? {
        return getSession()?.refreshToken
    }

    /**
     * Clears both access and refresh tokens from the session.
     */
    suspend fun clearTokens() {
        clearSession()
    }

    /**
     * Checks whether the current session has expired based on the expiration timestamp.
     *
     * @return True if the session is expired, false otherwise.
     */
    fun isSessionExpired(): Boolean {
        val session = runBlocking { getSession() }
        val isExpired = session?.expiresAt?.let { it <= Clock.System.now() } ?: true
        Log.d("TokenRepository", "isSessionExpired: $isExpired")
        return isExpired
    }
}
