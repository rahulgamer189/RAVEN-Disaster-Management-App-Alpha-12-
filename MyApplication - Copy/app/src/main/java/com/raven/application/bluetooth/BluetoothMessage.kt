package com.raven.application.bluetooth

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class BluetoothMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val message: String,
    val messageType: String = "CHAT", // "CHAT", "SOS", "BROADCAST"
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryPercentage: Int? = null,
    val ttl: Int = 16, // Increased from 8 to allow more "hops" across nodes
    val timestamp: Long = System.currentTimeMillis()
) {
    fun relay(maxAgeMs: Long = 24 * 60 * 60 * 1000L): BluetoothMessage? {
        if (ttl <= 0 || System.currentTimeMillis() - timestamp > maxAgeMs) return null
        return copy(ttl = ttl - 1)
    }
}

@Serializable
data class Camp(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val version: Long = System.currentTimeMillis()
)
