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
    val readings: List<SensorReading>
)
