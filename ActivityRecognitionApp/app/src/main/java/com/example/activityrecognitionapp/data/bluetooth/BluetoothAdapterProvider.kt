package com.example.activityrecognitionapp.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothAdapterProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Zwraca instancję BluetoothAdapter.
     *
     * @return BluetoothAdapter? Instancja BluetoothAdapter lub null, jeśli Bluetooth nie jest dostępny.
     */
    fun getBluetoothAdapter(): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter
    }





}