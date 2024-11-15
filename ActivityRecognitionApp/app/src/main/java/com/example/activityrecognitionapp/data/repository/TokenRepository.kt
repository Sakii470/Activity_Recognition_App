package com.example.activityrecognitionapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import com.example.activityrecognitionapp.data.network.SupabaseClient
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.gotrue.gotrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    //private val supabaseApiClient: SupabaseApiClient
    //private val supabaseAuthViewModel: SupabaseAuthViewModel
) : ViewModel() {
    // DataStore instance for managing preferences, using the "user_prefs" storage name
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")
    // Companion object containing keys used in DataStore for storing preferences
    companion object {
        // Key for storing and accessing the access token in DataStore
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")

        val USER_NAME_KEY = stringPreferencesKey("user_name")
    }


    // Flow to retrieve the name, providing live updates when the token changes
    val userNameFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME_KEY]
        }

//    suspend fun saveUserName(name: String){
//        context.dataStore.edit{prefrences ->
//            prefrences[USER_NAME_KEY] = name
//        }
//    }


    // Flow to retrieve the access token, providing live updates when the token changes
    val accessTokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }

    /**
     * Saves the access token to DataStore.
     * @param token The access token string to save.
     */
    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
        }
    }

    /**
     * Clears the access token from DataStore.
     */
    suspend fun clearAccessToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(USER_NAME_KEY)
        }
    }

     fun getUserId(): String? {
        return SupabaseClient.Client.gotrue.currentSessionOrNull()?.user?.id
    }
}