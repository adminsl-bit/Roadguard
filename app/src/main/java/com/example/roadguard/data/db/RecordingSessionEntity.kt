package com.example.roadguard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_sessions")
data class RecordingSessionEntity(
    @PrimaryKey val id: Long,
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
    val jsonFileName: String
)
