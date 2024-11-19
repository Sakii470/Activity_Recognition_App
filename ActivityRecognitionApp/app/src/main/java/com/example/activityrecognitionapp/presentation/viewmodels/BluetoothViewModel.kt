package com.example.activityrecognitionapp.presentation.viewmodels


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.components.NetworkBannerManager
import com.example.activityrecognitionapp.data.bluetooth.BluetoothAdapterProvider
import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.BluetoothController
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.ConnectionResult
import com.example.activityrecognitionapp.presentation.states.BluetoothUiState
import com.example.activityrecognitionapp.presentation.states.HomeUiState
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
    private val adapterProvider: BluetoothAdapterProvider,
    //private val connectivityObserver: NetworkConnectivityObserver,
    private val networkBannerManager: NetworkBannerManager

) : ViewModel() {
    private var deviceConnectionJob: Job? = null

    // Bluetooth UI State
    private val _bluetoothUiState = MutableStateFlow(BluetoothUiState())
    val bluetoothUiState: StateFlow<BluetoothUiState> = _bluetoothUiState.asStateFlow()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    private val _events = MutableSharedFlow<BluetoothEvent>(replay = 1)
    val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()

    val isNetworkAvailable: StateFlow<Boolean> = networkBannerManager.isNetworkAvailable
    val showNetworkBanner: StateFlow<Boolean> = networkBannerManager.showNetworkBanner



    init {

        //connectivityObserver.start()
        //observeNetworkConnectivity()

        // Subskrypcja isBluetoothEnabled
        viewModelScope.launch {
            bluetoothController.isBluetoothEnabled.collect { isEnabled ->
                _bluetoothUiState.update { it.copy(isBluetoothEnabled = isEnabled) }
            }
        }

        // Subskrypcja isConnected
        viewModelScope.launch {
            bluetoothController.isConnected.collect { isConnected ->
                _bluetoothUiState.update { it.copy(isConnected = isConnected) }
            }
        }

        // Subskrypcja scannedDevices
        viewModelScope.launch {
            bluetoothController.scannedDevices.collect { scannedDevices ->
                _bluetoothUiState.update { it.copy(scannedDevices = scannedDevices) }
            }
        }

        // Subskrypcja błędów
        viewModelScope.launch {
            bluetoothController.errors.collect { error ->
                _bluetoothUiState.update { it.copy(errorMessage = error) }
                _errorMessages.emit(error)
            }
        }
    }

    private fun observeNetworkConnectivity() {
        viewModelScope.launch {
            showNetworkBanner.collect { show ->
                if (!show) {
                    Log.d("BluetoothViewModel", "Network banner dismissed.")
                }
            }
        }
    }

    // Funkcja odpowiedzialna za nawiązanie połączenia z urządzeniem Bluetooth.
    fun connectToDevice(device: BluetoothDeviceDomain) {
        _bluetoothUiState.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
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
                isConnected = false,
                connectedDevice = null
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
                    if (result.dataFromBluetooth != null) {
                        updateHomeUi(result.dataFromBluetooth)
                    } else {
                        updateHomeUi("Connect to Bluetooth Device!")
                    }
                }
                is ConnectionResult.Error -> {
                    _bluetoothUiState.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.message
                        )
                    }
                    _errorMessages.emit(result.message)
                }
            }
        }
            .catch { throwable ->
                bluetoothController.closeConnection()
                _bluetoothUiState.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = throwable.message
                    )
                }
                _errorMessages.emit(throwable.message ?: "Unknown error")
            }
            .launchIn(viewModelScope)
    }

    // Funkcja do wysyłania danych aktywności do Supabase.
    private fun sendActivityData(activityType: String) {
        viewModelScope.launch {
            val userId = tokenRepository.getUserId()
            if (userId == null) {
                Log.e("BluetoothViewModel", "User ID is null. Cannot send activity data.")
                _errorMessages.emit("User not logged in.")
                return@launch
            }

            val timestamp = getCurrentTimestampFormatted()
            val data = ActivityDataSupabase(
                user_id = userId,
                activity_type = activityType.substringBefore("="),
                timestamp = timestamp
            )

            try {
                // Save data locally first
                repository.saveActivityDataLocally(data)
                Log.d("BluetoothViewModel", "Activity Data saved locally")

                // Then try to sync local changes
                    repository.syncLocalChanges()

            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Exception when saving or syncing activity data", e)
                _errorMessages.emit("Exception: ${e.message}")
            }
        }
    }

    // Funkcja aktualizująca stan HomeUiState na podstawie rodzaju aktywności.
    private fun updateHomeUi(kindActivity: String) {
        _homeUiState.update {
            it.copy(
                stand = it.stand + if (kindActivity.startsWith("stand")) 1 else 0,
                walk = it.walk + if (kindActivity.startsWith("walk")) 1 else 0,
                run = it.run + if (kindActivity.startsWith("run")) 1 else 0,
                total = it.total + 1
            )
        }
    }

    // Emitowanie zdarzenia żądania włączenia Bluetooth


    fun enableBluetooth() {
        viewModelScope.launch {
            Log.d("BluetoothViewModel", "emit from enableBluetooth")
            _events.emit(BluetoothEvent.RequestEnableBluetooth)
        }
    }

    // Funkcja sprawdzająca, czy Bluetooth jest włączony.
    fun isBluetoothEnabled(): Boolean {
        return _bluetoothUiState.value.isBluetoothEnabled
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

    fun hideNetworkBanner() {
        networkBannerManager.dismissBanner()
    }

    override fun onCleared() {
        super.onCleared()
        networkBannerManager.stopObserving()
    }
}


fun getCurrentTimestampFormatted(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getDefault()
    return formatter.format(Date())
}



sealed class BluetoothEvent {
    object RequestEnableBluetooth : BluetoothEvent()
}













