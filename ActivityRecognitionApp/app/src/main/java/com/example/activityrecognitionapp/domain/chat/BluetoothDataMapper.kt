package com.example.activityrecognitionapp.domain.chat

fun String.toBluetoothData(): BluetoothData{
    val name = substringBeforeLast("#")
    val data = substringAfter("H")
    return BluetoothData(
        senderName = name,
        data = data
    )
}

fun BluetoothData.toByteArray(): ByteArray{
    return "$senderName#$data".encodeToByteArray()
}