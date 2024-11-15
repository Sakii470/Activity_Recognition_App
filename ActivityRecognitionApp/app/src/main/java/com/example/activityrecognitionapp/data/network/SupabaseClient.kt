package com.example.activityrecognitionapp.data.network

import com.example.activityrecognitionapp.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue

object SupabaseClient {

    // Supabase GoTrue Client - dla autoryzacji
    val Client = createSupabaseClient(
        supabaseUrl = BuildConfig.supabaseUrl,
        supabaseKey = BuildConfig.supabaseKey
    ) {
        install(GoTrue)
    }
}