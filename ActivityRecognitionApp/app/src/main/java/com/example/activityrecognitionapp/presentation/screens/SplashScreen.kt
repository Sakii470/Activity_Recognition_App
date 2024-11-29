package com.example.activityrecognitionapp.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.example.activityrecognitionapp.R
import kotlinx.coroutines.delay

/**
 * Composable function representing the Splash Screen of the app.
 *
 * @param navController The NavController used to navigate between screens.
 */
@Composable
fun SplashScreen(navController: NavController) {

    // State to control the visibility of the splash content
    var isVisible by remember { mutableStateOf(false) }

    // Side-effect to handle the splash screen display duration and navigation
    LaunchedEffect(Unit) {
        isVisible = true
        delay(1500)
        isVisible = false
        delay(300)

        // Navigate to the "login" screen and remove "splash" from the back stack
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    // Animated visibility to handle the entrance and exit animations of the splash content
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -1000 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 1000 })
    ) {
        // Box to center the splash content both vertically and horizontally
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // Display the app icon or logo image
            Image(
                painter = painterResource(id = R.drawable.jogging),
                contentDescription = "App Icon",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }
    }

}
