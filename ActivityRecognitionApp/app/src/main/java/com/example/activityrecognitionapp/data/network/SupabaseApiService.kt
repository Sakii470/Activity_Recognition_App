package com.example.activityrecognitionapp.data.network

import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCount
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query


interface SupabaseApiService {

        @POST("rest/v1/activity_table")
        suspend fun insertActivityData(
            @Header("Authorization") authorization: String,
            @Header("apikey") apiKey: String,
            @Body data: ActivityDataSupabase
        ): Response<Void>

        @GET("rest/v1/activity_counts_per_day")
        suspend fun getUserActivitiesForMonth(
            @Header("apikey") apiKey: String,
            @Header("Authorization") authorization: String,
            @Query("user_id") userIdFilter: String,
            @Query("timestamp") timestampGte: String, // Start Date
            @Query("timestamp") timestampLte: String, // End Date
            @Query("select") select: String = "user_id,timestamp,activity_type,count"
        ): Response<List<ActivityCount>>
    }
