package com.example.activityrecognitionapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.components.BluetoothDeviceList
import com.example.activityrecognitionapp.components.HeadingTextComponent
import com.example.activityrecognitionapp.components.NormalTextComponent
import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.presentation.states.BluetoothUiState

/**
 * Composable function representing the Device Screen.
 *
 * @param state The current UI state of Bluetooth.
 * @param connectedDevice The currently connected Bluetooth device, if any.
 * @param onStartScan Callback to initiate device scanning.
 * @param onDisconnect Callback to disconnect from the current device.
 * @param onDeviceClick Callback when a device is clicked from the list.
 * @param contentPadding Padding values for the content.
 */
@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    connectedDevice: BluetoothDeviceDomain?,
    onStartScan: () -> Unit,
    onDisconnect: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    contentPadding: PaddingValues
) {
    DeviceScreenContent(
        state = state,
        connectedDevice = connectedDevice,
        onStartScan = onStartScan,
        onDisconnect = onDisconnect,
        onDeviceClick = onDeviceClick,
    )
}

/**
 * Composable function that builds the content of the Device Screen.
 *
 * @param state The current UI state of Bluetooth.
 * @param connectedDevice The currently connected Bluetooth device, if any.
 * @param onStartScan Callback to initiate device scanning.
 * @param onDisconnect Callback to disconnect from the current device.
 * @param onDeviceClick Callback when a device is clicked from the list.
 */
@Composable
fun DeviceScreenContent(
    state: BluetoothUiState,
    connectedDevice: BluetoothDeviceDomain?,
    onStartScan: () -> Unit,
    onDisconnect: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Show a connecting indicator if the app is in the process of connecting
        if (state.isConnecting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Connecting...")
            }
        }

        // Display connection status if the app is connected and not currently connecting
        if (state.isConnected && !state.isConnecting) {
            connectedDevice?.let { device ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally // Center horizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected with ${device.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .weight(1f), // Take up remaining horizontal space
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = onDisconnect, // Handle disconnect button click
                            modifier = Modifier
                                .padding(
                                    top = 5.dp,
                                    end = 10.dp
                                ) // Additional padding to separate from edges
                                .size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.unlink),
                                contentDescription = "Disconnect Device",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // Heading for the Devices section
        HeadingTextComponent(
            value = stringResource(id = R.string.devices),
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 0.dp, bottom = 0.1.dp),
            textAlign = TextAlign.Left,
        )

        // Subheading or description for adding devices
        NormalTextComponent(
            value = stringResource(id = R.string.add_devices),
            modifier = Modifier.padding(start = 10.dp, top = 0.dp, end = 0.dp, bottom = 10.dp),
            style = TextStyle(fontSize = 15.sp),
            textAlign = TextAlign.Left
        )

        // Scaffold to structure the main content area
        Scaffold(
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium), // Rounded corners
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Button to start scanning for devices
                        IconButton(
                            onClick = onStartScan, // Handle scan button click
                            modifier = Modifier
                                .padding(
                                    top = 20.dp,
                                    bottom = 2.dp
                                ) // Additional padding to separate from edges
                                .size(60.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.add),
                                contentDescription = "Add Device",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp)) // Space between icon and text
                        Text(
                            text = "Add Device",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // List of scanned Bluetooth devices
                        BluetoothDeviceList(
                            scannedDevices = state.scannedDevices,
                            onClick = onDeviceClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
