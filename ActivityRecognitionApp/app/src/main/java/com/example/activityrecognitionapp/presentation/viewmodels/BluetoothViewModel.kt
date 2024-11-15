package com.example.activityrecognitionapp.presentation.viewmodels


import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.data.bluetooth.BluetoothAdapterProvider
import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.BluetoothController
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.ConnectionResult
import com.example.activityrecognitionapp.presentation.states.BluetoothUiState
import com.example.activityrecognitionapp.presentation.states.HomeUiState
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject


// ViewModel managed by Hilt responsible for Bluetooth application logic, including connections, scanning, and managing UI state.
@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val tokenRepository: TokenRepository,
    private val repository: DataRepository,
    private val adapterProvider: BluetoothAdapterProvider
) : ViewModel() {


    // Bluetooth adapter
    private val bluetoothAdapter: BluetoothAdapter? = adapterProvider.getBluetoothAdapter()

    // Typ Job jest używany do obsługi zadań w tle
    private var deviceConnectionJob: Job? = null

    // Używany do aktywnego odświeżania UI, jeśli coś się stanie.
//    private val _state = MutableStateFlow(BluetoothUiState())
//    val state: StateFlow<BluetoothUiState> = _state.asStateFlow()

    // Bluetooth UI State
    private val _bluetoothUiState = MutableStateFlow(BluetoothUiState())
    val bluetoothUiState: StateFlow<BluetoothUiState> = _bluetoothUiState.asStateFlow()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    private val _events = MutableSharedFlow<BluetoothEvent>()
    val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()

//    // Kombinacja dwóch StateFlow w jeden StateFlow
//    val combinedState = combine(
//        bluetoothController.scannedDevices,
//        _state
//    ) { scannedDevices, state ->
//        state.copy(
//            scannedDevices = scannedDevices
//        )
//    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    // Inicjalizacja subskrypcji na przepływach z BluetoothController
    init {
        bluetoothController.scannedDevices.onEach { scannedDevices ->
            _bluetoothUiState.update { it.copy(scannedDevices = scannedDevices) }
        }.launchIn(viewModelScope)

        bluetoothController.isConnected.onEach { isConnected ->
            _bluetoothUiState.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _bluetoothUiState.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    // Funkcja odpowiedzialna za nawiązanie połączenia z urządzeniem Bluetooth.
    fun connectToDevice(device: BluetoothDeviceDomain) {
        _bluetoothUiState.update { it.copy(isConnecting = true) }
        deviceConnectionJob =
            bluetoothController
                .connectToDevice(device)
                .listen()
    }

    // Funkcja odpowiedzialna za rozłączenie z bieżącym urządzeniem Bluetooth.
    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _bluetoothUiState.update {
            it.copy(
                isConnecting = false,
                isConnected = false
            )
        }
    }

    // Funkcja do rozpoczęcia skanowania urządzeń Bluetooth.
    fun startScan() {
        bluetoothController.startDiscovery()
    }

    // Funkcja do zatrzymania skanowania urządzeń Bluetooth.
    fun stopScan() {
        bluetoothController.stopDiscovery()
    }

    // Funkcja do nasłuchiwania wyników połączenia Bluetooth i ich obsługi.
    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                is ConnectionResult.ConnectionEstablished -> {
                    Log.d("BluetoothViewModel", "Updating dataFromBluetooth to: ${result.dataFromBluetooth}")
                    sendActivityData(result.dataFromBluetooth)

                    _bluetoothUiState.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null,
                            dataFromBluetooth = result.dataFromBluetooth,
                            connectedDevice = result.connectedDevice,
                        )
                    }
                    updateHomeUi(result.dataFromBluetooth)
                }

                is ConnectionResult.Error -> {
                    _bluetoothUiState.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
            .catch { throwable ->
                bluetoothController.closeConnection()
                _bluetoothUiState.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    // Funkcja do wysyłania danych aktywności do Supabase.
    fun sendActivityData(activityType: String) {
        viewModelScope.launch {
            val userId = tokenRepository.getUserId()
            if (userId == null) {
                Log.e("BluetoothViewModel", "User ID is null. Cannot send activity data.")
                _errorMessages.emit("User not logged in.")
                return@launch
            }

            //val activityType = result.dataFromBluetooth.substringBefore("=") // Adjust parsing as needed
            val timestamp = getCurrentTimestampFormatted()

            val data = ActivityDataSupabase(
                user_id = userId,
                activity_type = activityType.substringBefore("="),
                timestamp = timestamp
            )

            try {
                val success = repository.sendActivityData(data)
                if (success) {
                    Log.d("BluetoothViewModel", "Activity Data sent successfully")
                } else {
                    Log.e("BluetoothViewModel", "Error sending activity data")
                    Log.d("JSON Data", Gson().toJson(data))
                    _errorMessages.emit("Error sending activity data.")
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Exception when sending activity data", e)
                _errorMessages.emit("Exception: ${e.message}")
            }
        }
    }

    // Funkcja aktualizująca stan HomeUiState na podstawie rodzaju aktywności.
    fun updateHomeUi(kindActivity: String) {
        _homeUiState.value = _homeUiState.value.copy(
            stand = _homeUiState.value.stand + if (kindActivity.startsWith("stand")) 1 else 0,
            walk = _homeUiState.value.walk + if (kindActivity.startsWith("walk")) 1 else 0,
            run = _homeUiState.value.run + if (kindActivity.startsWith("run")) 1 else 0,
            total = _homeUiState.value.stand + _homeUiState.value.walk + _homeUiState.value.run
        )
    }

    // Funkcja obsługująca zdarzenie włączenia Bluetooth.
    fun onBluetoothEnabled() {
        viewModelScope.launch {
            _bluetoothUiState.update { it.copy(errorMessage = null) }
        }
    }

    // Funkcja sprawdzająca, czy Bluetooth jest włączony.
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    // Funkcja obsługująca niepowodzenie włączenia Bluetooth.
    fun onBluetoothEnableFailed() {
        viewModelScope.launch {
            _errorMessages.emit("Enabling Bluetooth failed.")
        }
    }

    // Funkcja czyszcząca komunikaty błędów.
    fun clearErrorMessage() {
        viewModelScope.launch {
            _bluetoothUiState.update { it.copy(errorMessage = null) }
        }
    }

    // Funkcja emitująca zdarzenie żądania włączenia Bluetooth.
    fun enableBluetooth() {
        viewModelScope.launch {
            _events.emit(BluetoothEvent.RequestEnableBluetooth)
        }
    }
}

fun getCurrentTimestampFormatted(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date())
}

sealed class BluetoothEvent {
    object RequestEnableBluetooth : BluetoothEvent()
}








