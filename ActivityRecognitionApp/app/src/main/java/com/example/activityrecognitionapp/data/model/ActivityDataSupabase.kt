package com.example.activityrecognitionapp.data.model

/**
 * Represents activity data to be sent to Supabase.
 *
 * @property user_id The unique identifier of the user. Nullable in case the user is not authenticated.
 * @property activity_type The type or category of the activity (e.g., walking, running).
 * @property timestamp The timestamp when the activity was recorded, typically in ISO 8601 format.
 */
data class ActivityDataSupabase(
    val user_id: String?,
    val activity_type: String,
    val timestamp: String
)
