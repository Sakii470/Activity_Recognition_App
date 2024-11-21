package com.example.activityrecognitionapp.domain.usecase.Bluetooth

import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * EnableBluetoothUseCase enables Bluetooth on the device.
 */
class EnableBluetoothUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke():Boolean {
        return repository.enableBluetooth()
    }
}