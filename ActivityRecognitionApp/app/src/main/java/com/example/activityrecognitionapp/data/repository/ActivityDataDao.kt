package com.example.activityrecognitionapp.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.activityrecognitionapp.data.model.ActivityDataEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing ActivityDataEntity operations.
 */
@Dao
interface ActivityDataDao {

    /**
     * Inserts a single activity. Replaces on conflict.
     *
     * @param activity The activity entity to insert.
     * @return The row ID of the inserted activity.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityDataEntity): Long

    /**
     * Inserts multiple activities. Replaces on conflict.
     *
     * @param activities The list of activity entities to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityDataEntity>)

    /**
     * Retrieves all activities from the database.
     *
     * @return A list of all activity entities.
     */
    @Query("SELECT * FROM activity_counts_per_day")
    suspend fun getAllActivities(): List<ActivityDataEntity>

    /**
     * Retrieves all activities as a Flow for reactive updates.
     *
     * @return A Flow emitting lists of activity entities.
     */
    @Query("SELECT * FROM activity_counts_per_day")
    fun getAllActivitiesFlow(): Flow<List<ActivityDataEntity>>

    /**
     * Deletes all activities from the database.
     */
    @Query("DELETE FROM activity_counts_per_day")
    suspend fun deleteAllActivities()

    /**
     * Retrieves activities that have not been synced with the server.
     *
     * @return A list of unsynced activity entities.
     */
    @Query("SELECT * FROM activity_counts_per_day WHERE isSynced = 0")
    suspend fun getUnsyncedActivities(): List<ActivityDataEntity>

    /**
     * Updates the sync status of a specific activity.
     *
     * @param userId The ID of the user.
     * @param activityType The type of activity.
     * @param timestamp The timestamp of the activity.
     * @param isSynced The new sync status.
     */
    @Query("UPDATE activity_counts_per_day SET isSynced = :isSynced WHERE user_id = :userId AND activity_type = :activityType AND timestamp = :timestamp")
    suspend fun updateSyncStatus(userId: String, activityType: String, timestamp: String, isSynced: Boolean)
}
