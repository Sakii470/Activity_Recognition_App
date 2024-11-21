package com.example.activityrecognitionapp.presentation.states

class DataUiState {
    data class ActivityCount(
        val user_id: String?,
        val timestamp: String,
        val activity_type: String,
        val count: Int?
    )

    data class ActivityCountAggregated(
        val date: String,
        val stand: Int,
        val walk: Int,
        val run: Int
    )


}