package com.example.activityrecognitionapp.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.components.ActivityBarChartWithLegend
import com.example.activityrecognitionapp.presentation.states.HomeUiState
import com.example.activityrecognitionapp.presentation.viewmodels.BluetoothViewModel

/**
 * Composable function for the Home screen.
 *
 * @param navController Navigation controller to handle screen transitions.
 * @param viewModel ViewModel handling Bluetooth and home UI state.
 */
@Composable
fun HomeScreen(navController: NavController, viewModel: BluetoothViewModel) {

    // Observe the current Bluetooth and Home UI states from the ViewModel
    val bluetoothUiState by viewModel.bluetoothUiState.collectAsState()
    val homeUiState by viewModel.homeUiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Show dialog if Bluetooth is disconnected
    LaunchedEffect(bluetoothUiState.isConnected) {
        if (!bluetoothUiState.isConnected) {
            showDialog = true
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Display Bluetooth connection status
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 8.dp)
                    .clip(MaterialTheme.shapes.medium), // Rounded corners
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (bluetoothUiState.isConnected) {
                        // Show data received from Bluetooth
                        Text(
                            text = "${bluetoothUiState.dataFromBluetooth}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        // Show disconnected message
                        Text(
                            text = stringResource(id = R.string.no_connect_ble_dev),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(35.dp))

            // Display activity bar chart with legend
            ActivityBarChartWithLegend(
                standingPercentage = homeUiState.standPercentage,
                walkingPercentage = homeUiState.walkPercentage,
                runningPercentage = homeUiState.runPercentage,
                unknownActivityPercentage = homeUiState.unknownActivityPercentage,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Display activity counters
            ActivityCounters(homeUiState = homeUiState)

            Spacer(modifier = Modifier.height(80.dp))

            // Row containing Start and Pause/Reset icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start Icon
                Icon(
                    painter = painterResource(id = R.drawable.start),
                    contentDescription = "Start Icon",
                    modifier = Modifier
                        .size(70.dp)
                        .clickable {
                            viewModel.startChart()
                        },
                    tint = Color.Unspecified
                )

                // Pause/Reset Icon
                Icon(
                    painter = painterResource(id = R.drawable.pause),
                    contentDescription = "Pause/Reset Icon",
                    modifier = Modifier
                        .size(70.dp)
                        .clickable {
                            viewModel.stopChart()
                        },
                    tint = Color.Unspecified
                )
            }
        }
    }

    // Show AlertDialog if Bluetooth is disconnected
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "No Connection",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.bluetooth_warnning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        navController.navigate("bluetooth") // Navigate to Bluetooth screen
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }, // Dismiss the dialog
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Composable function to display activity counters.
 *
 * @param homeUiState Current state of the home screen.
 */
@Composable
fun ActivityCounters(homeUiState: HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActivityCounter(label = "Standing", count = homeUiState.stand)
        ActivityCounter(label = "Walking", count = homeUiState.walk)
        ActivityCounter(label = "Running", count = homeUiState.run)
        ActivityCounter(label = "Unknown", count = homeUiState.unknownActivity)
    }
}

/**
 * Composable function to display a single activity counter.
 *
 * @param label Label for the activity type.
 * @param count Count of the activity occurrences.
 */
@Composable
fun ActivityCounter(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
