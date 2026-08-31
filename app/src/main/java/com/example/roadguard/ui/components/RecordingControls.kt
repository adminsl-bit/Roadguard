package com.example.roadguard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadguard.data.models.ActiveSessionState
import com.example.roadguard.data.models.DetectionSensitivity
import com.example.roadguard.data.models.GroundTruthEventType
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoBorderSubtle
import com.example.ui.theme.BentoCardBg
import com.example.ui.theme.BentoCardInner
import com.example.ui.theme.NormalRoadEmerald
import com.example.ui.theme.PotholeOrange
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimarySkyLight
import com.example.ui.theme.RecordRed
import com.example.ui.theme.RoughRoadViolet
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SpeedBreakerAmber
import java.util.Locale

@Composable
fun StartRecordingPanel(
    onStartClick: () -> Unit,
    autoDetectEnabled: Boolean,
    sensitivity: DetectionSensitivity,
    onToggleAutoDetect: (Boolean) -> Unit,
    onSelectSensitivity: (DetectionSensitivity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("start_recording_panel"),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .testTag("start_recording_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NormalRoadEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Recording",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "START RECORDING",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-Detection Configuration Panel
            Surface(
                color = BentoCardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BentoBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = SpeedBreakerAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Auto-Detect Potholes & Bumps",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "Real-time vertical acceleration & jerk analysis",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Slate400
                                )
                            }
                        }

                        Switch(
                            checked = autoDetectEnabled,
                            onCheckedChange = onToggleAutoDetect,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NormalRoadEmerald,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = BentoCardBg
                            ),
                            modifier = Modifier.testTag("auto_detect_switch")
                        )
                    }

                    if (autoDetectEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DETECTION SENSITIVITY:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            ),
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetectionSensitivity.values().forEach { sens ->
                                val isSelected = sens == sensitivity
                                Surface(
                                    color = if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else BentoCardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) PrimaryCyan else BentoBorderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectSensitivity(sens) }
                                ) {
                                    Text(
                                        text = sens.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) PrimaryCyan else Slate400,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveRecordingPanel(
    sessionState: ActiveSessionState,
    onMarkEvent: (GroundTruthEventType) -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_recording_panel"),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        border = BorderStroke(1.dp, RecordRed.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Live Recording Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = RecordRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RecordRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(alpha)
                                .background(RecordRed, CircleShape)
                        )
                        Text(
                            text = "RECORDING ACTIVE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            ),
                            color = RecordRed
                        )
                    }
                }

                val totalSec = sessionState.durationMs / 1000
                val minutes = totalSec / 60
                val seconds = totalSec % 60
                val millis = (sessionState.durationMs % 1000) / 100

                Surface(
                    color = BentoCardInner,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BentoBorderSubtle)
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, millis),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Bento Panel
            Surface(
                color = BentoCardInner,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BentoBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SAMPLES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Slate400
                        )
                        Text(
                            text = String.format(Locale.US, "%,d", sessionState.sampleCount),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = PrimarySkyLight
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PEAK G-FORCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Slate400
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f G", sessionState.peakSessionGForce),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (sessionState.peakSessionGForce > 1.4f) PotholeOrange else PrimaryCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "POTHOLES & EVENTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Slate400
                        )
                        Text(
                            text = "${sessionState.totalMarkedEvents}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = SpeedBreakerAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ground Truth Buttons Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POTHOLE & EVENT TAGGING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Slate400
                )
                if (sessionState.autoDetectEnabled) {
                    Surface(
                        color = SpeedBreakerAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "⚡ AUTO-DETECT ON",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = SpeedBreakerAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2x2 Bento Grid of Ground Truth Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GroundTruthButton(
                    title = "POTHOLE",
                    count = sessionState.markedPotholes,
                    icon = Icons.Default.Warning,
                    baseColor = PotholeOrange,
                    testTag = "mark_pothole_button",
                    onClick = { onMarkEvent(GroundTruthEventType.POTHOLE) },
                    modifier = Modifier.weight(1f)
                )

                GroundTruthButton(
                    title = "SPEED BREAKER",
                    count = sessionState.markedSpeedBreakers,
                    icon = Icons.Default.Traffic,
                    baseColor = SpeedBreakerAmber,
                    testTag = "mark_speed_breaker_button",
                    onClick = { onMarkEvent(GroundTruthEventType.SPEED_BREAKER) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GroundTruthButton(
                    title = "ROUGH ROAD",
                    count = sessionState.markedRoughRoads,
                    icon = Icons.Default.Waves,
                    baseColor = RoughRoadViolet,
                    testTag = "mark_rough_road_button",
                    onClick = { onMarkEvent(GroundTruthEventType.ROUGH_ROAD) },
                    modifier = Modifier.weight(1f)
                )

                GroundTruthButton(
                    title = "NORMAL ROAD",
                    count = sessionState.markedNormalRoads,
                    icon = Icons.Default.Terrain,
                    baseColor = NormalRoadEmerald,
                    testTag = "mark_normal_road_button",
                    onClick = { onMarkEvent(GroundTruthEventType.NORMAL_ROAD) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent STOP RECORDING Button
            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("stop_recording_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecordRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Recording",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "STOP & SAVE RECORDING",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GroundTruthButton(
    title: String,
    count: Int,
    icon: ImageVector,
    baseColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = baseColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, baseColor.copy(alpha = 0.75f)),
        modifier = modifier
            .height(84.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = baseColor)
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = baseColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = baseColor,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Count: $count",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
