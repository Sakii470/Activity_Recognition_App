package com.example.activityrecognitionapp.data.repository

import android.util.Log
import com.example.activityrecognitionapp.BuildConfig
import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.data.network.SupabaseApiService
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCount
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository @Inject constructor(
    private val supabaseApiService: SupabaseApiService
) {

    suspend fun sendActivityData(data: ActivityDataSupabase): Boolean {
        return try {
            val response = supabaseApiService.insertActivityData(
                authorization = "Bearer ${BuildConfig.supabaseKey}",
                apiKey = BuildConfig.supabaseKey, // Bez "Bearer "
                data = data
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DataRepository", "Error sending activity data", e)
            false
        }
    }

    suspend fun getUserActivitiesForMonth(userId: String): List<ActivityCount>? {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        val startOfMonth = String.format("%04d-%02d-01T00:00:00Z", year, month)
        val endOfMonth = String.format(
            "%04d-%02d-%02dT23:59:59Z",
            year,
            month,
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        )

        val userIdFilter = "eq.$userId"
        val timestampGte = "gte.$startOfMonth"
        val timestampLte = "lte.$endOfMonth"

        return try {
            val response = supabaseApiService.getUserActivitiesForMonth(
                apiKey = BuildConfig.supabaseKey, // Bez "Bearer "
                authorization = "Bearer ${BuildConfig.supabaseKey}",
                userIdFilter = userIdFilter,
                timestampGte = timestampGte,
                timestampLte = timestampLte
            )
            if (response.isSuccessful) {
                Log.d("Supabase", "Data fetched successfully: ${response.body()}")
                response.body()
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("Supabase", "Error fetching data: ${response.code()} - ${response.message()} - $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Connection error: ${e.message}")
            null
        }
    }
}
