package com.example.activityrecognitionapp.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.activityrecognitionapp.data.repository.DataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataRepository: DataRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            dataRepository.syncLocalChanges()
           // dataRepository.syncServerChanges()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}