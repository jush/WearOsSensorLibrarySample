package com.soy.libraryarchitecturesample

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.widget.TextView
import com.soy.sensorlibrary.service.SensorService

private const val TAG = "MainActivity"

class MainActivity : WearableActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        setAmbientEnabled()

        startSensorService()
    }

    // This could be called also outside the activity (for example in DemoApp.onCreate) since it
    // does not depend on anything from activity itself
    private fun startSensorService() {
        Log.d(TAG, "onCreate: Starting foreground service...")
        SensorService.startService(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        ConsumerListener.setTextView(findViewById<TextView>(R.id.text))
    }

    override fun onPause() {
        // We don't want to create a memory leak so we need to remove the textview reference
        ConsumerListener.setTextView(null)
        super.onPause()
    }

    override fun onDestroy() {
        stopSensorService()
        super.onDestroy()
    }

    // This could be called also outside the activity since it does not depend on anything from
    // activity itself or even never and let Android kill the process
    private fun stopSensorService() {
        Log.d(TAG, "onCreate: Starting foreground service...")
        SensorService.stopService(applicationContext)
    }

}