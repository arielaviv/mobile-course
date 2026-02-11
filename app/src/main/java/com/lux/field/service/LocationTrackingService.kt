package com.lux.field.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.lux.field.R
import com.lux.field.data.repository.LocationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentIntervalMs = NORMAL_INTERVAL_MS

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            Log.d(TAG, "Location: ${location.latitude}, ${location.longitude} acc=${location.accuracy}")
            serviceScope.launch {
                locationRepository.saveLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    altitude = location.altitude,
                    timestamp = location.time,
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_BOOST -> {
                currentIntervalMs = BOOST_INTERVAL_MS
                restartLocationUpdates()
                return START_STICKY
            }
            ACTION_NAV_BOOST -> {
                currentIntervalMs = NAV_BOOST_INTERVAL_MS
                restartLocationUpdates()
                return START_STICKY
            }
            ACTION_NORMAL -> {
                currentIntervalMs = NORMAL_INTERVAL_MS
                restartLocationUpdates()
                return START_STICKY
            }
            else -> {
                startForegroundWithNotification()
                startLocationUpdates()
                return START_STICKY
            }
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, currentIntervalMs)
            .setMinUpdateIntervalMillis(currentIntervalMs / 2)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun restartLocationUpdates() {
        stopLocationUpdates()
        startLocationUpdates()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.location_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.location_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.location_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LocationTracking"
        private const val CHANNEL_ID = "location_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val NORMAL_INTERVAL_MS = 30_000L
        private const val BOOST_INTERVAL_MS = 15_000L
        private const val NAV_BOOST_INTERVAL_MS = 1_000L

        const val ACTION_START = "com.lux.field.action.LOCATION_START"
        const val ACTION_STOP = "com.lux.field.action.LOCATION_STOP"
        const val ACTION_BOOST = "com.lux.field.action.LOCATION_BOOST"
        const val ACTION_NAV_BOOST = "com.lux.field.action.LOCATION_NAV_BOOST"
        const val ACTION_NORMAL = "com.lux.field.action.LOCATION_NORMAL"

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun boostInterval(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_BOOST
            }
            context.startService(intent)
        }

        fun navBoostInterval(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_NAV_BOOST
            }
            context.startService(intent)
        }

        fun normalInterval(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_NORMAL
            }
            context.startService(intent)
        }
    }
}
