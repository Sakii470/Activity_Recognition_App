package com.example.activityrecognitionapp.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.activityrecognitionapp.presentation.theme.LighterPrimary

/**
 * Displays a bottom navigation bar with Home, Data, and Device items.
 *
 * @param navController Controls navigation between different screens.
 */
@Composable
fun BottomNavigationBar(navController: NavController) {
    // Get the current navigation destination
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    // Define the navigation items
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Data", Icons.Default.Storage, "data"),
        BottomNavItem("Device", Icons.Default.Watch, "bluetooth")
    )

    // Create the navigation bar
    NavigationBar {
        items.forEach { item ->
            // Check if the current item is selected
            val selected = currentDestination?.route == item.route

            // Animate the icon color based on selection
            val iconColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            // Define each navigation item
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor
                    )
                },
                label = { Text(item.label) },
                selected = selected,
                onClick = {
                    // Navigate to the selected route
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = LighterPrimary
                )
            )
        }
    }
}
