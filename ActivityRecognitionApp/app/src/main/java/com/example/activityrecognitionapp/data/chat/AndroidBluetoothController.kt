package com.example.activityrecognitionapp.data.chat

import FoundDeviceReceiver
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.activityrecognitionapp.domain.chat.BluetoothController
import com.example.activityrecognitionapp.domain.chat.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.chat.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID


@Suppress("UNREACHABLE_CODE")
@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
): BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    var bluetoothGatt: BluetoothGatt? = null


    private val leScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi

            if (device.name != null && device.name.isNotEmpty()) {
                val bluetoothDeviceDomain = device.toBluetoothDeviceDomain(rssi)
                _scannedDevices.update { devices ->
                    // Sprawdź, czy urządzenie już istnieje na liście
                    if (devices.any { it.address == bluetoothDeviceDomain.address }) {
                        // Jeśli urządzenie już istnieje, zwróć niezmienioną listę
                        devices
                    } else {
                        // Jeśli urządzenia nie ma na liście, dodaj je
                        devices + bluetoothDeviceDomain
                    }
                }
            }
        }
    }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    val services = MutableStateFlow<List<BluetoothGattService>>(emptyList())
    private val GATT_MAX_MTU_SIZE = 517
    private val connectionResultChannel = Channel<ConnectionResult>(Channel.BUFFERED)


    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain(rssi = null)
            if(newDevice in devices) devices else devices + newDevice
        }
    }

    private var bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.emit("Can't connect to device")
            }
        }
    }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
        )
    }


    override fun startDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        updatePairedDevices()

        bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCallback)
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
    }

//    override fun startBluetoothServer(): Flow<ConnectionResult> {
//        return flow {
//            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//                throw SecurityException("Permission denied")
//
//            }
//            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
//                "Data_service",
//                UUID.fromString(Service_UUID)
//            )
//
//            var shouldLoop = true;
//            while (shouldLoop) {
//                currentClientSocket = try {
//                    currentServerSocket?.accept()
//
//                } catch (e: IOException) {
//                    shouldLoop = false
//                    null
//                }
//                emit(ConnectionResult.ConnectionEstabilished)
//                currentClientSocket?.let {
//                    currentServerSocket?.close()
//                }
//            }
//        }.onCompletion {
//            closeConnection()
//        }.flowOn(Dispatchers.IO)
//    }


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {

                services.value = gatt.services
                connectionResultChannel.trySend(ConnectionResult.ConnectionEstabilished)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionResultChannel.trySend(ConnectionResult.Error("Disconnected"))
                gatt.close()
            } else {
                connectionResultChannel.trySend(ConnectionResult.Error("Error. Try Again."))
                gatt.close()
            }
        }



//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            super.onServicesDiscovered(gatt, status)
//            services.value = gatt.services
//            val services = gatt.services
//            for (service in services) {
//                Log.d("BluetoothGattCallback", "Usługa UUID: ${service.uuid}")
//            }
//        }



//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                with(gatt) {
//                    Log.w(
//                        "BluetoothGattCallback",
//                        "Discovered ${services.size} services for ${device.address}"
//                    )
//                    printGattTable() // See implementation just above this section
//                    // Consider connection setup as complete here
//                }
//            } else {
//                Log.w("BluetoothGattCallback", "Service discovery failed with status ${status}")
//            }
//        }
    }

//    private fun BluetoothGatt.printGattTable() {
//        if (services.isEmpty()) {
//            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
//            return
//        }
//        services.forEach { service ->
//            val characteristicsTable = service.characteristics.joinToString(
//                separator = "\n|--",
//                prefix = "|--"
//            ) { it.uuid.toString() }
//            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
//            )
//        }
//    }
//    val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(
//            gatt: BluetoothGatt,
//            status: Int,
//            newState: Int
//        ) {
//            super.onConnectionStateChange(gatt, status, newState)
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                _isConnected.update { true }
//
//                val device = gatt.device
//                if(device.bondState == BluetoothDevice.BOND_NONE){
//                    Log.d("BondLog","Device not bonded, starting bonding process")
//                    device.createBond()
//                    Log.i(TAG, "Połączono z urządzeniem: ${device?.address}")
//                    gatt.discoverServices()  // Rozpocznij wyszukiwanie usług
//                }
//
//            }
//
//            else {
//                _isConnected.update { false }
//            }
//        }
//    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {

        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("Permission denied")
            }

            bluetoothAdapter?.let { adapter ->
                try {
                    val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
//                    if(bluetoothDevice?.bondState == BluetoothDevice.BOND_BONDED){
//                        //removeBond(device)
//                    }
                    bluetoothGatt = bluetoothDevice?.connectGatt(context, false, gattCallback)

                    for (result in connectionResultChannel) {
                        emit(result) // Emituj wyniki połączenia
                        updateScannedDevices()
                    }
//                    if (device.bondState == BluetoothDevice.BOND_NONE) {
//                        Log.d(TAG, "Starting bonding process with device")
//                        device.createBond()
//                    } else {
//                        Log.d(TAG, "Device already bonded")
//                    }

                } catch (exception: IllegalArgumentException) {
                    Log.w(TAG, "Device not found with provided address.")
                }
                // connect to the GATT server on the device
            } ?: run {
                Log.w(TAG, "BluetoothAdapter not initialized")
            }
        }



//        return flow {
//            if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//                throw SecurityException("Permission denied")
//            }
//
//            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
//
//
//            currentClientSocket= bluetoothDevice
//
//                ?.createRfcommSocketToServiceRecord(
//                    UUID.fromString(Service_UUID)
//                )
//
//            stopDiscovery()
//
//            if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == false){
//                    Log.d("XXXX", "XXXXX")
//            }
//
//            currentClientSocket?.let { socket ->
//                try {
//                    socket.connect()
//                    emit(ConnectionResult.ConnectionEstabilished)
//
//                } catch (e: IOException) {
//                    socket.close()
//                    currentClientSocket = null
//                    emit(ConnectionResult.Error("Connection was interapted"))
//                }
//            }
//        }.onCompletion {
//            closeConnection()
//        }.flowOn(Dispatchers.IO)

//        return flow {
//            // Sprawdzanie uprawnień
//            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//                throw SecurityException("Permission denied")
//            }
//
//            // Pobranie urządzenia z adresu
//            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
//            if (bluetoothDevice == null) {
//                emit(ConnectionResult.Error("Device not found"))
//                return@flow
//            }
//
//            // Próba połączenia GATT
//            bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)
//
//
//
//            // Oczekiwanie na zmianę stanu w GATT Callback
//            // (GATT Callback powinien obsługiwać zmiany stanu połączenia)
//            emit(ConnectionResult.ConnectionEstabilished)
//
//        }.catch { e ->
//            // Obsługa błędów
//            emit(ConnectionResult.Error(e.message ?: "Unknown error"))
//        }.onCompletion {
//            // Zamknięcie połączenia w przypadku zakończenia
//            closeConnection()
//        }.flowOn(Dispatchers.IO)
    }


    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket= null
        currentServerSocket= null
    }

//    override fun release() {
//        context.unregisterReceiver(foundDeviceReceiver)
//    }

    private fun updatePairedDevices() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?.also { devices ->
                _pairedDevices.update { devices }
            }
    }

    private fun updateScannedDevices(){
        _scannedDevices.update { emptyList() }
        startDiscovery()
    }

//    fun removeBond(device: BluetoothDeviceDomain) {
//        try {
//            device::class.java.getMethod("removeBond").invoke(device)
//        } catch (e: Exception) {
//            Log.e(TAG, "Removing bond has been failed. ${e.message}")
//        }
//    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val Service_UUID = "020c6211-64f6-4e6d-82e8-c2c1391f75fa"
    }
}