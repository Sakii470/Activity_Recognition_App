package com.example.activityrecognitionapp.presentation

import android.annotation.SuppressLint

import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.domain.chat.BluetoothController
import com.example.activityrecognitionapp.domain.chat.BluetoothDeviceDomain
import com.example.activityrecognitionapp.presentation.BluetoothUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import android.content.Context as Context1
import com.example.activityrecognitionapp.domain.chat.ConnectionResult

//defined class ViewModel which one is manage by Hilt. @Inject - in constuctor means get BluetoothController when create ViewModel
@HiltViewModel
class BluetoothViewModel @Inject constructor(
//reference to instance BluetoothController
    private val bluetoothController: BluetoothController
): ViewModel() {


//    companion object {
//        val SERVICE_UUID: UUID = UUID.fromString("020c6211-64f6-4e6d-82e8-c2c1391f75fa")
//        val TxCharacteristic_UUID: UUID = UUID.fromString("020c6213-64f6-4e6d-82e8-c2c1391f75fa")
//    }

    //type Job is use to to tasks works in background
    private var deviceConnectionJob: Job? = null

    //is used to active refresh UI if something happen.
    private val _state = MutableStateFlow(BluetoothUiState())

    var dataFromBluetooth by mutableStateOf(0f)
        private set

    //combine 3 StateFlow in 1 StateFlow
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    //init is responsible for subscribe stateFlows if any stateFlow will be change this block update _stateFlow
    init {
        bluetoothController.isConnected.onEach { isConnected ->
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _state.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    //function responsible for create connection with bluetooth device.

    fun connectToDevice(device: BluetoothDeviceDomain) {
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .listen()
}


    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update { it.copy(
            isConnecting = false,
            isConnected = false
        ) }
    }

//    fun waitForIncomingConnections() {
//        _state.update { it.copy(isConnecting = true) }
//        deviceConnectionJob = bluetoothController
//            .startBluetoothServer()
//            .listen()
//    }

    fun startScan() {
        bluetoothController.startDiscovery()
    }

    fun stopScan() {
        bluetoothController.stopDiscovery()
    }
    //function subscribe outcomes binding with create bluetooth devices.
    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when(result) {
                is ConnectionResult.ConnectionEstabilished -> {
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

//    override fun onCleared() {
//        super.onCleared()
//        bluetoothController.release()
//    }
}



