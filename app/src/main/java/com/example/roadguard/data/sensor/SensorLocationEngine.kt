package com.example.roadguard.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.example.roadguard.data.models.ActiveSessionState
import com.example.roadguard.data.models.DetectionSensitivity
import com.example.roadguard.data.models.GroundTruthEventItem
import com.example.roadguard.data.models.GroundTruthEventType
import com.example.roadguard.data.models.LiveGpsData
import com.example.roadguard.data.models.LiveMotionData
import com.example.roadguard.data.models.RoadRoughnessLevel
import com.example.roadguard.data.models.SensorAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.sqrt

class SensorLocationEngine(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Sensors
    private val accelSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val linAccelSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gravitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val rotVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Sensor Availability
    private val _sensorAvailability = MutableStateFlow(
        SensorAvailability(
            hasAccelerometer = accelSensor != null,
            hasLinearAcceleration = linAccelSensor != null,
            hasGyroscope = gyroSensor != null,
            hasGravity = gravitySensor != null,
            hasRotationVector = rotVectorSensor != null
        )
    )
    val sensorAvailability: StateFlow<SensorAvailability> = _sensorAvailability.asStateFlow()

    // Live Sensor State
    private val _liveMotion = MutableStateFlow(LiveMotionData(accelZ = 9.81f, gravityZ = 9.81f))
    val liveMotion: StateFlow<LiveMotionData> = _liveMotion.asStateFlow()

    // Live GPS State
    private val _liveGps = MutableStateFlow(LiveGpsData())
    val liveGps: StateFlow<LiveGpsData> = _liveGps.asStateFlow()

    // Active Recording State
    private val _activeSession = MutableStateFlow(ActiveSessionState())
    val activeSession: StateFlow<ActiveSessionState> = _activeSession.asStateFlow()

    // Callback when an event (auto or manual) is detected
    var onEventDetectedListener: ((GroundTruthEventItem) -> Unit)? = null

    // Recording internals
    private val isRecording = AtomicBoolean(false)
    private val sampleCounter = AtomicLong(0L)
    private var activeSessionId = 0L
    private var sessionStartTime = 0L
    private var sessionCsvFile: File? = null
    private var csvBufferedWriter: BufferedWriter? = null

    // Tracking distance & speed metrics
    private var lastRecordedLocation: Location? = null
    private var totalDistanceAccumulated = 0f
    private var totalSpeedSum = 0.0
    private var speedSampleCount = 0
    private var maxRecordedSpeedKmh = 0f
    private var peakGForceEncountered = 1.0f

    // Rotation matrices for orientation
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Cached sensor values for high-frequency writes
    @Volatile private var latestAccX = 0f
    @Volatile private var latestAccY = 0f
    @Volatile private var latestAccZ = 9.81f

    @Volatile private var latestLinX = 0f
    @Volatile private var latestLinY = 0f
    @Volatile private var latestLinZ = 0f

    @Volatile private var latestGyroX = 0f
    @Volatile private var latestGyroY = 0f
    @Volatile private var latestGyroZ = 0f

    @Volatile private var latestGravX = 0f
    @Volatile private var latestGravY = 0f
    @Volatile private var latestGravZ = 9.81f

    @Volatile private var latestPitch = 0f
    @Volatile private var latestRoll = 0f
    @Volatile private var latestAzimuth = 0f

    // Road Disturbance Auto-Detection Engine state
    private var autoDetectEnabled = true
    private var currentSensitivity = DetectionSensitivity.HIGH
    private var lastPotholeDetectionTime = 0L
    private var lastSpeedBreakerDetectionTime = 0L
    private var lastRoughRoadDetectionTime = 0L

    // Rolling window buffer for road roughness (RMS of vertical linear acceleration)
    private val roughnessWindow = FloatArray(25)
    private var roughnessWindowIndex = 0

    // Pending ground truth events to tag next sensor sample
    private val pendingGroundTruthTag = ConcurrentLinkedQueue<String>()
    private val recordedEventsList = mutableListOf<GroundTruthEventItem>()

    // Event counter
    private var potholeCount = 0
    private var speedBreakerCount = 0
    private var roughRoadCount = 0
    private var normalRoadCount = 0

    private fun processLocation(location: Location) {
        val speedMps = if (location.hasSpeed()) location.speed else 0f
        val speedKmh = speedMps * 3.6f

        if (isRecording.get() && lastRecordedLocation != null) {
            val dist = location.distanceTo(lastRecordedLocation!!)
            // Filter out extreme jumps
            if (dist in 0.1f..500f) {
                totalDistanceAccumulated += dist
            }
        }
        if (isRecording.get()) {
            lastRecordedLocation = location
            totalSpeedSum += speedKmh
            speedSampleCount++
            if (speedKmh > maxRecordedSpeedKmh) {
                maxRecordedSpeedKmh = speedKmh
            }
        }

        _liveGps.update {
            LiveGpsData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                speedMps = speedMps,
                speedKmh = speedKmh,
                altitudeMeters = if (location.hasAltitude()) location.altitude else null,
                timestamp = System.currentTimeMillis(),
                isAvailable = true
            )
        }

        if (isRecording.get()) {
            _activeSession.update { current ->
                current.copy(
                    currentSpeedKmh = speedKmh,
                    totalDistanceMeters = totalDistanceAccumulated
                )
            }
        }
    }

    // GPS Location callback
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            processLocation(location)
        }
    }

    private val systemLocationListener = android.location.LocationListener { location ->
        processLocation(location)
    }

    fun startSensorMonitoring() {
        sensorManager?.let { sm ->
            // Use SENSOR_DELAY_GAME (~50Hz) for balanced responsiveness and stability
            accelSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            linAccelSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            gyroSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            gravitySensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            rotVectorSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    fun stopSensorMonitoring() {
        if (!isRecording.get()) {
            sensorManager?.unregisterListener(this)
        }
    }

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            ).setMinUpdateIntervalMillis(500L)
             .setMinUpdateDistanceMeters(0f)
             .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: Exception) {
            _liveGps.update { it.copy(isAvailable = false) }
        }

        // Register system LocationManager as robust fallback
        try {
            locationManager?.let { lm ->
                if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER,
                        1000L,
                        0f,
                        systemLocationListener,
                        Looper.getMainLooper()
                    )
                }
                if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        1000L,
                        0f,
                        systemLocationListener,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun stopGpsUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
        try {
            locationManager?.removeUpdates(systemLocationListener)
        } catch (_: Exception) {}
    }

    fun setAutoDetectEnabled(enabled: Boolean) {
        autoDetectEnabled = enabled
        _activeSession.update { it.copy(autoDetectEnabled = enabled) }
    }

    fun setSensitivity(sensitivity: DetectionSensitivity) {
        currentSensitivity = sensitivity
        _activeSession.update { it.copy(sensitivity = sensitivity) }
    }

    /**
     * Start high-frequency recording session
     */
    fun startRecording(): Long {
        if (isRecording.get()) return activeSessionId

        val sessionId = System.currentTimeMillis()
        activeSessionId = sessionId
        sessionStartTime = sessionId
        sampleCounter.set(0L)
        potholeCount = 0
        speedBreakerCount = 0
        roughRoadCount = 0
        normalRoadCount = 0
        totalDistanceAccumulated = 0f
        totalSpeedSum = 0.0
        speedSampleCount = 0
        maxRecordedSpeedKmh = 0f
        peakGForceEncountered = 1.0f
        lastRecordedLocation = null
        recordedEventsList.clear()
        pendingGroundTruthTag.clear()

        // Create sessions directory
        val sessionsDir = File(context.filesDir, "sessions")
        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs()
        }

        val csvFile = File(sessionsDir, "roadguard_session_${sessionId}.csv")
        sessionCsvFile = csvFile

        try {
            val writer = BufferedWriter(FileWriter(csvFile, false), 32768)
            // Exact required header format
            writer.write("timestamp,latitude,longitude,gps_accuracy,speed_mps,speed_kmh,accelerometer_x,accelerometer_y,accelerometer_z,linear_acceleration_x,linear_acceleration_y,linear_acceleration_z,gyro_x,gyro_y,gyro_z,gravity_x,gravity_y,gravity_z,ground_truth_event\n")
            writer.flush()
            csvBufferedWriter = writer
        } catch (_: Exception) {
            csvBufferedWriter = null
        }

        isRecording.set(true)

        // Ensure all sensor listeners are active
        startSensorMonitoring()

        _activeSession.value = ActiveSessionState(
            isRecording = true,
            sessionId = sessionId,
            startTime = sessionStartTime,
            durationMs = 0L,
            sampleCount = 0L,
            currentSpeedKmh = _liveGps.value.speedKmh ?: 0f,
            totalDistanceMeters = 0f,
            autoDetectEnabled = autoDetectEnabled,
            sensitivity = currentSensitivity,
            peakSessionGForce = 1.0f,
            currentRoughnessLevel = RoadRoughnessLevel.SMOOTH
        )

        return sessionId
    }

    /**
     * Mark a ground truth event manually
     */
    fun markGroundTruthEvent(eventType: GroundTruthEventType): GroundTruthEventItem {
        val now = System.currentTimeMillis()
        val currentGps = _liveGps.value
        val totalG = sqrt(latestAccX * latestAccX + latestAccY * latestAccY + latestAccZ * latestAccZ) / 9.81f

        val eventItem = GroundTruthEventItem(
            id = now,
            sessionId = activeSessionId,
            eventType = eventType,
            timestamp = now,
            latitude = currentGps.latitude,
            longitude = currentGps.longitude,
            speedKmh = currentGps.speedKmh,
            isAutoDetected = false,
            gForceMagnitude = totalG
        )

        recordedEventsList.add(eventItem)
        pendingGroundTruthTag.add(eventType.exportCode)

        when (eventType) {
            GroundTruthEventType.POTHOLE -> potholeCount++
            GroundTruthEventType.SPEED_BREAKER -> speedBreakerCount++
            GroundTruthEventType.ROUGH_ROAD -> roughRoadCount++
            GroundTruthEventType.NORMAL_ROAD -> normalRoadCount++
        }

        _activeSession.update { current ->
            current.copy(
                lastMarkedEvent = eventItem,
                markedPotholes = potholeCount,
                markedSpeedBreakers = speedBreakerCount,
                markedRoughRoads = roughRoadCount,
                markedNormalRoads = normalRoadCount
            )
        }

        onEventDetectedListener?.invoke(eventItem)
        return eventItem
    }

    /**
     * Record an automatically detected road disturbance
     */
    private fun recordAutoDetectedEvent(eventType: GroundTruthEventType, gForce: Float) {
        val now = System.currentTimeMillis()
        val currentGps = _liveGps.value

        val eventItem = GroundTruthEventItem(
            id = now,
            sessionId = activeSessionId,
            eventType = eventType,
            timestamp = now,
            latitude = currentGps.latitude,
            longitude = currentGps.longitude,
            speedKmh = currentGps.speedKmh,
            isAutoDetected = true,
            gForceMagnitude = gForce
        )

        recordedEventsList.add(eventItem)
        pendingGroundTruthTag.add("${eventType.exportCode}_AUTO")

        when (eventType) {
            GroundTruthEventType.POTHOLE -> potholeCount++
            GroundTruthEventType.SPEED_BREAKER -> speedBreakerCount++
            GroundTruthEventType.ROUGH_ROAD -> roughRoadCount++
            GroundTruthEventType.NORMAL_ROAD -> normalRoadCount++
        }

        _activeSession.update { current ->
            current.copy(
                lastMarkedEvent = eventItem,
                markedPotholes = potholeCount,
                markedSpeedBreakers = speedBreakerCount,
                markedRoughRoads = roughRoadCount,
                markedNormalRoads = normalRoadCount
            )
        }

        onEventDetectedListener?.invoke(eventItem)
    }

    /**
     * Stop recording session and return summary
     */
    fun stopRecording(): SessionStopResult? {
        if (!isRecording.get()) return null

        isRecording.set(false)
        val endTime = System.currentTimeMillis()
        val durationMs = endTime - sessionStartTime
        val sampleCount = sampleCounter.get()

        // Close CSV file writer
        try {
            csvBufferedWriter?.flush()
            csvBufferedWriter?.close()
        } catch (_: Exception) {}
        csvBufferedWriter = null

        val avgSpeedKmh = if (speedSampleCount > 0) (totalSpeedSum / speedSampleCount).toFloat() else 0f

        val result = SessionStopResult(
            sessionId = activeSessionId,
            startTime = sessionStartTime,
            endTime = endTime,
            durationMs = durationMs,
            sampleCount = sampleCount,
            potholeCount = potholeCount,
            speedBreakerCount = speedBreakerCount,
            roughRoadCount = roughRoadCount,
            normalRoadCount = normalRoadCount,
            totalDistanceMeters = totalDistanceAccumulated,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxRecordedSpeedKmh,
            csvFileName = sessionCsvFile?.name ?: "roadguard_session_${activeSessionId}.csv",
            events = recordedEventsList.toList()
        )

        _activeSession.value = ActiveSessionState(
            isRecording = false,
            autoDetectEnabled = autoDetectEnabled,
            sensitivity = currentSensitivity
        )

        return result
    }

    fun updateDuration(currentTimestamp: Long) {
        if (isRecording.get()) {
            val dur = currentTimestamp - sessionStartTime
            _activeSession.update {
                it.copy(
                    durationMs = dur,
                    sampleCount = sampleCounter.get(),
                    peakSessionGForce = peakGForceEncountered
                )
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                latestAccX = event.values[0]
                latestAccY = event.values[1]
                latestAccZ = event.values[2]
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                latestLinX = event.values[0]
                latestLinY = event.values[1]
                latestLinZ = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                latestGyroX = event.values[0]
                latestGyroY = event.values[1]
                latestGyroZ = event.values[2]
            }
            Sensor.TYPE_GRAVITY -> {
                latestGravX = event.values[0]
                latestGravY = event.values[1]
                latestGravZ = event.values[2]
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                latestAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                latestPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                latestRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            }
        }

        // Calculate 3D G-Force & Vertical Disturbance
        val totalAccelMag = sqrt(latestAccX * latestAccX + latestAccY * latestAccY + latestAccZ * latestAccZ)
        val currentGForce = totalAccelMag / 9.81f
        if (currentGForce > peakGForceEncountered) {
            peakGForceEncountered = currentGForce
        }

        // If linear acceleration sensor is available, use latestLinZ, else derive from vertical acceleration
        val verticalLinAccel = if (linAccelSensor != null) {
            latestLinZ
        } else {
            latestAccZ - 9.81f
        }

        // Update rolling window for road roughness score
        roughnessWindow[roughnessWindowIndex] = verticalLinAccel * verticalLinAccel
        roughnessWindowIndex = (roughnessWindowIndex + 1) % roughnessWindow.size
        var sumSquares = 0f
        for (sq in roughnessWindow) {
            sumSquares += sq
        }
        val roughnessRms = sqrt(sumSquares / roughnessWindow.size)

        val currentRoughnessLevel = when {
            roughnessRms > 2.2f || currentGForce > 1.6f -> RoadRoughnessLevel.SEVERE
            roughnessRms > 1.2f || currentGForce > 1.3f -> RoadRoughnessLevel.ROUGH
            roughnessRms > 0.5f || currentGForce > 1.15f -> RoadRoughnessLevel.MODERATE
            else -> RoadRoughnessLevel.SMOOTH
        }

        // Real-Time Automatic Disturbance Detection
        if (isRecording.get() && autoDetectEnabled) {
            detectRoadAnomalies(now, verticalLinAccel, currentGForce, roughnessRms)
        }

        // Always write sample during recording on primary motion updates
        if (isRecording.get() && (event.sensor.type == Sensor.TYPE_ACCELEROMETER || event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION)) {
            writeSample(now)
        }

        // Update live motion state for UI
        _liveMotion.value = LiveMotionData(
            accelX = latestAccX,
            accelY = latestAccY,
            accelZ = latestAccZ,
            linAccelX = latestLinX,
            linAccelY = latestLinY,
            linAccelZ = latestLinZ,
            gyroX = latestGyroX,
            gyroY = latestGyroY,
            gyroZ = latestGyroZ,
            gravityX = latestGravX,
            gravityY = latestGravY,
            gravityZ = latestGravZ,
            pitchDeg = latestPitch,
            rollDeg = latestRoll,
            azimuthDeg = latestAzimuth,
            verticalGForce = currentGForce,
            peakGForce = peakGForceEncountered,
            roughnessScore = roughnessRms,
            roughnessLevel = currentRoughnessLevel,
            timestamp = now
        )
    }

    /**
     * Automatic Road Anomaly Classifier:
     * - Potholes: Sharp high-amplitude vertical impact/drop (|lin_z| > potholeThreshold or G > 1.45G)
     * - Speed Breakers: Characteristic compression/rebound bump with vertical displacement
     * - Rough Road: Continuous high vibration RMS over sustained duration
     */
    private fun detectRoadAnomalies(
        timestamp: Long,
        verticalLinAccel: Float,
        gForce: Float,
        roughnessRms: Float
    ) {
        val potholeThresh = currentSensitivity.potholeThreshold
        val bumpThresh = currentSensitivity.bumpThreshold

        // 1. Pothole Detection (Vertical sharp drop & impact with 700ms debounce)
        val absLinZ = abs(verticalLinAccel)
        if ((absLinZ >= potholeThresh || gForce >= (1.0f + (potholeThresh / 9.81f))) &&
            (timestamp - lastPotholeDetectionTime > 700L)
        ) {
            lastPotholeDetectionTime = timestamp
            recordAutoDetectedEvent(GroundTruthEventType.POTHOLE, gForce)
            return
        }

        // 2. Speed Breaker / Bump Detection (Moderate heave/compression with 900ms debounce)
        if (absLinZ >= bumpThresh && absLinZ < potholeThresh &&
            (timestamp - lastSpeedBreakerDetectionTime > 900L) &&
            (timestamp - lastPotholeDetectionTime > 600L)
        ) {
            lastSpeedBreakerDetectionTime = timestamp
            recordAutoDetectedEvent(GroundTruthEventType.SPEED_BREAKER, gForce)
            return
        }

        // 3. Sustained Rough Road Section (Roughness RMS > 1.5 with 2500ms debounce)
        if (roughnessRms > 1.6f && (timestamp - lastRoughRoadDetectionTime > 2500L)) {
            lastRoughRoadDetectionTime = timestamp
            recordAutoDetectedEvent(GroundTruthEventType.ROUGH_ROAD, gForce)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun writeSample(timestamp: Long) {
        val writer = csvBufferedWriter ?: return
        val count = sampleCounter.incrementAndGet()
        val gps = _liveGps.value

        // Check if there is an active ground truth tag
        val eventTag = pendingGroundTruthTag.poll() ?: ""

        val latStr = gps.latitude?.let { String.format(Locale.US, "%.7f", it) } ?: ""
        val lonStr = gps.longitude?.let { String.format(Locale.US, "%.7f", it) } ?: ""
        val accStr = gps.accuracyMeters?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        val spdMpsStr = gps.speedMps?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        val spdKmhStr = gps.speedKmh?.let { String.format(Locale.US, "%.2f", it) } ?: ""

        try {
            val line = String.format(
                Locale.US,
                "%d,%s,%s,%s,%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s\n",
                timestamp,
                latStr,
                lonStr,
                accStr,
                spdMpsStr,
                spdKmhStr,
                latestAccX,
                latestAccY,
                latestAccZ,
                latestLinX,
                latestLinY,
                latestLinZ,
                latestGyroX,
                latestGyroY,
                latestGyroZ,
                latestGravX,
                latestGravY,
                latestGravZ,
                eventTag
            )
            writer.write(line)
            // Flush periodically to prevent data loss without causing IO bottleneck
            if (count % 25 == 0L) {
                writer.flush()
            }
        } catch (_: Exception) {}
    }
}

data class SessionStopResult(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val sampleCount: Long,
    val potholeCount: Int,
    val speedBreakerCount: Int,
    val roughRoadCount: Int,
    val normalRoadCount: Int,
    val totalDistanceMeters: Float,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Float,
    val csvFileName: String,
    val events: List<GroundTruthEventItem>
)
