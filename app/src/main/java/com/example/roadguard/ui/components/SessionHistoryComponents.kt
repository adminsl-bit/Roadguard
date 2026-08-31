package com.example.roadguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadguard.data.db.RecordingSessionEntity
import com.example.ui.theme.NormalRoadEmerald
import com.example.ui.theme.PotholeOrange
import com.example.ui.theme.PrimarySky
import com.example.ui.theme.PrimarySkyLight
import com.example.ui.theme.RecordRed
import com.example.ui.theme.RoughRoadViolet
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SpeedBreakerAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.US)

@Composable
fun SessionItemCard(
    session: RecordingSessionEntity,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("session_card_${session.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Date and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormatter.format(Date(session.startTime)),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Session #${session.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_session_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Session",
                        tint = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Grid: Duration, Distance, Samples, Speed
            Surface(
                color = Slate800,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SessionMetricItem(
                            label = "DURATION",
                            value = formatDuration(session.durationMs)
                        )
                        SessionMetricItem(
                            label = "SAMPLES",
                            value = String.format(Locale.US, "%,d", session.sampleCount)
                        )
                        SessionMetricItem(
                            label = "DISTANCE",
                            value = if (session.totalDistanceMeters >= 1000)
                                String.format(Locale.US, "%.2f km", session.totalDistanceMeters / 1000f)
                            else
                                String.format(Locale.US, "%.0f m", session.totalDistanceMeters)
                        )
                        SessionMetricItem(
                            label = "AVG SPEED",
                            value = String.format(Locale.US, "%.1f km/h", session.avgSpeedKmh)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Marked Events Badges
            val totalMarked = session.potholeCount + session.speedBreakerCount + session.roughRoadCount + session.normalRoadCount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EVENTS ($totalMarked):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate400
                )

                if (session.potholeCount > 0) {
                    EventCountPill("Potholes: ${session.potholeCount}", PotholeOrange)
                }
                if (session.speedBreakerCount > 0) {
                    EventCountPill("Breakers: ${session.speedBreakerCount}", SpeedBreakerAmber)
                }
                if (session.roughRoadCount > 0) {
                    EventCountPill("Rough: ${session.roughRoadCount}", RoughRoadViolet)
                }
                if (session.normalRoadCount > 0) {
                    EventCountPill("Normal: ${session.normalRoadCount}", NormalRoadEmerald)
                }
                if (totalMarked == 0) {
                    Text(text = "None marked", style = MaterialTheme.typography.labelSmall, color = Slate400)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Export Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("export_csv_btn_${session.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimarySky,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export CSV",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXPORT CSV",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("export_json_btn_${session.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimarySkyLight
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Export JSON",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXPORT JSON",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Session?") },
            text = { Text("Are you sure you want to delete recording session #${session.id} and its CSV/JSON files?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = RecordRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EventCountPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SessionMetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Slate400
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White
        )
    }
}

@Composable
fun SessionSavedDialog(
    session: RecordingSessionEntity,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Saved",
                    tint = NormalRoadEmerald
                )
                Text("Recording Session Saved")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Session successfully captured and stored locally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate200
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Duration: ${formatDuration(session.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                Text(
                    text = "• Sensor Samples: ${String.format(Locale.US, "%,d", session.sampleCount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                Text(
                    text = "• Total Marked Events: ${session.potholeCount + session.speedBreakerCount + session.roughRoadCount + session.normalRoadCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                Text(
                    text = "• Distance: ${String.format(Locale.US, "%.1f m", session.totalDistanceMeters)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onExportCsv()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarySky)
                ) {
                    Text("Export CSV")
                }
                OutlinedButton(
                    onClick = {
                        onExportJson()
                    }
                ) {
                    Text("Export JSON")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}
