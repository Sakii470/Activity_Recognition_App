package com.example.activityrecognitionapp.data.network

import com.example.activityrecognitionapp.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SupabaseApiClient {



    object SupabaseClient {


        val Client = createSupabaseClient(
            supabaseUrl = BuildConfig.supabaseUrl,
            supabaseKey = BuildConfig.supabaseKey
        ) {
            install(GoTrue)
        }
    }

        // Retrofit Client - dla zapytań HTTP
        val apiService: SupabaseApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BuildConfig.supabaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseApiService::class.java)
        }
    }
