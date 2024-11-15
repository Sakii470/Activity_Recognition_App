

package com.example.activityrecognitionapp.presentation.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.activityrecognitionapp.presentation.viewmodels.BluetoothEvent
import com.example.activityrecognitionapp.presentation.viewmodels.BluetoothViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun BluetoothScreen(
    //viewModel: BluetoothViewModel= hiltViewModel(),
    viewModel: BluetoothViewModel,
    onBluetoothEnableFailed: () -> Unit = {},
) {
    val bluetoothUiState by viewModel.bluetoothUiState.collectAsState()
    //val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showConnectionMessage by remember { mutableStateOf(false) }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
//viewModel.onBluetoothEnabled()
        } else {
            onBluetoothEnableFailed()
        }
    }

    // Zbieranie komunikatów błędów i wyświetlanie Snackbar
    LaunchedEffect(key1 = true) {
        viewModel.errorMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    // Zbieranie zdarzeń z ViewModel i obsługa włączania Bluetooth
    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BluetoothEvent.RequestEnableBluetooth -> {
                    enableBluetoothLauncher.launch(
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    )
                }
            }
        }
    }
    // Użycie `Scaffold` w Material 3 z `SnackbarHost`
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { contentPadding ->
            DeviceScreen(
                state = bluetoothUiState,
                connectedDevice = bluetoothUiState.connectedDevice,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onDeviceClick = viewModel::connectToDevice,
                onDisconnect = viewModel::disconnectFromDevice,
                modifier = Modifier.padding(contentPadding)
            )
        }
    )

    //Obsługa uprawnień Bluetooth
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
                    message = "Bluetooth must be enabled to use this mobile device functionality."
                )
            }
        }
    )
}

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
            true
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

    // Wywołanie żądania uprawnień po inicjalizacji
    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        }
    }
}


