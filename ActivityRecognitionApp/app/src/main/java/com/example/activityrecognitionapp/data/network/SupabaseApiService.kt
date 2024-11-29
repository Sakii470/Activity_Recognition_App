package com.example.activityrecognitionapp.data.network

import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCount
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service interface for interacting with Supabase's REST API.
 * Defines endpoints for inserting and retrieving activity data.
 */
interface SupabaseApiService {

    /**
     * Inserts a new activity record into the Supabase `activity_table`.
     *
     * @param authorization The authorization header in the format "Bearer {token}".
     * @param apiKey The Supabase API key.
     * @param data The [ActivityDataSupabase] object containing activity details.
     * @return A [Response] indicating the success or failure of the insertion.
     */
    @POST("rest/v1/activity_table")
    suspend fun insertActivityData(
        @Header("Authorization") authorization: String,
        @Header("apikey") apiKey: String,
        @Body data: ActivityDataSupabase
    ): Response<Void>

    /**
     * Retrieves user activities for the current month from the Supabase `activity_counts_per_day` view.
     *
     * @param apiKey The Supabase API key.
     * @param authorization The authorization header in the format "Bearer {token}".
     * @param userIdFilter The filter for the user ID, formatted as "eq.{userId}".
     * @param timestampGte The start date filter, formatted as "gte.{startOfMonth}".
     * @param timestampLte The end date filter, formatted as "lte.{endOfMonth}".
     * @param select Specifies the columns to retrieve, defaulting to "user_id,timestamp,activity_type,count".
     * @return A [Response] containing a list of [ActivityCount] objects or an error response.
     */
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
