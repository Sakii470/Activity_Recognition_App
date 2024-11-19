package com.example.activityrecognitionapp.data.model

import androidx.room.Entity


@Entity(tableName = "activity_counts_per_day",
    primaryKeys = ["user_id", "activity_type", "timestamp"])
data class ActivityDataEntity (
    val user_id: String,
    val activity_type: String,
    val timestamp: String,
    val count: Int?,
    val isSynced: Boolean? = false,
    //val lastModified: Long
)
