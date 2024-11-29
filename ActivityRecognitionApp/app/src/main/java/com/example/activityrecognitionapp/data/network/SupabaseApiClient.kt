package com.example.activityrecognitionapp.data.network

import com.example.activityrecognitionapp.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object that provides Supabase clients for authentication and API interactions.
 */
object SupabaseApiClient {

    /**
     * Nested object that initializes the Supabase GoTrue client for authentication.
     */
    object SupabaseClient {

        /**
         * Supabase GoTrue client initialized with the project's URL and API key.
         * Enables authentication features by installing the GoTrue plugin.
         */
        val Client = createSupabaseClient(
            supabaseUrl = BuildConfig.supabaseUrl,
            supabaseKey = BuildConfig.supabaseKey
        ) {
            install(GoTrue)
        }
    }

    /**
     * Retrofit service for making HTTP requests to Supabase APIs.
     * Lazily initialized to ensure it's created only when needed.
     */
    val apiService: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.supabaseUrl) // Base URL for Supabase API
            .addConverterFactory(GsonConverterFactory.create()) // JSON converter
            .build()
            .create(SupabaseApiService::class.java) // Creates the API service interface
    }
}
