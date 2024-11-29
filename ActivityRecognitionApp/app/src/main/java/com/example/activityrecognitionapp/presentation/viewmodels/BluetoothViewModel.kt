package com.example.activityrecognitionapp.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityrecognitionapp.components.network.NetworkBannerManager
import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.data.repository.DataRepository
import com.example.activityrecognitionapp.data.repository.TokenRepository
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.ConnectionResult
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.ClearErrorMessageUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.ConnectToDeviceUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.DisconnectUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.EnableBluetoothUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.StartScanUseCase
import com.example.activityrecognitionapp.domain.usecases.Bluetooth.StopScanUseCase
import com.example.activityrecognitionapp.presentation.states.BluetoothUiState
import com.example.activityrecognitionapp.presentation.states.HomeUiState
import com.example.activityrecognitionapp.utils.Event
import com.example.activityrecognitionapp.utils.EventBus
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

/**
 * ViewModel responsible for managing Bluetooth-related operations, including scanning,
 * connecting, and maintaining the UI state for Bluetooth interactions. It also handles
 * activity data processing and network connectivity status.
 */
@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val startScanUseCase: StartScanUseCase,
    private val stopScanUseCase: StopScanUseCase,
    private val connectToDeviceUseCase: ConnectToDeviceUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    private val enableBluetoothUseCase: EnableBluetoothUseCase,
    private val clearErrorMessageUseCase: ClearErrorMessageUseCase,
    private val bluetoothRepository: BluetoothRepository,
    private val tokenRepository: TokenRepository,
    private val dataRepository: DataRepository,
    private val networkBannerManager: NetworkBannerManager
) : ViewModel() {

    // Job to manage the lifecycle of the device connection coroutine
    private var deviceConnectionJob: Job? = null

    // MutableStateFlow holding the current state of Bluetooth-related UI components
    private val _bluetoothUiState = MutableStateFlow(BluetoothUiState())
    val bluetoothUiState: StateFlow<BluetoothUiState> = _bluetoothUiState.asStateFlow()

    // MutableStateFlow holding the current state of the Home UI components
    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    // MutableSharedFlow for emitting error messages to the UI
    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    // MutableSharedFlow for emitting Bluetooth events, such as requests to enable Bluetooth
    private val _events = MutableSharedFlow<BluetoothEvent>(replay = 1)
    val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()

    // Flag to indicate if the activity chart is currently active
    var isChartActive = false

    // Network connectivity states exposed to the UI
    val isNetworkAvailable: StateFlow<Boolean> = networkBannerManager.isNetworkAvailable
    val showNetworkBanner: StateFlow<Boolean> = networkBannerManager.showNetworkBanner

    // Counter to track the number of times the chart has been reset
    var countToChartReset = 0

    /**
     * Initialization block where various flows are collected to update UI states
     * and handle events such as user logout.
     */
    init {
        // Collect and handle global events from the EventBus
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is Event.Logout -> {
                        resetHomeUiState()
                        resetBluetoothUiState()
                        disconnectFromDevice()
                    }
                }
            }
        }

        // Observe Bluetooth enabled state and update UI accordingly
        viewModelScope.launch {
            bluetoothRepository.isBluetoothEnabled.collect { isEnabled ->
                _bluetoothUiState.update { it.copy(isBluetoothEnabled = isEnabled) }
            }
        }

        // Observe Bluetooth connection state and update UI accordingly
        viewModelScope.launch {
            bluetoothRepository.isConnected.collect { isConnected ->
                _bluetoothUiState.update { it.copy(isConnected = isConnected) }
            }
        }

        // Observe scanned Bluetooth devices and update the UI list
        viewModelScope.launch {
            bluetoothRepository.scannedDevices.collect { scannedDevices ->
                _bluetoothUiState.update { it.copy(scannedDevices = scannedDevices) }
            }
        }

        // Observe Bluetooth errors, update UI state, and emit error messages
        viewModelScope.launch {
            bluetoothRepository.errors.collect { error ->
                _bluetoothUiState.update { it.copy(errorMessage = error) }
                _errorMessages.emit(error)
            }
        }
    }

    /**
     * Activates the activity chart and resets the reset counter.
     */
    fun startChart() {
        viewModelScope.launch {
            isChartActive = true
            countToChartReset = 0
        }
    }

    /**
     * Deactivates the activity chart and increments the reset counter.
     * Resets the chart if the counter reaches a predefined threshold.
     */
    fun stopChart() {
        viewModelScope.launch {
            isChartActive = false
            countToChartReset += 1
            if (countToChartReset == 2) {
                resetChart()
                countToChartReset = 0
            }
        }
    }

    /**
     * Resets the Home UI state to its default values.
     */
    fun resetChart() {
        viewModelScope.launch {
            resetHomeUiState()
        }
    }

    /**
     * Initiates scanning for nearby Bluetooth devices.
     * Clears the current list of scanned devices before starting the scan.
     */
    fun startScan() {
        // Optionally clear the current list of scanned devices
        // _bluetoothUiState.update { it.copy(scannedDevices = emptyList()) }
        startScanUseCase.invoke()
    }

    /**
     * Stops the ongoing Bluetooth device scan.
     */
    fun stopScan() = stopScanUseCase.invoke()

    /**
     * Connects to a specified Bluetooth device.
     *
     * @param device The Bluetooth device to connect to.
     */
    fun connectToDevice(device: BluetoothDeviceDomain) {
        _bluetoothUiState.update { it.copy(isConnecting = true) }
        deviceConnectionJob = connectToDeviceUseCase.invoke(device)
            .listen()
    }

    /**
     * Disconnects from the currently connected Bluetooth device and updates the UI state.
     */
    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        disconnectUseCase.invoke()
        _bluetoothUiState.update {
            it.copy(
                isConnecting = false,
                isConnected = false,
                connectedDevice = null
            )
        }
    }

    /**
     * Listens to the flow of connection results and updates the UI state accordingly.
     *
     * @return A Job representing the listening coroutine.
     */
    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                is ConnectionResult.ConnectionEstablished -> {
                    Log.d("BluetoothViewModel", "Received data: ${result.dataFromBluetooth}")
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
                    if (result.dataFromBluetooth.isNotEmpty()) {
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
                // Handle any unexpected errors by disconnecting and updating the UI
                disconnectUseCase.invoke()
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

    /**
     * Sends activity data to the repository for local storage and synchronization with the server.
     *
     * @param activityType The type of activity detected from the Bluetooth device.
     */
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
                // Save the activity data locally
                dataRepository.saveActivityDataLocally(data)
                Log.d("BluetoothViewModel", "Activity Data saved locally")
                // Attempt to synchronize local changes with the server
                dataRepository.syncLocalChanges()
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Exception when saving or syncing activity data", e)
                _errorMessages.emit("Exception: ${e.message}")
            }
        }
    }

    /**
     * Updates the Home UI state based on the type of activity detected.
     *
     * @param kindActivity The type of activity (e.g., "stand", "walk", "run").
     */
    private fun updateHomeUi(kindActivity: String) {
        if (isChartActive) {
            _homeUiState.update {
                it.copy(
                    stand = it.stand + if (kindActivity.startsWith("stand")) 1 else 0,
                    walk = it.walk + if (kindActivity.startsWith("walk")) 1 else 0,
                    run = it.run + if (kindActivity.startsWith("run")) 1 else 0,
                    unknownActivity = it.unknownActivity + if (kindActivity.startsWith("Unknown Activity")) 1 else 0,
                    total = it.total + 1
                )
            }
        }
    }

    /**
     * Resets the Home UI state to its default values.
     */
    fun resetHomeUiState() {
        _homeUiState.value = HomeUiState()
    }

    /**
     * Resets the Bluetooth UI state to its default values.
     */
    fun resetBluetoothUiState() {
        _bluetoothUiState.value = BluetoothUiState()
    }

    /**
     * Enables Bluetooth on the device and updates the UI state based on the result.
     */
    fun enableBluetooth() {
        viewModelScope.launch {
            val isEnabled = enableBluetoothUseCase()
            _bluetoothUiState.update { it.copy(isBluetoothEnabled = isEnabled) }
        }
    }

    /**
     * Checks if Bluetooth is currently enabled on the device.
     *
     * @return True if Bluetooth is enabled, false otherwise.
     */
    fun isBluetoothEnabled(): Boolean = bluetoothRepository.isBluetoothEnabled()

    /**
     * Clears any existing error messages from the UI state.
     */
    fun clearErrorMessage() = clearErrorMessageUseCase.invoke()

    /**
     * Hides the network connectivity banner from the UI.
     */
    fun hideNetworkBanner() = networkBannerManager.dismissBanner()

    /**
     * Cleans up resources when the ViewModel is cleared.
     * Stops observing network connectivity changes.
     */
    override fun onCleared() {
        super.onCleared()
        networkBannerManager.stopObserving()
    }
}

/**
 * Formats the current timestamp into a standardized string format.
 *
 * @return The formatted current timestamp.
 */
fun getCurrentTimestampFormatted(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getDefault()
    return formatter.format(Date())
}

/**
 * Sealed class representing various Bluetooth-related events that can be emitted
 * to the UI or other components.
 */
sealed class BluetoothEvent {
    /**
     * Event indicating a request to enable Bluetooth on the device.
     */
    object RequestEnableBluetooth : BluetoothEvent()
}
