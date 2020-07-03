package com.soy.sensorlibrary.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.soy.sensorlibrary.R
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val TAG = "SensorService"
private const val NOTIFICATION_ID = 101

/**
 * wake-up sensors ensure that their data is delivered independently of the state of the SoC.
 * While the SoC is awake, the wake-up sensors behave like non-wake-up-sensors.
 * When the SoC is asleep, wake-up sensors must wake up the SoC to deliver events.
 * They must still let the SoC go into suspend mode, but must also wake it up when an event needs to be reported.
 * That is, the sensor must wake the SoC up and deliver the events before the maximum reporting latency has elapsed or the hardware FIFO gets full.
 *
 * Taken from: https://source.android.com/devices/sensors/suspend-mode#wake-up_sensors
 */
private const val WAKE_UP_SENSOR = true

private val ONE_MINUTE_IN_US = TimeUnit.MINUTES.toMicros(1).toInt()
private val ONE_SECOND_IN_US = TimeUnit.SECONDS.toMicros(1).toInt()

/**
 * To start and stop this service please use [SensorService.Companion.startService] and [SensorService.Companion.stopService]
 */
class SensorService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() called")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind() called with: intent = $intent")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "onStartCommand() called with: intent = $intent, flags = $flags, startId = $startId"
        )
        when (intent?.action) {
            ACTION_START -> {
                escalateToForegroundService()
                startListeningHeartRate()
            }
            ACTION_STOP -> {
                stopListeningHeartRate()
                stopForeground(true)
                stopSelf()
            }
            else -> Log.e(TAG, "onStartCommand: Invalid action")
        }

        // We don't have means to recover from the system killing us so let's set it to NOT_STICKY
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
    }

    private fun startListeningHeartRate() {
        val sensorManager = getSystemService(SensorManager::class.java)
        val hrSensor: Sensor? =
            sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, WAKE_UP_SENSOR)
        if (hrSensor == null) {
            Log.e(TAG, "startListeningHeartRate: Couldn't register HR sensor")
            return
        }
        // Request values every second but buffer for maximum of one minute
        sensorManager.registerListener(
            HeartRateSensorListener, hrSensor, ONE_SECOND_IN_US,
            ONE_MINUTE_IN_US
        )
    }

    private fun stopListeningHeartRate() {
        val sensorManager = getSystemService(SensorManager::class.java)
        sensorManager.unregisterListener(HeartRateSensorListener)
    }

    private fun escalateToForegroundService() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channelID = "SensorServiceChannelId"
        val channel =
            NotificationChannel(
                channelID,
                "SensorServiceChannel",
                NotificationManager.IMPORTANCE_LOW
            )

        notificationManager.createNotificationChannel(channel)
        val notification: Notification = NotificationCompat.Builder(applicationContext, channelID)
            .setContentTitle("Sensor Service")
            .setContentText("\uD83D\uDCA4")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        this.startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val ACTION_START: String = "START"
        private const val ACTION_STOP: String = "STOP"

        internal var externalListener: HeartRateListener? = null

        @JvmStatic
        fun setHeartRateListener(listener: HeartRateListener) {
            externalListener = listener
        }

        @JvmStatic
        fun startService(context: Context) {
            val startIntent = Intent(context, SensorService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(startIntent)
        }

        @JvmStatic
        fun stopService(context: Context) {
            val startIntent = Intent(context, SensorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startForegroundService(startIntent)
        }
    }
}

object HeartRateSensorListener : SensorEventListener2 {
    private val zone = ZoneId.systemDefault()
    private val bootTime = Instant.now().minusNanos(SystemClock.elapsedRealtimeNanos())
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged() called with: sensor = $sensor, accuracy = $accuracy")
    }

    override fun onFlushCompleted(sensor: Sensor?) {
        Log.d(TAG, "onFlushCompleted() called with: sensor = $sensor")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val eventTimestamp = bootTime.plusNanos(event.timestamp)
        val eventTime = eventTimestamp.atZone(zone)
        Log.d(TAG, "onSensorChanged() called with: event values ${event.values[0]} at $eventTime")

        // Notify external parties
        SensorService.externalListener?.run {
            this.onHeartRate(eventTimestamp.toEpochMilli(), event.values[0].toInt())
        }
    }

}
