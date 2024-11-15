package com.example.activityrecognitionapp.presentation.states

class DataUiState {
    data class ActivityCount(
        val user_id: String,
        val timestamp: String, // Pełna data i czas w formacie ISO 8601
        val activity_type: String,
        val count: Int
    )

    data class ActivityCountAggregated(
        val date: String, // "yyyy-MM-dd"
        val stand: Int,
        val walk: Int,
        val run: Int
    )
}