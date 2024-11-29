package com.example.activityrecognitionapp.components.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents an item in the bottom navigation bar.
 *
 * @property label The text label displayed for the navigation item.
 * @property icon The icon displayed alongside the label.
 * @property route The navigation route associated with this item.
 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)