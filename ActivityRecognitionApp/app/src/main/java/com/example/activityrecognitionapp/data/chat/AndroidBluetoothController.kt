package com.example.activityrecognitionapp.data.chat

import FoundDeviceReceiver
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ContentValues.TAG
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.activityrecognitionapp.domain.chat.BluetoothController
import com.example.activityrecognitionapp.domain.chat.BluetoothDataTransferService
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.activityrecognitionapp.domain.chat.ConnectionResult as ConnectionResult1


@Suppress("UNREACHABLE_CODE")
@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    var bluetoothGatt: BluetoothGatt? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // override val data: MutableSharedFlow<ConnectionResult<ActivityRecognitionResult>> = MutableSharedFlow()


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

    private val UUID_Service: UUID = UUID.fromString("020c6211-64f6-4e6d-82e8-c2c1391f75fa")
    private val UUID_Characteristic: UUID = UUID.fromString("020c6213-64f6-4e6d-82e8-c2c1391f75fa")

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

    private val connectionResultChannel = Channel<ConnectionResult1>(Channel.BUFFERED)

    private var dataTransferService: BluetoothDataTransferService? = null


    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain(rssi = null)
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    private var bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                //         _errors.emit("Can't connect to device")
            }
        }
    }

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
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        updatePairedDevices()
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCallback)
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()

                gatt.discoverServices()
                //gatt.readCharacteristic(cha)
               // connectionResultChannel.trySend(ConnectionResult.ConnectionEstabilished("xxDdddd"))


                //readCharacteristic(BluetoothGattCharacteristic(UUID_Characteristic,0,0))
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionResultChannel.trySend(ConnectionResult1.Error("Disconnected"))
                gatt.close()
            } else {
                connectionResultChannel.trySend(ConnectionResult1.Error("Error.Try Again."))
                gatt.close()
            }
        }

//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            super.onServicesDiscovered(gatt, status)
//            gatt.requestMtu(517)
//
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                val services = gatt.services
//                Log.d(TAG, "Znalezione usługi: ${services.size}")
//
//                for (service in services) {
//                    Log.d(TAG, "Usługa UUID: ${service.uuid}")
//                    for (characteristic in service.characteristics) {
//                        Log.d(TAG, "  Charakterystyka UUID: ${characteristic.uuid}")
//                        Log.d(TAG, "    Właściwości: ${characteristic.properties}")
//
//                        // Sprawdź, czy charakterystyka obsługuje odczyt
//                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
//                            // Odczytaj wartość
//                            gatt.readCharacteristic(characteristic)
//                        } else {
//                            Log.e(TAG, "Charakterystyka nie obsługuje odczytu.")
//                        }
//                    }
//                }
//            } else {
//                Log.e(TAG, "Nie udało się odkryć usług, status: $status")
//            }
//        }


//                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            super.onServicesDiscovered(gatt, status)
//            services.value = gatt.services
//            val services = gatt.services
//            for (service in services) {
//                Log.d("BluetoothGattCallback", "Usługa UUID: ${service.uuid}")
//            }
//        }

//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            super.onServicesDiscovered(gatt, status)
//
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                val services = gatt.services
//
//                // Wypisz znalezione usługi
//                Log.d(TAG, "Znalezione usługi:")
//                for (service in services) {
//                    Log.d(TAG, "Usługa UUID: ${service.uuid}")
//                    // Wypisz charakterystyki dla tej usługi
//                    for (characteristic in service.characteristics) {
//                        Log.d(TAG, "  Charakterystyka UUID: ${characteristic.uuid}")
//                        // Opcjonalnie, wypisz uprawnienia i właściwości charakterystyki
//                        Log.d(TAG, "    Właściwości: ${characteristic.properties}")
//                    }
//                }
//            } else {
//                Log.e(TAG, "Nie udało się odkryć usług, status: $status")
//            }
//        }


//        override fun onCharacteristicRead(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            status: Int
//        ) {
//            super.onCharacteristicRead(gatt, characteristic, status)
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                // Pobierz wartość z charakterystyki
//                val value = characteristic.value
//                // Przetwórz wartość w zależności od typu danych
//                if (value != null) {
//                    // Możesz przekształcić wartość w odpowiedni format, np. ByteArray na String
//                    val stringValue = value.decodeToString() // lub inna metoda konwersji
//                    Log.d(TAG, "Odczytana wartość charakterystyki: $stringValue")
//                } else {
//                    Log.e(TAG, "Odczytana wartość jest pusta")
//                }
//            } else {
//                Log.e(TAG, "Nie udało się odczytać charakterystyki, status: $status")
//            }
//        }


//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            super.onServicesDiscovered(gatt, status)
//            services.value = gatt.services
//            val services = gatt.services
//            for (service in services) {
//                Log.d("BluetoothGattCallback", "Usługa UUID: ${service.uuid}")
//            }
//        }


        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                printGattTable()
                coroutineScope.launch {
                    // data.emit(Resource.Loading(message = "Adjusting MTU space..."))
                }
                gatt.requestMtu(517)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val characteristic =
                findCharacteristics(UUID_Service.toString(), UUID_Characteristic.toString())
            if (characteristic == null) {
                coroutineScope.launch {
                    // data.emit(Resource.Error(errorMessage = "Could not find temp and humidity publisher"))
                }
                return
            }
            enableNotification(characteristic)
        }

//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            value: ByteArray
//        ) {
//            super.onCharacteristicChanged(gatt, characteristic, value)
//        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                when (uuid) {
                    UUID_Characteristic -> {
                        val datafromBluetooth = value
                            coroutineScope.launch {
                                connectionResultChannel.trySend(ConnectionResult.ConnectionEstabilished(datafromBluetooth.decodeToString()))
                        }
                    }else -> Unit
                }
            }
        }
    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false){
                Log.d("BLEReceiveManager","set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray){
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    private fun findCharacteristics(serviceUUID: String, characteristicsUUID:String):BluetoothGattCharacteristic?{
        return bluetoothGatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }





//        @Suppress("DEPRECATION")
//        override fun onCharacteristicRead(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            status: Int
//        ) {
//            super.onCharacteristicRead(gatt, characteristic, status)
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                // Retrieve the value from the characteristic
//                val value = characteristic.value
//                // Convert the ByteArray to your BluetoothData type
//                val bluetoothData = value.decodeToString().toBluetoothData()
//                Log.e("BluetoothData", bluetoothData.toString())
//                connectionResultChannel.trySend(
//                    ConnectionResult.ConnectionEstabilished(
//                        bluetoothData.toString()
//                    )
//                )
//                // Emit the data to your flow or handle it accordingly
//
//            } else {
//                // Handle the error case if needed
//                Log.e("BluetoothGattCallback", "Failed to read characteristic: $status")
//            }
//        }


        override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {

            return flow {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    throw SecurityException("Permission denied")
                }

                bluetoothAdapter?.let { adapter ->
                    try {
                        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)

                        bluetoothGatt =
                            bluetoothDevice?.connectGatt(context, false, gattCallback)

                        for (result in connectionResultChannel) {
                            emit(result) // Emit Connection outcomes
                            updateScannedDevices()
                        }


                    } catch (exception: IllegalArgumentException) {

                        emit(ConnectionResult1.Error("Device not found with provided address."))

                    }
                    // connect to the GATT server on the device
                } ?: run {
                    emit(ConnectionResult1.Error("BluetoothAdapter not initialized"))
                }
            }
        }


        override fun closeConnection() {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        }

//    override fun release() {
//        context.unregisterReceiver(foundDeviceReceiver)
//    }

        private fun updatePairedDevices() {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                return
            }
            bluetoothAdapter
                ?.bondedDevices
                ?.map { it.toBluetoothDeviceDomain() }
                ?.also { devices ->
                    _pairedDevices.update { devices }
                }
        }

        private fun updateScannedDevices() {
            _scannedDevices.update { emptyList() }
            startDiscovery()
        }

        fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
            bluetoothGatt?.let { gatt ->
                val value = gatt.readCharacteristic(characteristic)
                Log.d("Chara", value.toString())
            } ?: run {
                Log.w(TAG, "BluetoothGatt not initialized")
            }
        }

        private fun hasPermission(permission: String): Boolean {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }




