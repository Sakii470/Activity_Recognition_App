package com.example.activityrecognitionapp.data.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.activityrecognitionapp.data.model.ActivityDataEntity

@Database(entities = [ActivityDataEntity::class], version = 4)
abstract class AppDatabase: RoomDatabase() {
    abstract fun activityDataDao(): ActivityDataDao
}