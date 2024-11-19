package com.example.activityrecognitionapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.activityrecognitionapp.BuildConfig
import com.example.activityrecognitionapp.data.model.ActivityDataEntity
import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.data.network.SupabaseApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val activityDao: ActivityDataDao,
    private val tokenRepository: TokenRepository,
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    suspend fun saveActivityDataLocally(data: ActivityDataSupabase) {
        val activityEntity = ActivityDataEntity(
            user_id = data.user_id ?: "",
            activity_type = data.activity_type,
            timestamp = data.timestamp,
            count = 1,
            isSynced = false, // Mark as not synced initially
            //lastModified = System.currentTimeMillis()
        )
        activityDao.insertActivity(activityEntity)
        Log.d("DataRepository", "Inserted activity into SQLite with $activityEntity")
    }

    // Function to send data to the server
    suspend fun sendActivityDataToServer(activity: ActivityDataEntity): Boolean {
        val data = ActivityDataSupabase(
            user_id = activity.user_id,
            activity_type = activity.activity_type,
            timestamp = activity.timestamp,
        )
        return try {
            val accessToken = tokenRepository.getAccessToken()
            if (accessToken == null) {
                Log.e("DataRepository", "Access token is null. Cannot send activity data.")
                return false
            }
            val response = supabaseApiService.insertActivityData(
                authorization = "Bearer ${BuildConfig.supabaseKey}",
                apiKey = BuildConfig.supabaseKey,
                data = data
            )
            if (response.isSuccessful) {
                // Update isSynced to true
                activityDao.updateSyncStatus(
                    activity.user_id,
                    activity.activity_type,
                    activity.timestamp,
                    true
                )
                Log.d(
                    "DataRepository",
                    "Activity data synced with server: ${activity.user_id}, ${activity.activity_type}, ${activity.timestamp}"
                )
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(
                    "DataRepository",
                    "Error syncing data: ${response.code()} - ${response.message()} - $errorBody"
                )
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DataRepository", "Error sending activity data", e)
            false
        }
    }

    // Function to sync all unsynced activities
    suspend fun syncLocalChanges() {
        val unsyncedActivities = activityDao.getUnsyncedActivities()
        for (activity in unsyncedActivities) {
            val success = sendActivityDataToServer(activity)
            if (success) {
                Log.d("DataRepository", "Synced activity: ")
            } else {
                Log.e("DataRepository", "Failed to sync activity: ")
            }
        }
    }

    suspend fun getUserActivitiesForMonth(userId: String): List<ActivityDataEntity>? {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val startOfMonth = String.format("%04d-%02d-01T00:00:00", year, month)
        val endOfMonth = String.format(
            "%04d-%02d-%02dT23:59:59",
            year,
            month,
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        )

        val userIdFilter = "eq.$userId"
        val timestampGte = "gte.$startOfMonth"
        val timestampLte = "lte.$endOfMonth"

        return try {
            val accessToken = tokenRepository.getAccessToken()
            if (accessToken == null) {
                Log.e("DataRepository", "Access token is null. Cannot fetch activity data.")
                return null
            }
            val response = supabaseApiService.getUserActivitiesForMonth(
                apiKey = BuildConfig.supabaseKey, // Bez "Bearer "
                authorization = "Bearer ${BuildConfig.supabaseKey}",
                userIdFilter = userIdFilter,
                timestampGte = timestampGte,
                timestampLte = timestampLte
            )
            if (response.isSuccessful) {
                val activityCounts = response.body() ?: emptyList()
                Log.d("Supabase", "Data fetched successfully: $activityCounts")

                val activityDataEntities = activityCounts.map { activityCount ->
                    ActivityDataEntity(
                        user_id = activityCount.user_id ?: "",
                        activity_type = activityCount.activity_type ?: "",
                        timestamp = activityCount.timestamp ?: "",
                        count = activityCount.count,
                        isSynced = true
                        // lastModified = parseISO8601ToMillis(activityCount.timestamp)
                    )
                }

                activityDao.insertActivities(activityDataEntities)
                Log.d("DataRepository", "Inserted activities into SQLite: $activityDataEntities")

                val allActivities = activityDao.getAllActivities()
                Log.d("DataRepository", "Fetched activities from SQLite: $allActivities")
                allActivities

            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(
                    "Supabase",
                    "Error fetching data: ${response.code()} - ${response.message()} - $errorBody"
                )
                activityDao.getAllActivities()
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Connection error: ${e.message}")
            activityDao.getAllActivities()
        }
    }


    suspend fun getAllActivitiesFromDatabase(): List<ActivityDataEntity> {
        val data = activityDao.getAllActivities()
        Log.d("DataRepository", "Fetched activities: $data")
        return data
    }

    // Function to clear all data from SQLite database
    suspend fun clearAllData() {
        activityDao.deleteAllActivities()
        Log.d("DataRepository", "All local data cleared from SQLite database.")
    }

    suspend fun insertActivity(activity: ActivityDataEntity) {
        activityDao.insertActivity(activity)
        Log.d("DataRepository", "Inserted activity: $activity")
    }

    suspend fun refreshActivitiesFromServer(userId: String) {
        // Możesz tutaj zaimplementować logikę odświeżania danych, jeśli potrzebujesz
        getUserActivitiesForMonth(userId)
    }

    // Expose the flow from the DAO
    fun getAllActivitiesFlow(): Flow<List<ActivityDataEntity>> {
        return activityDao.getAllActivitiesFlow()
    }



}

