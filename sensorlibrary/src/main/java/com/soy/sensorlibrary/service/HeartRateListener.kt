package com.soy.sensorlibrary.service

interface HeartRateListener {
    fun onHeartRate(eventTimestamp: Long, bpm: Int)
}