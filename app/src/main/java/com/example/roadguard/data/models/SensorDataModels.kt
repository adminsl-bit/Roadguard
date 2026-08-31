package com.example.roadguard.data.models

enum class GroundTruthEventType(val label: String, val exportCode: String) {
    POTHOLE("Pothole", "POTHOLE"),
    SPEED_BREAKER("Speed Breaker", "SPEED_BREAKER"),
    ROUGH_ROAD("Rough Road", "ROUGH_ROAD"),
    NORMAL_ROAD("Normal Road", "NORMAL_ROAD")
}

enum class DetectionSensitivity(val label: String, val potholeThreshold: Float, val bumpThreshold: Float) {
    HIGH("High (Testing / Hand)", 2.0f, 1.6f),
    MEDIUM("Medium (Standard Drive)", 3.2f, 2.4f),
    LOW("Low (Severe Only)", 4.5f, 3.5f)
}

enum class RoadRoughnessLevel(val label: String) {
    SMOOTH("Smooth Road"),
    MODERATE("Minor Vibrations"),
    ROUGH("Rough / Uneven"),
    SEVERE("Severe / Potholes")
}

data class SensorAvailability(
    val hasAccelerometer: Boolean = false,
    val hasLinearAcceleration: Boolean = false,
    val hasGyroscope: Boolean = false,
    val hasGravity: Boolean = false,
    val hasRotationVector: Boolean = false
)

data class LiveMotionData(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.81f,
    val linAccelX: Float = 0f,
    val linAccelY: Float = 0f,
    val linAccelZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = 9.81f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val azimuthDeg: Float = 0f,
    val verticalGForce: Float = 1.0f,
    val peakGForce: Float = 1.0f,
    val roughnessScore: Float = 0f,
    val roughnessLevel: RoadRoughnessLevel = RoadRoughnessLevel.SMOOTH,
    val timestamp: Long = 0L
)

data class LiveGpsData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val speedMps: Float? = null,
    val speedKmh: Float? = null,
    val altitudeMeters: Double? = null,
    val timestamp: Long = 0L,
    val isAvailable: Boolean = false
)

data class GroundTruthEventItem(
    val id: Long = 0L,
    val sessionId: Long,
    val eventType: GroundTruthEventType,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val speedKmh: Float?,
    val isAutoDetected: Boolean = false,
    val gForceMagnitude: Float = 1.0f
)

data class ActiveSessionState(
    val isRecording: Boolean = false,
    val sessionId: Long = 0L,
    val startTime: Long = 0L,
    val durationMs: Long = 0L,
    val sampleCount: Long = 0L,
    val currentSpeedKmh: Float = 0f,
    val totalDistanceMeters: Float = 0f,
    val lastMarkedEvent: GroundTruthEventItem? = null,
    val markedPotholes: Int = 0,
    val markedSpeedBreakers: Int = 0,
    val markedRoughRoads: Int = 0,
    val markedNormalRoads: Int = 0,
    val autoDetectEnabled: Boolean = true,
    val sensitivity: DetectionSensitivity = DetectionSensitivity.HIGH,
    val peakSessionGForce: Float = 1.0f,
    val currentRoughnessLevel: RoadRoughnessLevel = RoadRoughnessLevel.SMOOTH
) {
    val totalMarkedEvents: Int
        get() = markedPotholes + markedSpeedBreakers + markedRoughRoads + markedNormalRoads
}
