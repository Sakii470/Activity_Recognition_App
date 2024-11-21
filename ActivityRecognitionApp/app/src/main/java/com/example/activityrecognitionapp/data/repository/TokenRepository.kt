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

@Singleton
class TokenRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    // DataStore instance for managing preferences, using the "user_prefs" storage name
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

    // Klucze używane w DataStore
    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val EXPIRES_AT_KEY = stringPreferencesKey("expires_at")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    // Funkcje do zarządzania nazwą użytkownika
    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    fun getDisplayName(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME_KEY]
        }
    }

    // Flow do pobierania tokenu dostępu z aktualizacjami
    val accessTokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }

    // Czyszczenie tokenu dostępu i nazwy użytkownika
    suspend fun clearAccessToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(USER_NAME_KEY)
        }
    }

    // Pobieranie identyfikatora użytkownika
    fun getUserId(): String? {
        return SupabaseApiClient.SupabaseClient.Client.gotrue.currentSessionOrNull()?.user?.id
    }

    // Funkcje do zarządzania sesją
    suspend fun saveSession(session: Session) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = session.accessToken
            preferences[REFRESH_TOKEN_KEY] = session.refreshToken
            preferences[EXPIRES_AT_KEY] = session.expiresAt.toString()
        }
    }

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

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Funkcje do zarządzania tokenami
    suspend fun saveAccessToken(token: String) {
        val session = getSession()?.copy(accessToken = token) ?: Session(
            accessToken = token,
            refreshToken = "",
            expiresAt = Clock.System.now()
        )
        saveSession(session)
    }

    suspend fun saveRefreshToken(token: String) {
        val session = getSession()?.copy(refreshToken = token) ?: Session(
            accessToken = "",
            refreshToken = token,
            expiresAt = Clock.System.now()
        )
        saveSession(session)
    }

    suspend fun getAccessToken(): String? {
        return getSession()?.accessToken
    }

    suspend fun getRefreshToken(): String? {
        return getSession()?.refreshToken
    }

    suspend fun clearTokens() {
        clearSession()
    }

    // Sprawdzenie, czy sesja wygasła
    fun isSessionExpired(): Boolean {
        val session = runBlocking { getSession() }
        val isExpired = session?.expiresAt?.let { it <= Clock.System.now() } ?: true
        Log.d("TokenRepository", "isSessionExpired: $isExpired")
        return isExpired
    }
}


//    // Companion object containing keys used in DataStore for storing preferences
//    companion object {
//        // Key for storing and accessing the access token in DataStore
//        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
//        val USER_NAME_KEY = stringPreferencesKey("user_name")
//    }
//
//
//
//
//    suspend fun saveUserName(name: String) {
//        context.dataStore.edit { prefrences ->
//            prefrences[USER_NAME_KEY] = name
//        }
//    }
//
//    fun getDisplayName(): Flow<String?> {
//        return context.dataStore.data.map { preferences ->
//            preferences[USER_NAME_KEY]
//        }
//    }
//
//
//    // Flow to retrieve the access token, providing live updates when the token changes
//    val accessTokenFlow: Flow<String?> = context.dataStore.data
//        .map { preferences ->
//            preferences[ACCESS_TOKEN_KEY]
//        }
//
//    /**
//     * Saves the access token to DataStore.
//     * @param token The access token string to save.
//     */
////    suspend fun saveAccessToken(token: String) {
////        context.dataStore.edit { preferences ->
////            preferences[ACCESS_TOKEN_KEY] = token
////        }
////    }
//
//    /**
//     * Clears the access token from DataStore.
//     */
//    suspend fun clearAccessToken() {
//        context.dataStore.edit { preferences ->
//            preferences.remove(ACCESS_TOKEN_KEY)
//            preferences.remove(USER_NAME_KEY)
//        }
//    }
//
//    fun getUserId(): String? {
//        return SupabaseClient.Client.gotrue.currentSessionOrNull()?.user?.id
//    }
//
//
//
//    suspend fun saveSession(session: Session) {
//        context.dataStore.edit { preferences ->
//            preferences[stringPreferencesKey("access_token")] = session.accessToken
//            preferences[stringPreferencesKey("refresh_token")] = session.refreshToken
//            preferences[stringPreferencesKey("expires_at")] = session.expiresAt.toString()
//        }
//    }
//
//     suspend fun getSession(): Session? {
//        val preferences = context.dataStore.data.first()
//        val accessToken = preferences[stringPreferencesKey("access_token")]
//        val refreshToken = preferences[stringPreferencesKey("refresh_token")]
//        val expiresAtString = preferences[stringPreferencesKey("expires_at")]
//        val expiresAt = expiresAtString?.let { Instant.parse(it) }
//
//        return if (accessToken != null && refreshToken != null && expiresAt != null) {
//            Session(
//                accessToken = accessToken,
//                refreshToken = refreshToken,
//                expiresAt = expiresAt
//            )
//        } else {
//            null
//        }
//    }
//
//     suspend fun clearSession() {
//        context.dataStore.edit { preferences ->
//            preferences.clear()
//        }
//    }
////     suspend fun getRefreshToken(): String? {
////        return getSession()?.refreshToken
////    }
////
////     suspend fun clearTokens() {
////        clearSession()
////    }
////
////    fun isSessionExpired(): Boolean {
////        val session = runBlocking {getSession() }
////        val isExpired = session?.expiresAt?.let { it <= Clock.System.now() } ?: true
////        Log.d("SupabaseAuthViewModel", "isSessionExpired: $isExpired")
////        return isExpired
////    }
//
//
//
//     suspend fun saveAccessToken(token: String) {
//        val session = sessionStorage.getSession()?.copy(accessToken = token) ?: Session(accessToken = token, refreshToken = "", expiresAt = Clock.System.now())
//        sessionStorage.saveSession(session)
//    }
//
//     suspend fun saveRefreshToken(token: String) {
//        val session = sessionStorage.getSession()?.copy(refreshToken = token) ?: Session(accessToken = "", refreshToken = token, expiresAt = Clock.System.now())
//        sessionStorage.saveSession(session)
//    }
//
//     suspend fun getAccessToken(): String? {
//        return sessionStorage.getSession()?.accessToken
//    }
//
//     suspend fun getRefreshToken(): String? {
//        return sessionStorage.getSession()?.refreshToken
//    }
//
//     suspend fun clearTokens() {
//        sessionStorage.clearSession()
//    }
//
//   fun isSessionExpired(): Boolean {
//        val session = runBlocking { sessionStorage.getSession() }
//        return session?.expiresAt?.let { it <= Clock.System.now() } ?: true
//    }




