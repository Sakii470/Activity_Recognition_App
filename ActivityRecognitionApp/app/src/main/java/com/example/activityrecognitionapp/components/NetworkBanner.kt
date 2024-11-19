package com.example.activityrecognitionapp.components

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

@Composable
fun NetworkBanner(
    isConnected: Boolean,
    onBannerDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("NetworkBanner", "Rendering NetworkBanner: isConnected=$isConnected")

    val (message, barColor) = if (isConnected) {
        "Connection Restored" to Color(0xFF4CAF50) // Green
    } else {
        "No Connection" to Color(0xFF2196F3) // Blue
    }

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
            contentAlignment = Alignment.Center // Wyrównanie całej zawartości Boxa do środka
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, // Wyrównanie elementów w wierszu w pionie
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center     // Wyrównanie elementów w wierszu w poziomie
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp) // Odstęp między tekstem a wskaźnikiem
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp), // Rozmiar wskaźnika
                    color = Color.White,
                    strokeWidth = 2.dp // Grubość wskaźnika
                )
            }
        }
    }

    if (isConnected) {
        LaunchedEffect(Unit) {
            delay(2000)
            onBannerDismissed()
            Log.d("NetworkBanner", "Banner dismissed after reconnection.")
        }
    }
}