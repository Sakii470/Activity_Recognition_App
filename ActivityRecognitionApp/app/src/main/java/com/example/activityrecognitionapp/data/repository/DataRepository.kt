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

/**
 * Repository for handling activity data synchronization between local storage and Supabase server.
 * Manages saving, retrieving, and syncing user activities.
 */
@Singleton
class DataRepository @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val activityDao: ActivityDataDao,
    private val tokenRepository: TokenRepository,
    @ApplicationContext private val context: Context
) {
    // SharedPreferences for storing synchronization preferences
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    /**
     * Saves activity data locally in the SQLite database.
     *
     * @param data The [ActivityDataSupabase] object containing activity details.
     */
    suspend fun saveActivityDataLocally(data: ActivityDataSupabase) {
        val activityEntity = ActivityDataEntity(
            user_id = data.user_id ?: "",
            activity_type = data.activity_type,
            timestamp = data.timestamp,
            count = 1,
            isSynced = false // Marks the entry as not yet synced with the server
            //lastModified = System.currentTimeMillis() // Optional: Track last modification time
        )
        activityDao.insertActivity(activityEntity)
        Log.d("DataRepository", "Inserted activity into SQLite: $activityEntity")
    }

    /**
     * Sends a single activity entry to the Supabase server.
     *
     * @param activity The [ActivityDataEntity] object to be sent.
     * @return `true` if the data was successfully sent and synced, `false` otherwise.
     */
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
            // Sends the activity data to the server with appropriate authorization
            val response = supabaseApiService.insertActivityData(
                authorization = "Bearer ${BuildConfig.supabaseKey}",
                apiKey = BuildConfig.supabaseKey,
                data = data
            )
            if (response.isSuccessful) {
                // Marks the activity as synced in the local database
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

    /**
     * Synchronizes all locally stored unsynced activities with the Supabase server.
     * Iterates through each unsynced activity and attempts to send it to the server.
     */
    suspend fun syncLocalChanges() {
        val unsyncedActivities = activityDao.getUnsyncedActivities()
        for (activity in unsyncedActivities) {
            val success = sendActivityDataToServer(activity)
            if (success) {
                Log.d("DataRepository", "Synced activity: $activity")
            } else {
                Log.e("DataRepository", "Failed to sync activity: $activity")
            }
        }
    }

    /**
     * Retrieves all activities for a specific user within the current month.
     *
     * @param userId The unique identifier of the user.
     * @return A list of [ActivityDataEntity] objects or `null` if the operation fails.
     */
    suspend fun getUserActivitiesForMonth(userId: String): List<ActivityDataEntity>? {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
        val startOfMonth = String.format("%04d-%02d-01T00:00:00", year, month)
        val endOfMonth = String.format(
            "%04d-%02d-%02dT23:59:59",
            year,
            month,
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        )

        // Filters for the Supabase API query
        val userIdFilter = "eq.$userId"
        val timestampGte = "gte.$startOfMonth"
        val timestampLte = "lte.$endOfMonth"

        return try {
            val accessToken = tokenRepository.getAccessToken()
            if (accessToken == null) {
                Log.e("DataRepository", "Access token is null. Cannot fetch activity data.")
                return null
            }
            // Fetches activities from the server within the specified time frame
            val response = supabaseApiService.getUserActivitiesForMonth(
                apiKey = BuildConfig.supabaseKey, // No "Bearer " prefix
                authorization = "Bearer ${BuildConfig.supabaseKey}",
                userIdFilter = userIdFilter,
                timestampGte = timestampGte,
                timestampLte = timestampLte
            )
            if (response.isSuccessful) {
                val activityCounts = response.body() ?: emptyList()
                Log.d("Supabase", "Data fetched successfully: $activityCounts")

                // Converts server data to local database entities
                val activityDataEntities = activityCounts.map { activityCount ->
                    ActivityDataEntity(
                        user_id = activityCount.user_id ?: "",
                        activity_type = activityCount.activity_type ?: "",
                        timestamp = activityCount.timestamp ?: "",
                        count = activityCount.count,
                        isSynced = true // Marks as already synced
                        // lastModified = parseISO8601ToMillis(activityCount.timestamp) // Optional
                    )
                }

                // Inserts fetched activities into the local database
                activityDao.insertActivities(activityDataEntities)
                Log.d("DataRepository", "Inserted activities into SQLite: $activityDataEntities")

                // Retrieves all activities from the local database
                val allActivities = activityDao.getAllActivities()
                Log.d("DataRepository", "Fetched activities from SQLite: $allActivities")
                allActivities

            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(
                    "Supabase",
                    "Error fetching data: ${response.code()} - ${response.message()} - $errorBody"
                )
                // Returns activities from the local database in case of an error
                activityDao.getAllActivities()
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Connection error: ${e.message}")
            // Returns activities from the local database if a connection error occurs
            activityDao.getAllActivities()
        }
    }

    /**
     * Retrieves all activities stored in the local SQLite database.
     *
     * @return A list of [ActivityDataEntity] objects representing all stored activities.
     */
    suspend fun getAllActivitiesFromDatabase(): List<ActivityDataEntity> {
        val data = activityDao.getAllActivities()
        Log.d("DataRepository", "Fetched activities: $data")
        return data
    }

    /**
     * Clears all activity data from the local SQLite database.
     * Useful for resetting the database or logging out the user.
     */
    suspend fun clearAllData() {
        activityDao.deleteAllActivities()
        Log.d("DataRepository", "All local data cleared from SQLite database.")
    }

    /**
     * Inserts a single activity entry into the local SQLite database.
     *
     * @param activity The [ActivityDataEntity] object to be inserted.
     */
    suspend fun insertActivity(activity: ActivityDataEntity) {
        activityDao.insertActivity(activity)
        Log.d("DataRepository", "Inserted activity: $activity")
    }

    /**
     * Refreshes activities by fetching the latest data from the server for the given user.
     *
     * @param userId The unique identifier of the user whose activities are to be refreshed.
     */
    suspend fun refreshActivitiesFromServer(userId: String) {
        // Implement additional logic here if needed for refreshing data
        getUserActivitiesForMonth(userId)
    }

    /**
     * Provides a [Flow] of all activities stored in the local database.
     * Enables reactive data handling in the UI layer.
     *
     * @return A [Flow] emitting lists of [ActivityDataEntity] objects.
     */
    fun getAllActivitiesFlow(): Flow<List<ActivityDataEntity>> {
        return activityDao.getAllActivitiesFlow()
    }
}
