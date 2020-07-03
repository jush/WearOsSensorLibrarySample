package com.soy.libraryarchitecturesample

import android.util.Log
import android.widget.TextView
import com.soy.sensorlibrary.service.HeartRateListener
import java.time.Instant
import java.time.ZoneId

private typealias Timestamp = Long

private typealias BPM = Int

private const val TAG = "ConsumerListener"

/**
 * Own heart rate listener implementation that redirects the heart rate values to at TextView if set.
 *
 * Other implementation of a [HeartRateListener] could for example create a notification or store
 * the values.
 */
object ConsumerListener : HeartRateListener {
    private val zone = ZoneId.systemDefault()

    private var textView: TextView? = null

    private var lastKnownValue: Pair<Timestamp, BPM> = 0L to 0

    override fun onHeartRate(eventTimestamp: Long, bpm: Int) {
        Log.d(TAG, "onHeartRate() called with: eventTimestamp = $eventTimestamp, bpm = $bpm")
        lastKnownValue = eventTimestamp to bpm
        updateTextViewIfAvailable()
    }

    fun setTextView(textView: TextView?) {
        this.textView = textView
        // Immediately populate the last known value
        updateTextViewIfAvailable()
    }

    private fun updateTextViewIfAvailable() {
        textView?.run {
            Log.d(TAG, "updating text view...")
            val eventTime = Instant.ofEpochMilli(lastKnownValue.first).atZone(zone).toLocalTime()
            text = "${lastKnownValue.second} at $eventTime"
        }
    }
}