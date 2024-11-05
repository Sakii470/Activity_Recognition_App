package com.example.activityrecognitionapp.presentation


import android.app.Application
import android.bluetooth.BluetoothManager
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.domain.BluetoothController
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import android.bluetooth.BluetoothAdapter

import android.content.Context
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.example.activityrecognitionapp.domain.ConnectionResult

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch



// ViewModel managed by Hilt responsible for Bluetooth application logic, including connections, scanning, and managing UI state.
@HiltViewModel
class BluetoothViewModel @Inject constructor(
    //Reference to instance BluetoothController
    private val bluetoothController: BluetoothController,
    private val app: Application
): ViewModel() {


    // Bluetooth adapter
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Method to check if Bluetooth is enabled


    //Type Job is use to to tasks works in background
    private var deviceConnectionJob: Job? = null

    //Is used to active refresh UI if something happen.
    private val _state = MutableStateFlow(BluetoothUiState())

//    val isBluetoothEnabled: Boolean
//        get() = bluetoothAdapter?.isEnabled == true

    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    private val _events = MutableSharedFlow<BluetoothEvent>()
    val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()


    //combine 2 StateFlow into 1 StateFlow
    val state = combine(
        bluetoothController.scannedDevices,

        _state
    ) { scannedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,

        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    //Init is responsible for subscribe stateFlows if any stateFlow will be change this block update _stateFlow
    init {
        bluetoothController.isConnected.onEach { isConnected ->
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _state.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    //Function responsible for create connection with bluetooth device.
    fun connectToDevice(device: BluetoothDeviceDomain) {
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController //Start connection process by calling connectToDevice method from BluetoothController
            .connectToDevice(device) // Result of connection is observed using listen()
            .listen() // Subscribe to the outcome of the connection attempt.
}

    // Function responsible for disconnecting from the current Bluetooth device.
    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update { it.copy(
            isConnecting = false,
            isConnected = false
        ) }
    }

// Function to start scanning for Bluetooth devices.
    fun startScan() {
        bluetoothController.startDiscovery()
    }
    // Function to stop scanning for Bluetooth devices.
    fun stopScan() {
        bluetoothController.stopDiscovery()
    }
    //Function subscribe outcomes binding with create bluetooth devices.
    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when(result) {
                is ConnectionResult.ConnectionEstablished -> {
                    _state.update { it.copy(
                        isConnected = true,
                        isConnecting = false,
                        errorMessage = null,
                        dataFromBluetooth = result.dataFromBluetooth

                    ) }
                }
                is ConnectionResult.Error -> {
                    _state.update { it.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = result.message
                    ) }
                }
            }
        }
            .catch { throwable ->
                bluetoothController.closeConnection()
                _state.update { it.copy(
                    isConnected = false,
                    isConnecting = false,
                ) }
            }
            .launchIn(viewModelScope)
    }

    fun onBluetoothEnabled() {
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
        }
    }
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun onBluetoothEnableFailed() {
        viewModelScope.launch {
            _errorMessages.emit("Enabling Bluetooth failed.")
        }
    }

    fun clearErrorMessage() {
        viewModelScope.launch {
            _state.update {it.copy(errorMessage=null)  }
        }
    }

    fun enableBluetooth() {
        viewModelScope.launch {
            _events.emit(BluetoothEvent.RequestEnableBluetooth)
        }
    }
}

sealed class BluetoothEvent {
    object RequestEnableBluetooth : BluetoothEvent()
}




