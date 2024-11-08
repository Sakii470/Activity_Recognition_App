package com.example.activityrecognitionapp.data.network

import com.example.activityrecognitionapp.data.model.ActivityCount
import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {
    @POST("rest/v1/activity_table")
    fun insertActivityData(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Body data: ActivityDataSupabase
    ): Call<Void>

    @GET("rest/v1/activity_table")
    fun getUserActivities(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Query("user_id") userId: String,
        @Query("select") select: String = "activity_type, count:activity_type"
    ): Call<List<ActivityCount>>
}
