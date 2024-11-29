package com.example.activityrecognitionapp.presentation.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.activityrecognitionapp.presentation.viewmodels.BluetoothViewModel
import kotlinx.coroutines.launch

/**
 * Composable function for the Bluetooth screen.
 *
 * @param viewModel ViewModel handling Bluetooth logic.
 * @param onBluetoothEnableFailed Callback invoked when enabling Bluetooth fails.
 */
@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel,
    onBluetoothEnableFailed: () -> Unit = {},
) {
    // Observe the current Bluetooth UI state from the ViewModel
    val bluetoothUiState by viewModel.bluetoothUiState.collectAsState()
    // Initialize SnackbarHostState for displaying snackbars
    val snackbarHostState = remember { SnackbarHostState() }
    // Coroutine scope for launching snackbar actions
    val coroutineScope = rememberCoroutineScope()

    // Scaffold layout with SnackbarHost
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { contentPadding ->
            // Display the DeviceScreen with necessary callbacks and padding
            DeviceScreen(
                state = bluetoothUiState,
                connectedDevice = bluetoothUiState.connectedDevice,
                onStartScan = viewModel::startScan,
                onDeviceClick = viewModel::connectToDevice,
                onDisconnect = viewModel::disconnectFromDevice,
                // Uncomment and implement if Bluetooth enabling is needed
                // enableBluetooth = viewModel::enableBluetooth,
                contentPadding = contentPadding // Pass the content padding
            )
        }
    )

    // Handle Bluetooth permissions
    PermissionsHandler(
        isBluetoothEnabled = viewModel.isBluetoothEnabled(),
        onPermissionsGranted = {
            if (!viewModel.isBluetoothEnabled()) {
                // Enable Bluetooth if not already enabled
                viewModel.enableBluetooth()
            }
        },
        onPermissionsDenied = {
            // Show snackbar message if permissions are denied
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Bluetooth must be enabled to use this mobile device functionality.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    )
}

/**
 * Composable function to handle Bluetooth permissions.
 *
 * @param isBluetoothEnabled Current state of Bluetooth being enabled.
 * @param onPermissionsGranted Callback invoked when permissions are granted.
 * @param onPermissionsDenied Callback invoked when permissions are denied.
 */
@Composable
fun PermissionsHandler(
    isBluetoothEnabled: Boolean,
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
) {
    // Launcher for requesting multiple permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        // Determine if Bluetooth permissions are granted based on Android version
        val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                    perms[Manifest.permission.BLUETOOTH_SCAN] == true
        } else {
            perms[Manifest.permission.BLUETOOTH] == true &&
                    perms[Manifest.permission.BLUETOOTH_ADMIN] == true
        }

        // Invoke callbacks based on permission results
        if (canEnableBluetooth && !isBluetoothEnabled) {
            onPermissionsGranted()
        }
        if (isBluetoothEnabled) {
            // Do nothing if Bluetooth is already enabled
            Unit
        } else {
            onPermissionsDenied()
        }
    }

    // Launch permission request when the composable is first rendered
    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Request Bluetooth permissions for Android S and above
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            // Request Bluetooth permissions for below Android S
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }
    }
}
