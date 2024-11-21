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

@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel,
    onBluetoothEnableFailed: () -> Unit = {},
) {
    val bluetoothUiState by viewModel.bluetoothUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launcher to enable Bluetooth
//    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode != android.app.Activity.RESULT_OK) {
//            onBluetoothEnableFailed()
//            // Optionally, you can also call viewModel.onBluetoothEnableFailed() here
//            // Uncomment the below line if such a method exists in ViewModel
//            // viewModel.onBluetoothEnableFailed()
//        }
//        // No need to update the Bluetooth state manually
//    }

    // Collect error messages and display Snackbar
//    LaunchedEffect(key1 = true) {
//        viewModel.errorMessages.collectLatest { message ->
//            if (message.isNotEmpty()) {
//                snackbarHostState.showSnackbar(message)
//                viewModel.clearErrorMessage()
//            }
//        }
//    }

//    // Collect events from ViewModel and handle enabling Bluetooth
//    LaunchedEffect(Unit) {
//        Log.d("BluetoothScreen", "LaunchedEffect started")
//        viewModel.events.collectLatest { event ->
//            when (event) {
//                is BluetoothEvent.RequestEnableBluetooth -> {
//                    // Log for debugging
//                    Log.d("BluetoothScreen", "Launching intent to request enabling Bluetooth")
////                    BluetoothbluetoothEnableLauncher.launch(
////                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
////                    )
//                }
//            }
//        }
//    }

    // Use Scaffold with SnackbarHost
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { contentPadding ->
            DeviceScreen(
                state = bluetoothUiState,
                connectedDevice = bluetoothUiState.connectedDevice,
                onStartScan = viewModel::startScan,
                onDeviceClick = viewModel::connectToDevice,
                onDisconnect = viewModel::disconnectFromDevice,
                //enableBluetooth = viewModel::enableBluetooth,
                contentPadding = contentPadding // Pass the content padding
            )
        }
    )

    // Handle Bluetooth permissions
    PermissionsHandler(
        isBluetoothEnabled = viewModel.isBluetoothEnabled(),
        onPermissionsGranted = {
            if (!viewModel.isBluetoothEnabled()) {
                viewModel.enableBluetooth()
            }
        },
        onPermissionsDenied = {
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
 * PermissionsHandler manages Bluetooth permissions.
 */
@Composable
fun PermissionsHandler(
    isBluetoothEnabled: Boolean,
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                    perms[Manifest.permission.BLUETOOTH_SCAN] == true
        } else {
            perms[Manifest.permission.BLUETOOTH] == true &&
                    perms[Manifest.permission.BLUETOOTH_ADMIN] == true
        }

        if (canEnableBluetooth && !isBluetoothEnabled) {
            onPermissionsGranted()
        }
        if (isBluetoothEnabled) {
            Unit
        } else {
            onPermissionsDenied()
        }
    }

   //  Launch permission request on initialization
    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }
    }


}




