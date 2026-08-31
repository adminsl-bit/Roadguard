package com.example.roadguard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<RecordingSessionEntity>>

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): RecordingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RecordingSessionEntity): Long

    @Query("DELETE FROM recording_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<GroundTruthEventEntity>)

    @Query("SELECT * FROM ground_truth_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEventsForSession(sessionId: Long): Flow<List<GroundTruthEventEntity>>

    @Query("SELECT * FROM ground_truth_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsListForSession(sessionId: Long): List<GroundTruthEventEntity>
}
