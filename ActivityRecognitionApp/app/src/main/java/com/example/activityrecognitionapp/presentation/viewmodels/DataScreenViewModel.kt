package com.example.activityrecognitionapp.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.domain.ActivityDataProcessor
import com.example.activityrecognitionapp.domain.usecases.ActivitiesData.FetchUserActivitiesUseCase
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCountAggregated
import com.github.mikephil.charting.data.BarData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class DataScreenViewModel @Inject constructor(
    private val activityDataProcessor: ActivityDataProcessor,
    private val fetchUserActivitiesUseCase: FetchUserActivitiesUseCase,
) : ViewModel() {

    private val _activityCounts = MutableStateFlow<List<ActivityCountAggregated>>(emptyList())
    val activityCounts: StateFlow<List<ActivityCountAggregated>> = _activityCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()




    init {
        fetchUserActivities()
    }

    /**
     * Fetches user activities by invoking the use case and updates the UI state.
     */
    private fun fetchUserActivities() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = fetchUserActivitiesUseCase()

            result.onSuccess { aggregatedData ->
                _activityCounts.value = aggregatedData
                _errorMessage.value = null
                Log.d("DataScreenViewModel", "Activities fetched successfully.")
            }.onFailure { exception ->
                _errorMessage.value = exception.message
                Log.e("DataScreenViewModel", "Error fetching activities", exception)
            }
            _isLoading.value = false
        }
    }

    // Public functions to prepare data for the UI

    /**
     * Prepares data for a selected day.
     */
    fun prepareDataForSelectedDay(selectedDate: Date): Pair<List<ActivityCountAggregated>, List<String>> {
        return activityDataProcessor.prepareDataForSelectedDay(activityCounts.value, selectedDate)
    }

    /**
     * Prepares data for the "Day" tab.
     */
    fun prepareDataForDayTab(): Pair<List<ActivityCountAggregated>, List<String>> {
        return activityDataProcessor.prepareDataForDayTab(activityCounts.value)
    }

    /**
     * Prepares data for the "Week" tab.
     */
    fun prepareDataForWeekTab(referenceDate: Date): Pair<List<ActivityCountAggregated>, List<String>> {
        return activityDataProcessor.prepareDataForWeekTab(activityCounts.value, referenceDate)
    }

    /**
     * Prepares data for the "Month" tab.
     */
    fun prepareDataForMonthTab(referenceDate: Date): Pair<List<ActivityCountAggregated>, List<String>> {
        return activityDataProcessor.prepareDataForMonthTab(activityCounts.value, referenceDate)
    }

    /**
     * Calculates the maximum Y value for chart scaling.
     */
    fun calculateMaxYValue(activityCounts: List<ActivityCountAggregated>): Float {
        return activityDataProcessor.calculateMaxYValue(activityCounts)
    }

    /**
     * Prepares the BarData for the stacked bar chart.
     */
    fun prepareStackedBarData(activityCounts: List<ActivityCountAggregated>): BarData {
        return activityDataProcessor.prepareStackedBarData(activityCounts)
    }
}