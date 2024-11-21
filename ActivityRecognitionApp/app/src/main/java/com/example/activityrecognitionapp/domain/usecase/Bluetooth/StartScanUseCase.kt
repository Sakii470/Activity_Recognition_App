package com.example.activityrecognitionapp.domain.usecase.Bluetooth

import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * StartScanUseCase initiates scanning for Bluetooth devices.
 */
class StartScanUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke() {
        repository.startScan()
    }
}