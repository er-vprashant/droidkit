package com.prashant.droidkit.core.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

internal object ShakeDetector : SensorEventListener {

    private const val THRESHOLD = 12f
    private const val COOLDOWN_MS = 1000L
    private var lastShakeTime = 0L
    private var onShake: (() -> Unit)? = null
    private var sensorManager: SensorManager? = null

    fun start(context: Context, enabled: Boolean, callback: () -> Unit) {
        if (!enabled) return
        onShake = callback
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        onShake = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()
        if (acceleration > THRESHOLD && now - lastShakeTime > COOLDOWN_MS) {
            lastShakeTime = now
            onShake?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
