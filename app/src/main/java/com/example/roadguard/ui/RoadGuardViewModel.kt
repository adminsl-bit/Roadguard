package com.example.roadguard.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadguard.data.db.AppDatabase
import com.example.roadguard.data.db.GroundTruthEventEntity
import com.example.roadguard.data.db.RecordingSessionEntity
import com.example.roadguard.data.export.ExportManager
import com.example.roadguard.data.models.ActiveSessionState
import com.example.roadguard.data.models.DetectionSensitivity
import com.example.roadguard.data.models.GroundTruthEventType
import com.example.roadguard.data.models.LiveGpsData
import com.example.roadguard.data.models.LiveMotionData
import com.example.roadguard.data.models.SensorAvailability
import com.example.roadguard.data.sensor.SensorLocationEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class RoadGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.recordingDao()
    private val sensorEngine = SensorLocationEngine(application)
    private val exportManager = ExportManager(application)

    val sensorAvailability: StateFlow<SensorAvailability> = sensorEngine.sensorAvailability
    val liveMotion: StateFlow<LiveMotionData> = sensorEngine.liveMotion
    val liveGps: StateFlow<LiveGpsData> = sensorEngine.liveGps
    val activeSession: StateFlow<ActiveSessionState> = sensorEngine.activeSession

    val sessionHistory: StateFlow<List<RecordingSessionEntity>> = dao.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _lastSavedSession = MutableStateFlow<RecordingSessionEntity?>(null)
    val lastSavedSession: StateFlow<RecordingSessionEntity?> = _lastSavedSession.asStateFlow()

    private val _eventNotification = MutableStateFlow<String?>(null)
    val eventNotification: StateFlow<String?> = _eventNotification.asStateFlow()

    private var timerJob: Job? = null
    private var notificationJob: Job? = null

    init {
        sensorEngine.startSensorMonitoring()

        sensorEngine.onEventDetectedListener = { event ->
            val prefix = if (event.isAutoDetected) "⚡ AUTO-DETECTED" else "TAGGED"
            val text = when (event.eventType) {
                GroundTruthEventType.POTHOLE -> "$prefix POTHOLE (${String.format(Locale.US, "%.1fG", event.gForceMagnitude)})"
                GroundTruthEventType.SPEED_BREAKER -> "$prefix SPEED BREAKER"
                GroundTruthEventType.ROUGH_ROAD -> "$prefix ROUGH ROAD SECTION"
                GroundTruthEventType.NORMAL_ROAD -> "$prefix NORMAL ROAD"
            }
            triggerHapticFeedback(longDuration = false)
            _eventNotification.value = text
            notificationJob?.cancel()
            notificationJob = viewModelScope.launch {
                delay(2200)
                if (_eventNotification.value == text) {
                    _eventNotification.value = null
                }
            }
        }
    }

    fun onLocationPermissionResult(isGranted: Boolean) {
        _hasLocationPermission.value = isGranted
        if (isGranted) {
            sensorEngine.startGpsUpdates()
        } else {
            sensorEngine.stopGpsUpdates()
        }
    }

    fun toggleAutoDetect(enabled: Boolean) {
        sensorEngine.setAutoDetectEnabled(enabled)
    }

    fun setSensitivity(sensitivity: DetectionSensitivity) {
        sensorEngine.setSensitivity(sensitivity)
    }

    fun startRecording() {
        val sessionId = sensorEngine.startRecording()
        _lastSavedSession.value = null

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                sensorEngine.updateDuration(System.currentTimeMillis())
                delay(100)
            }
        }
        triggerHapticFeedback(longDuration = true)
    }

    fun stopRecording() {
        timerJob?.cancel()
        timerJob = null

        val stopResult = sensorEngine.stopRecording() ?: return
        triggerHapticFeedback(longDuration = true)

        viewModelScope.launch {
            val jsonFileName = "roadguard_session_${stopResult.sessionId}.json"

            val sessionEntity = RecordingSessionEntity(
                id = stopResult.sessionId,
                startTime = stopResult.startTime,
                endTime = stopResult.endTime,
                durationMs = stopResult.durationMs,
                sampleCount = stopResult.sampleCount,
                potholeCount = stopResult.potholeCount,
                speedBreakerCount = stopResult.speedBreakerCount,
                roughRoadCount = stopResult.roughRoadCount,
                normalRoadCount = stopResult.normalRoadCount,
                totalDistanceMeters = stopResult.totalDistanceMeters,
                avgSpeedKmh = stopResult.avgSpeedKmh,
                maxSpeedKmh = stopResult.maxSpeedKmh,
                csvFileName = stopResult.csvFileName,
                jsonFileName = jsonFileName
            )

            val eventEntities = stopResult.events.map { ev ->
                GroundTruthEventEntity(
                    sessionId = sessionEntity.id,
                    eventType = if (ev.isAutoDetected) "${ev.eventType.name}_AUTO" else ev.eventType.name,
                    timestamp = ev.timestamp,
                    latitude = ev.latitude,
                    longitude = ev.longitude,
                    speedKmh = ev.speedKmh
                )
            }

            dao.insertSession(sessionEntity)
            if (eventEntities.isNotEmpty()) {
                dao.insertEvents(eventEntities)
            }

            // Generate companion JSON file
            exportManager.createJsonForSession(sessionEntity, eventEntities)
            _lastSavedSession.value = sessionEntity
        }
    }

    fun markGroundTruth(eventType: GroundTruthEventType) {
        if (!activeSession.value.isRecording) return
        sensorEngine.markGroundTruthEvent(eventType)
    }

    fun dismissNotification() {
        _eventNotification.value = null
    }

    fun dismissSavedSessionDialog() {
        _lastSavedSession.value = null
    }

    fun exportCsv(session: RecordingSessionEntity): Intent? {
        return exportManager.shareFile(
            fileName = session.csvFileName,
            mimeType = "text/csv",
            title = "RoadGuard CSV - Session ${session.id}"
        )
    }

    fun exportJson(session: RecordingSessionEntity): Intent? {
        viewModelScope.launch {
            val events = dao.getEventsListForSession(session.id)
            exportManager.createJsonForSession(session, events)
        }
        return exportManager.shareFile(
            fileName = session.jsonFileName,
            mimeType = "application/json",
            title = "RoadGuard JSON - Session ${session.id}"
        )
    }

    fun deleteSession(session: RecordingSessionEntity) {
        viewModelScope.launch {
            dao.deleteSessionById(session.id)
            exportManager.deleteSessionFiles(session.csvFileName, session.jsonFileName)
            if (_lastSavedSession.value?.id == session.id) {
                _lastSavedSession.value = null
            }
        }
    }

    private fun triggerHapticFeedback(longDuration: Boolean) {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val duration = if (longDuration) 150L else 70L
                val effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val duration = if (longDuration) 150L else 70L
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        notificationJob?.cancel()
        sensorEngine.stopSensorMonitoring()
        sensorEngine.stopGpsUpdates()
    }
}
