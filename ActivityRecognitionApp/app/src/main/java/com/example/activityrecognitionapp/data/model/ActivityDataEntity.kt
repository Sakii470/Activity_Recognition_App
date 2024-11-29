package com.example.activityrecognitionapp.data.model

import androidx.room.Entity

/**
 * Represents an activity count entry for a specific user on a particular day.
 *
 * This data class is annotated with Room's [Entity] to define a table in the local SQLite database.
 * It stores information about user activities, including the type of activity, the timestamp,
 * the count of activities performed, and whether the data has been synchronized with the server.
 *
 * @property user_id The unique identifier of the user performing the activity.
 * @property activity_type The type/category of the activity (e.g., walking, running).
 * @property timestamp The timestamp representing the day the activity was recorded, typically in ISO 8601 format.
 * @property count The number of times the specified activity was performed on the given day.
 * @property isSynced A flag indicating whether this activity data has been successfully synced with the remote server.
 *                   Defaults to `false`, meaning the data has not yet been synced.
 */
@Entity(
    tableName = "activity_counts_per_day",
    primaryKeys = ["user_id", "activity_type", "timestamp"]
)
data class ActivityDataEntity(
    val user_id: String,
    val activity_type: String,
    val timestamp: String,
    val count: Int?,
    val isSynced: Boolean? = false
)
