package com.soy.libraryarchitecturesample

import android.app.Application
import com.soy.sensorlibrary.service.SensorService

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // This can be done anywhere because ConsumerListener is a Singleton that lives for
        // as long as this process is alive
        SensorService.setHeartRateListener(ConsumerListener)
    }
}