package com.example.activityrecognitionapp.data.model

data class ActivityDataSupabase(
    val user_id: String?,         // Identyfikator użytkownika
    val activity_type: String,    // Typ aktywności, np. "stand", "run", "walk"
    val timestamp: String          // Znacznik czasu (w milisekundach)
)


