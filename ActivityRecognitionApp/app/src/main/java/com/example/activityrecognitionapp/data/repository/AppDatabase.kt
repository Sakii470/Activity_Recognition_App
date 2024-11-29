package com.example.activityrecognitionapp.data.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.activityrecognitionapp.data.model.ActivityDataEntity

/**
 * Represents the application's local database using Room.
 *
 * This abstract class defines the database configuration and serves as the main access point
 * for the underlying connection to your app's persisted, relational data.
 *
 * @property activityDataDao Provides access to [ActivityDataDao] for CRUD operations on activity data.
 */

@Database(entities = [ActivityDataEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDataDao(): ActivityDataDao
}
