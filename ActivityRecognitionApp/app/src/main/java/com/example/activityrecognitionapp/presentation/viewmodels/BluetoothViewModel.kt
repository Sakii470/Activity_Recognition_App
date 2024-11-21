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
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.ClearErrorMessageUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.ConnectToDeviceUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.DisconnectUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.EnableBluetoothUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.StartScanUseCase
import com.example.activityrecognitionapp.domain.usecase.Bluetooth.StopScanUseCase
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


// ViewModel managed by Hilt responsible for Bluetooth application logic, including connections, scanning, and managing UI state.
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
    private var deviceConnectionJob: Job? = null

    // UI state
    private val _bluetoothUiState = MutableStateFlow(BluetoothUiState())
    val bluetoothUiState: StateFlow<BluetoothUiState> = _bluetoothUiState.asStateFlow()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    private val _events = MutableSharedFlow<BluetoothEvent>(replay = 1)
    val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()

//    private var _isChartActive =  MutableStateFlow(false)
//    val isChartActive: StateFlow<Boolean> = _isChartActive.asStateFlow()

    var isChartActive = false
    //var isChartPaused by remember { mutableStateOf(false) }

    val isNetworkAvailable: StateFlow<Boolean> = networkBannerManager.isNetworkAvailable
    val showNetworkBanner: StateFlow<Boolean> = networkBannerManager.showNetworkBanner

    var countToChartReset = 0

    init {

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

        // Collect isBluetoothEnabled
        viewModelScope.launch {
            bluetoothRepository.isBluetoothEnabled.collect { isEnabled ->
                _bluetoothUiState.update { it.copy(isBluetoothEnabled = isEnabled) }
            }
        }

        // Collect isConnected
        viewModelScope.launch {
            bluetoothRepository.isConnected.collect { isConnected ->
                _bluetoothUiState.update { it.copy(isConnected = isConnected) }
            }
        }

        // Collect scannedDevices
        viewModelScope.launch {
            bluetoothRepository.scannedDevices.collect { scannedDevices ->
                _bluetoothUiState.update { it.copy(scannedDevices = scannedDevices) }
            }
        }

        // Collect errors
        viewModelScope.launch {
            bluetoothRepository.errors.collect { error ->
                _bluetoothUiState.update { it.copy(errorMessage = error) }
                _errorMessages.emit(error)
            }
        }
    }


    fun startChart() {
        viewModelScope.launch {
            isChartActive = true
            countToChartReset = 0
        }
    }

    // Opcjonalna funkcja do wyłączania wykresu (jeśli potrzebna)
    fun stopChart() {
        viewModelScope.launch {
            isChartActive = false
            countToChartReset += 1
            if(countToChartReset == 2){
                resetChart()
                countToChartReset = 0
            }
        }
    }

    fun resetChart(){
        viewModelScope.launch {
            resetHomeUiState()
        }
    }


    // Function to start scanning for Bluetooth devices
    fun startScan() {
        // Update the state with an empty list
       // _bluetoothUiState.update { it.copy(scannedDevices = emptyList()) }
        // Start scanning for devices
        startScanUseCase.invoke()
    }

    // Function to stop scanning for Bluetooth devices
    fun stopScan() = stopScanUseCase.invoke()

    // Function to connect to a Bluetooth device
    fun connectToDevice(device: BluetoothDeviceDomain) {
        _bluetoothUiState.update { it.copy(isConnecting = true) }
        deviceConnectionJob = connectToDeviceUseCase.invoke(device)
            .listen()
    }

    // Function to disconnect from the current Bluetooth device
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

    // Function to listen for connection results
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
                    if (result.dataFromBluetooth.isNotEmpty() && result.dataFromBluetooth != null ) {
                        updateHomeUi(result.dataFromBluetooth)
                    }
                    else {
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

    // Function to send activity data to the repository
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
                dataRepository.saveActivityDataLocally(data)
                Log.d("BluetoothViewModel", "Activity Data saved locally")
                // Then try to sync local changes
                dataRepository.syncLocalChanges()
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Exception when saving or syncing activity data", e)
                _errorMessages.emit("Exception: ${e.message}")
            }
        }
    }

    // Function to update the Home UI state based on activity type
    private fun updateHomeUi(kindActivity: String) {
        if (isChartActive) {
            _homeUiState.update {
                it.copy(
                    stand = it.stand + if (kindActivity.startsWith("stand")) 1 else 0,
                    walk = it.walk + if (kindActivity.startsWith("walk")) 1 else 0,
                    run = it.run + if (kindActivity.startsWith("run")) 1 else 0,
                    total = it.total + 1
                )
            }
        }
    }

    // Function to reset the Home UI state
    fun resetHomeUiState() {
        _homeUiState.value = HomeUiState()
    }

    fun resetBluetoothUiState() {
        _bluetoothUiState.value = BluetoothUiState()
    }

    // Function to enable Bluetooth
    fun enableBluetooth() {
        viewModelScope.launch {
            val isEnabled = enableBluetoothUseCase()
            if(isEnabled){
                _bluetoothUiState.update { it.copy(isBluetoothEnabled = true ) }
            }
            else{
                _bluetoothUiState.update { it.copy(isBluetoothEnabled = false ) }
            }

        }
    }


    // Function to check if Bluetooth is enabled
    fun isBluetoothEnabled(): Boolean = bluetoothRepository.isBluetoothEnabled()

    // Function to clear error messages
    fun clearErrorMessage() = clearErrorMessageUseCase.invoke()

    // Function to hide network banner
    fun hideNetworkBanner() = networkBannerManager.dismissBanner()

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



//    private var deviceConnectionJob: Job? = null
//
//    // Bluetooth UI State
//    private val _bluetoothUiState = MutableStateFlow(BluetoothUiState())
//    val bluetoothUiState: StateFlow<BluetoothUiState> = _bluetoothUiState.asStateFlow()
//
//    private val _homeUiState = MutableStateFlow(HomeUiState())
//    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()
//
//    private val _errorMessages = MutableSharedFlow<String>()
//    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()
//
//    private val _events = MutableSharedFlow<BluetoothEvent>(replay = 1)
//    val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()
//
//    val isNetworkAvailable: StateFlow<Boolean> = networkBannerManager.isNetworkAvailable
//    val showNetworkBanner: StateFlow<Boolean> = networkBannerManager.showNetworkBanner
//
//
//
//    init {
//        viewModelScope.launch {
//            EventBus.events.collect { event ->
//                when (event) {
//                    is Event.Logout -> {
//                        resetHomeUiState()
//                    }
//                }
//            }
//        }
//
//
//        // Subskrypcja isBluetoothEnabled
//        viewModelScope.launch {
//            bluetoothRepository.isBluetoothEnabled.collect { isEnabled ->
//                _bluetoothUiState.update { it.copy(isBluetoothEnabled = isEnabled) }
//            }
//        }
//
//        // Subskrypcja isConnected
//        viewModelScope.launch {
//            bluetoothRepository.isConnected.collect { isConnected ->
//                _bluetoothUiState.update { it.copy(isConnected = isConnected) }
//            }
//        }
//
//        // Subskrypcja scannedDevices
//        viewModelScope.launch {
//            bluetoothRepository.scannedDevices.collect { scannedDevices ->
//                _bluetoothUiState.update { it.copy(scannedDevices = scannedDevices) }
//            }
//        }
//
//        // Subskrypcja błędów
//        viewModelScope.launch {
//            bluetoothRepository.errors.collect { error ->
//                _bluetoothUiState.update { it.copy(errorMessage = error) }
//                _errorMessages.emit(error)
//            }
//        }
//    }
//
//
//    // Funkcja odpowiedzialna za nawiązanie połączenia z urządzeniem Bluetooth.
//    fun connectToDevice(device: BluetoothDeviceDomain) {
//        _bluetoothUiState.update { it.copy(isConnecting = true) }
//        deviceConnectionJob = bluetoothRepository
//            .connectToDevice(device)
//            .listen()
//    }
//
//    // Funkcja odpowiedzialna za rozłączenie z bieżącym urządzeniem Bluetooth.
//    fun disconnectFromDevice() {
//        deviceConnectionJob?.cancel()
//        bluetoothRepository.closeConnection()
//        _bluetoothUiState.update {
//            it.copy(
//                isConnecting = false,
//                isConnected = false,
//                connectedDevice = null
//            )
//        }
//    }
//
//    fun resetHomeUiState() {
//        _homeUiState.value = HomeUiState()
//    }
//
//    // Funkcja do rozpoczęcia skanowania urządzeń Bluetooth.
//    fun startScan() {
//        bluetoothRepository.startDiscovery()
//    }
//
//    // Funkcja do zatrzymania skanowania urządzeń Bluetooth.
//    fun stopScan() {
//        bluetoothRepository.stopDiscovery()
//    }
//
//    // Funkcja do nasłuchiwania wyników połączenia Bluetooth i ich obsługi.
//    private fun Flow<ConnectionResult>.listen(): Job {
//        return onEach { result ->
//            when (result) {
//                is ConnectionResult.ConnectionEstablished -> {
//                    Log.d("BluetoothViewModel", "Updating dataFromBluetooth to: ${result.dataFromBluetooth}")
//                    sendActivityData(result.dataFromBluetooth)
//
//                    _bluetoothUiState.update {
//                        it.copy(
//                            isConnected = true,
//                            isConnecting = false,
//                            errorMessage = null,
//                            dataFromBluetooth = result.dataFromBluetooth,
//                            connectedDevice = result.connectedDevice,
//                        )
//                    }
//                    if (result.dataFromBluetooth != null) {
//                        updateHomeUi(result.dataFromBluetooth)
//                    } else {
//                        updateHomeUi("Connect to Bluetooth Device!")
//                    }
//                }
//                is ConnectionResult.Error -> {
//                    _bluetoothUiState.update {
//                        it.copy(
//                            isConnected = false,
//                            isConnecting = false,
//                            errorMessage = result.message
//                        )
//                    }
//                    _errorMessages.emit(result.message)
//                }
//            }
//        }
//            .catch { throwable ->
//                bluetoothRepository.closeConnection()
//                _bluetoothUiState.update {
//                    it.copy(
//                        isConnected = false,
//                        isConnecting = false,
//                        errorMessage = throwable.message
//                    )
//                }
//                _errorMessages.emit(throwable.message ?: "Unknown error")
//            }
//            .launchIn(viewModelScope)
//    }
//
//    // Funkcja do wysyłania danych aktywności do Supabase.
//    private fun sendActivityData(activityType: String) {
//        viewModelScope.launch {
//            val userId = tokenRepository.getUserId()
//            if (userId == null) {
//                Log.e("BluetoothViewModel", "User ID is null. Cannot send activity data.")
//                _errorMessages.emit("User not logged in.")
//                return@launch
//            }
//
//            val timestamp = getCurrentTimestampFormatted()
//            val data = ActivityDataSupabase(
//                user_id = userId,
//                activity_type = activityType.substringBefore("="),
//                timestamp = timestamp
//            )
//
//            try {
//                // Save data locally first
//                repository.saveActivityDataLocally(data)
//                Log.d("BluetoothViewModel", "Activity Data saved locally")
//
//                // Then try to sync local changes
//                    repository.syncLocalChanges()
//
//            } catch (e: Exception) {
//                Log.e("BluetoothViewModel", "Exception when saving or syncing activity data", e)
//                _errorMessages.emit("Exception: ${e.message}")
//            }
//        }
//    }
//
//    // Funkcja aktualizująca stan HomeUiState na podstawie rodzaju aktywności.
//    private fun updateHomeUi(kindActivity: String) {
//        _homeUiState.update {
//            it.copy(
//                stand = it.stand + if (kindActivity.startsWith("stand")) 1 else 0,
//                walk = it.walk + if (kindActivity.startsWith("walk")) 1 else 0,
//                run = it.run + if (kindActivity.startsWith("run")) 1 else 0,
//                total = it.total + 1
//            )
//        }
//    }
//
//    // Emitowanie zdarzenia żądania włączenia Bluetooth
//
//
//    fun enableBluetooth() {
//        viewModelScope.launch {
//            Log.d("BluetoothViewModel", "emit from enableBluetooth")
//            _events.emit(BluetoothEvent.RequestEnableBluetooth)
//        }
//    }
//
//    // Funkcja sprawdzająca, czy Bluetooth jest włączony.
//    fun isBluetoothEnabled(): Boolean {
//        return _bluetoothUiState.value.isBluetoothEnabled
//    }
//
//    // Funkcja obsługująca niepowodzenie włączenia Bluetooth.
//    fun onBluetoothEnableFailed() {
//        viewModelScope.launch {
//            _errorMessages.emit("Enabling Bluetooth failed.")
//        }
//    }
//
//    // Funkcja czyszcząca komunikaty błędów.
//    fun clearErrorMessage() {
//        viewModelScope.launch {
//            _bluetoothUiState.update { it.copy(errorMessage = null) }
//        }
//    }
//
//    fun hideNetworkBanner() {
//        networkBannerManager.dismissBanner()
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        networkBannerManager.stopObserving()
//    }
//
//
//}
//
//fun getCurrentTimestampFormatted(): String {
//    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
//    formatter.timeZone = TimeZone.getDefault()
//    return formatter.format(Date())
//}
//
//
//sealed class BluetoothEvent {
//    object RequestEnableBluetooth : BluetoothEvent()
//}













