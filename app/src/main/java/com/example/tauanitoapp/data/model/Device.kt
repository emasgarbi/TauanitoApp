package com.example.tauanitoapp.data.model

data class SensorReading(
    val name: String,
    val value: String,
    val unit: String?
)

enum class BatteryLevel { EMPTY, LOW, HALF, FULL, UNKNOWN }

data class Device(
    val id: String,
    val name: String,
    val customer: String?,      // estratto da "modello - Cliente" nel card
    val timestamp: String?,
    val voltage: String?,
    val batteryLevel: BatteryLevel,
    val readings: List<SensorReading>,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class HistoryRecord(
    val timestamp: String,
    val readings: List<SensorReading>
)

data class DeviceHistory(
    val deviceId: String,
    val deviceName: String,
    val records: List<HistoryRecord>
)
