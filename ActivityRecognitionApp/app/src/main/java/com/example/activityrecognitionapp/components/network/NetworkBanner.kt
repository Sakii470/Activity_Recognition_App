package com.example.activityrecognitionapp.components.network

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Displays a network connectivity banner that informs the user about the current connection status.
 *
 * When the device is connected, it shows a "Connection Restored" message with a green background.
 * When disconnected, it displays a "No Connection" message with a blue background.
 * The banner animates into view and can automatically dismiss after a short delay when reconnected.
 *
 * @param isConnected Indicates whether the device is currently connected to the internet.
 * @param onBannerDismissed Callback function invoked when the banner is dismissed.
 * @param modifier Modifier to customize the appearance and layout of the banner.
 */
@Composable
fun NetworkBanner(
    isConnected: Boolean,
    onBannerDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("NetworkBanner", "Rendering NetworkBanner: isConnected=$isConnected")

    // Determine the message and background color based on the connection status
    val (message, barColor) = if (isConnected) {
        "Connection Restored" to Color(0xFF4CAF50)
    } else {
        "No Connection" to Color(0xFF2196F3)
    }

    // Animated visibility for the banner with slide-in and slide-out animations
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        Box(
            modifier = modifier
                .background(barColor)
                .fillMaxWidth()
                .height(25.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }

    // Automatically dismiss the banner after 2 seconds when the connection is restored
    if (isConnected) {
        LaunchedEffect(Unit) {
            delay(2000)          // Wait for 2 seconds
            onBannerDismissed() // Invoke the dismissal callback
            Log.d("NetworkBanner", "Banner dismissed after reconnection.")
        }
    }
}
