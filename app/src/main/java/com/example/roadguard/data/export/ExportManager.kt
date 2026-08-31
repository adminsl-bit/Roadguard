package com.example.roadguard.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.roadguard.data.db.GroundTruthEventEntity
import com.example.roadguard.data.db.RecordingSessionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Generate a JSON file for the session containing metadata, events, and metrics
     */
    fun createJsonForSession(
        session: RecordingSessionEntity,
        events: List<GroundTruthEventEntity>
    ): File? {
        val sessionsDir = File(context.filesDir, "sessions")
        if (!sessionsDir.exists()) sessionsDir.mkdirs()

        val jsonFile = File(sessionsDir, "roadguard_session_${session.id}.json")

        try {
            val root = JSONObject().apply {
                put("app", "RoadGuard Sensor Test")
                put("session_id", session.id)
                put("start_time_iso", dateFormat.format(Date(session.startTime)))
                put("end_time_iso", dateFormat.format(Date(session.endTime)))
                put("duration_seconds", session.durationMs / 1000.0)
                put("duration_ms", session.durationMs)
                put("total_samples", session.sampleCount)
                put("distance_meters", session.totalDistanceMeters)
                put("distance_km", session.totalDistanceMeters / 1000.0)
                put("average_speed_kmh", session.avgSpeedKmh)
                put("max_speed_kmh", session.maxSpeedKmh)

                val counts = JSONObject().apply {
                    put("pothole", session.potholeCount)
                    put("speed_breaker", session.speedBreakerCount)
                    put("rough_road", session.roughRoadCount)
                    put("normal_road", session.normalRoadCount)
                    put("total_events", session.potholeCount + session.speedBreakerCount + session.roughRoadCount + session.normalRoadCount)
                }
                put("marked_events_summary", counts)

                val eventsArray = JSONArray()
                events.forEach { ev ->
                    val evObj = JSONObject().apply {
                        put("id", ev.id)
                        put("event_type", ev.eventType)
                        put("timestamp_ms", ev.timestamp)
                        put("timestamp_iso", dateFormat.format(Date(ev.timestamp)))
                        put("latitude", ev.latitude ?: JSONObject.NULL)
                        put("longitude", ev.longitude ?: JSONObject.NULL)
                        put("speed_kmh", ev.speedKmh ?: JSONObject.NULL)
                    }
                    eventsArray.put(evObj)
                }
                put("ground_truth_events", eventsArray)
                put("csv_file", session.csvFileName)
            }

            FileWriter(jsonFile).use { it.write(root.toString(2)) }
            return jsonFile
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Share a file (CSV or JSON) via Android ACTION_SEND Intent
     */
    fun shareFile(fileName: String, mimeType: String, title: String): Intent? {
        val file = File(File(context.filesDir, "sessions"), fileName)
        if (!file.exists()) return null

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            return null
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Exported RoadGuard Sensor Test data: $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, "Export $title")
    }

    fun deleteSessionFiles(csvFileName: String, jsonFileName: String) {
        try {
            val sessionsDir = File(context.filesDir, "sessions")
            File(sessionsDir, csvFileName).delete()
            File(sessionsDir, jsonFileName).delete()
        } catch (_: Exception) {}
    }
}
