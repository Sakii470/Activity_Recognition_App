package com.example.activityrecognitionapp.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.activityrecognitionapp.data.model.ActivityDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityDataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityDataEntity>)

    @Query("SELECT * FROM activity_counts_per_day")
    suspend fun getAllActivities(): List<ActivityDataEntity>

    @Query("SELECT * FROM activity_counts_per_day")
    fun getAllActivitiesFlow(): Flow<List<ActivityDataEntity>>

    @Query("DELETE FROM activity_counts_per_day")
    suspend fun deleteAllActivities()

    @Query("SELECT * FROM activity_counts_per_day WHERE isSynced = 0")
    suspend fun getUnsyncedActivities(): List<ActivityDataEntity>

    @Query("UPDATE activity_counts_per_day SET isSynced = :isSynced WHERE user_id = :userId AND activity_type = :activityType AND timestamp = :timestamp")
    suspend fun updateSyncStatus(userId: String, activityType: String, timestamp: String, isSynced: Boolean)
}