package com.example.activityrecognitionapp.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.data.network.NetworkConnectivityObserver
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.ActivityDataProcessor
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCount
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCountAggregated
import com.github.mikephil.charting.data.BarData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class DataScreenViewModel @Inject constructor(
    private val repository: DataRepository,
    private val tokenRepository: TokenRepository,
    private val activityDataProcessor: ActivityDataProcessor,
    private val networkConnectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _activityCounts = MutableStateFlow<List<ActivityCountAggregated>>(emptyList())
    val activityCounts: StateFlow<List<ActivityCountAggregated>> = _activityCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

   val isNetworkAvialable = networkConnectivityObserver.isConnected.value


    init {
        fetchUserActivities()
    }

    private fun fetchUserActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = tokenRepository.getUserId()
            if (userId == null) {
                _isLoading.value = false
                _errorMessage.value = "User not logged in."
                Log.e("DataScreenViewModel", "User ID is null. Cannot fetch activity data.")
                return@launch
            }
            try {
                // Pobierz dane z serwera i zapisz do SQLite
                val serverData = withContext(Dispatchers.IO) {
                    repository.getUserActivitiesForMonth(userId)
                }

                if (serverData != null && serverData.isNotEmpty() && isNetworkAvialable) {
                    // Przekształcenie danych z ActivityDataEntity na ActivityCount
                    val activityCounts = serverData.map { activityDataEntity ->
                        ActivityCount(
                            user_id = activityDataEntity.user_id,
                            activity_type = activityDataEntity.activity_type,
                            timestamp = activityDataEntity.timestamp,
                            count = activityDataEntity.count
                        )
                    }

                    // Agregacja danych
                    val aggregatedData = activityDataProcessor.aggregateActivityData(activityCounts)

                    // Zaktualizowanie StateFlow
                    _activityCounts.value = aggregatedData
                    _errorMessage.value = null
                    Log.d("DataScreenViewModel", "Activity data fetched from Supabase and aggregated successfully.")
                } else {
                    // Jeśli nie ma danych z serwera, pobierz lokalne
                    val localData = withContext(Dispatchers.IO) {
                        repository.getAllActivitiesFromDatabase()
                    }
                    if (localData.isNotEmpty()) {
                        val activityCounts = localData.map { activityDataEntity ->
                            ActivityCount(
                                user_id = activityDataEntity.user_id,
                                activity_type = activityDataEntity.activity_type,
                                timestamp = activityDataEntity.timestamp,
                                count = activityDataEntity.count
                            )
                        }
                        val aggregatedData = activityDataProcessor.aggregateActivityData(activityCounts)
                        _activityCounts.value = aggregatedData
                        _errorMessage.value = null
                        Log.d("DataScreenViewModel", "Activity data fetched from local SQLite and aggregated successfully.")
                    } else {
                        _errorMessage.value = "No activity data found in local database."
                        Log.e("DataScreenViewModel", "No data available in SQLite database.")
                    }
                }

            } catch (e: Exception) {
                _errorMessage.value = "An error occurred: ${e.message}"
                Log.e("DataScreenViewModel", "Error fetching activity data", e)
            } finally {
                _isLoading.value = false
            }
        }


    }

    // Publicne funkcje do przygotowania danych
    fun prepareDataForSelectedDay(selectedDate: Date): Pair<List<ActivityCountAggregated>, List<String>> {
        return activityDataProcessor.prepareDataForSelectedDay(activityCounts.value, selectedDate)
    }

    fun prepareDataForDayTab(): Pair<List<ActivityCountAggregated>, List<String>> {
        return activityDataProcessor.prepareDataForDayTab(activityCounts.value)
    }

    fun prepareDataForWeekTab(referenceDate: Date): Pair<List<ActivityCountAggregated>, List<String>> {
        return activityDataProcessor.prepareDataForWeekTab(activityCounts.value, referenceDate)
    }

    fun prepareDataForMonthTab(referenceDate: Date): Pair<List<ActivityCountAggregated>, List<String>> {
        return activityDataProcessor.prepareDataForMonthTab(activityCounts.value, referenceDate)
    }

    fun calculateMaxYValue(activityCounts: List<ActivityCountAggregated>): Float {
        return activityDataProcessor.calculateMaxYValue(activityCounts)
    }

    fun prepareStackedBarData(activityCounts: List<ActivityCountAggregated>): BarData {
        return activityDataProcessor.prepareStackedBarData(activityCounts)
    }

    // Opcjonalnie: Funkcje do dodawania aktywności, itp.
}