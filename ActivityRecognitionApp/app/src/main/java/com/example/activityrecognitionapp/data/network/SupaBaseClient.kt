package com.example.activityrecognitionapp.data.network



import com.example.activityrecognitionapp.BuildConfig

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue

object SupaBaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.supabaseUrl,
        supabaseKey = BuildConfig.supabaseKey
    ) {
        install(GoTrue)
    }
}