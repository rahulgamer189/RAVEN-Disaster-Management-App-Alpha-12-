package com.raven.application.bluetooth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

class TelemetryProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getTelemetry(highAccuracy: Boolean = false): TelemetryData {
        val location = try {
            val priority = if (highAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            val task = fusedLocationClient.getCurrentLocation(priority, null)
            Tasks.await(task, 5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        val batteryPct: Int? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        }

        return TelemetryData(
            latitude = location?.latitude,
            longitude = location?.longitude,
            batteryPercentage = batteryPct
        )
    }
}

data class TelemetryData(
    val latitude: Double?,
    val longitude: Double?,
    val batteryPercentage: Int?
)
