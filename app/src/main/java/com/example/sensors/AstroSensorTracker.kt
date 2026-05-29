package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AstroCelestialOrientation(
    val pitch: Float = 0f,   // vertical tilt up/down (elevation)
    val roll: Float = 0f,    // horizontal balance tilt
    val azimuth: Float = 0f  // compass direction (0 = North, 90 = East, 180 = South, 270 = West)
)

class AstroSensorTracker(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _orientation = MutableStateFlow(AstroCelestialOrientation())
    val orientation: StateFlow<AstroCelestialOrientation> = _orientation.asStateFlow()

    private var gravityValues = FloatArray(3)
    private var geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    fun startTracking() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, event.values.size)
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagneticValues, 0, event.values.size)
            hasGeomagnetic = true
        }

        if (hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
                val orientationValues = FloatArray(3)
                SensorManager.getOrientation(r, orientationValues)
                
                // Convert to degrees
                val azimuthDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                // Convert range from [-180, 180] to [0, 360]
                val normalizedAzimuth = (azimuthDeg + 360) % 360

                // Pitch & Roll in degrees
                val pitchDeg = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                val rollDeg = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

                _orientation.value = AstroCelestialOrientation(
                    pitch = pitchDeg,
                    roll = rollDeg,
                    azimuth = normalizedAzimuth
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
