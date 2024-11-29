package com.example.activityrecognitionapp.domain.usecases.ActivitiesData

import com.example.activityrecognitionapp.data.network.NetworkConnectivityObserver
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.ActivityDataProcessor
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCount
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCountAggregated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for fetching and aggregating user activities.
 */
class FetchUserActivitiesUseCase @Inject constructor(
    private val repository: DataRepository,
    private val activityDataProcessor: ActivityDataProcessor,
    private val tokenRepository: TokenRepository,
    private val networkConnectivityObserver: NetworkConnectivityObserver
) {

    /**
     * Fetches user activities and returns aggregated data.
     */
    suspend operator fun invoke(): Result<List<ActivityCountAggregated>> {
        return withContext(Dispatchers.IO) {
            val userId = tokenRepository.getUserId()
            if (userId == null) {
                return@withContext Result.failure(Exception("User not logged in."))
            }

            try {
                val isNetworkAvailable = networkConnectivityObserver.isConnected.value

                // Fetch data from server if network is available
                val serverData = if (isNetworkAvailable) {
                    repository.getUserActivitiesForMonth(userId)
                } else {
                    emptyList()
                }

                // Decide whether to use server data or local data
                val dataToProcess = if (!serverData.isNullOrEmpty()) {
                    serverData
                } else {
                    repository.getAllActivitiesFromDatabase()
                }

                if (dataToProcess.isEmpty()) {
                    return@withContext Result.failure(Exception("No activity data available."))
                }

                // Map data to ActivityCount
                val activityCounts = dataToProcess.map { activityDataEntity ->
                    ActivityCount(
                        user_id = activityDataEntity.user_id,
                        activity_type = activityDataEntity.activity_type,
                        timestamp = activityDataEntity.timestamp,
                        count = activityDataEntity.count
                    )
                }

                // Aggregate data
                val aggregatedData = activityDataProcessor.aggregateActivityData(activityCounts)
                return@withContext Result.success(aggregatedData)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    }
}
