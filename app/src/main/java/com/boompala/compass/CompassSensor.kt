package com.boompala.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Display
import android.view.Surface
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

data class SensorCompassState(
    val heading: Float? = null,
    val accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val available: Boolean = true,
    val message: String? = null,
    val tilted: Boolean = false,
    val magneticInterference: Boolean = false,
)

/** Keeps sensor-rate callbacks from driving more Compose frames than the display needs. */
internal class CompassUiUpdateLimiter {
    private var lastPublishedAtNanos: Long? = null

    fun shouldPublish(nowNanos: Long): Boolean {
        val last = lastPublishedAtNanos
        if (last != null && nowNanos - last < MIN_INTERVAL_NANOS) return false
        lastPublishedAtNanos = nowNanos
        return true
    }

    private companion object {
        const val MIN_INTERVAL_NANOS = 1_000_000_000L / 15L
    }
}

/** Sensor adapter kept separate from CompassMath so tests never require an Android sensor. */
class CompassSensorController(
    private val manager: SensorManager,
    private val display: Display?,
    private val onState: (SensorCompassState) -> Unit,
) : SensorEventListener, DefaultLifecycleObserver {
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetic = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gravity = FloatArray(3)
    private val field = FloatArray(3)
    private var haveGravity = false
    private var haveField = false
    private var registered = false
    private var smoothed: Float? = null
    private var lastAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
    private val uiUpdateLimiter = CompassUiUpdateLimiter()

    override fun onResume(owner: LifecycleOwner) = start()
    override fun onPause(owner: LifecycleOwner) = stop()

    fun start() {
        if (registered) return
        val available = (accelerometer != null && magnetic != null) || rotation != null
        if (!available) {
            onState(SensorCompassState(available = false, message = "此设备没有可用的指南针传感器"))
            return
        }
        val delay = SensorManager.SENSOR_DELAY_UI
        val registeredOk = if (accelerometer != null && magnetic != null) {
            manager.registerListener(this, accelerometer, delay) && manager.registerListener(this, magnetic, delay)
        } else if (rotation != null) {
            manager.registerListener(this, rotation, delay)
        } else false
        registered = registeredOk
        if (!registeredOk) onState(SensorCompassState(available = false, message = "无法启动指南针传感器"))
    }

    fun stop() {
        if (registered) manager.unregisterListener(this)
        registered = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        lastAccuracy = accuracy
        onState(SensorCompassState(heading = smoothed, accuracy = accuracy, available = true))
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> { copyLowPass(gravity, event.values); haveGravity = true }
            Sensor.TYPE_MAGNETIC_FIELD -> { copyLowPass(field, event.values); haveField = true }
        }
        val matrix = FloatArray(9)
        val ok = if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            true
        } else if (haveGravity && haveField) SensorManager.getRotationMatrix(matrix, null, gravity, field) else false
        if (!ok) return
        val remapped = FloatArray(9)
        val rotation = when (display?.rotation) { Surface.ROTATION_90 -> Surface.ROTATION_90; Surface.ROTATION_180 -> Surface.ROTATION_180; Surface.ROTATION_270 -> Surface.ROTATION_270; else -> Surface.ROTATION_0 }
        val axes = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y
            else -> SensorManager.AXIS_X
        }
        val axis2 = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_X
            else -> SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrix, axes, axis2, remapped)
        val orientation = SensorManager.getOrientation(remapped, FloatArray(3))
        val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat().let { if (it < 0) it + 360f else it }
        smoothed = CompassMath.smooth(smoothed, degrees, 0.22f)
        val norm = sqrt(gravity.sumOf { (it * it).toDouble() }).toFloat()
        val tilt = if (haveGravity && norm > 0f) Math.toDegrees(acos((abs(gravity[2]) / norm).coerceIn(0f, 1f)).toDouble()) > 60.0 else false
        val fieldNorm = sqrt(field.sumOf { (it * it).toDouble() }).toFloat()
        val interference = haveField && (fieldNorm < 20f || fieldNorm > 100f)
        if (uiUpdateLimiter.shouldPublish(event.timestamp)) {
            onState(SensorCompassState(smoothed, lastAccuracy, true, null, tilt, interference))
        }
    }

    private fun copyLowPass(target: FloatArray, source: FloatArray) {
        for (i in 0..2) target[i] = target[i] * 0.85f + source[i] * 0.15f
    }
}
