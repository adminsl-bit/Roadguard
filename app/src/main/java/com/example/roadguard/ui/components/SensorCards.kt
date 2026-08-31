package com.example.roadguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadguard.data.models.LiveGpsData
import com.example.roadguard.data.models.LiveMotionData
import com.example.roadguard.data.models.SensorAvailability
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoBorderSubtle
import com.example.ui.theme.BentoCardBg
import com.example.ui.theme.BentoCardInner
import com.example.ui.theme.NormalRoadEmerald
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimarySkyLight
import com.example.ui.theme.RecordRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale

@Composable
fun SensorAvailabilityRow(
    availability: SensorAvailability,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sensor_availability_card"),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = PrimarySkyLight.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Sensor Status",
                            tint = PrimarySkyLight,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                    Text(
                        text = "HARDWARE SENSORS STATUS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = Slate400
                    )
                }

                Surface(
                    color = BentoCardInner,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, BentoBorderSubtle)
                ) {
                    Text(
                        text = if (availability.hasAccelerometer) "ONLINE" else "OFFLINE",
                        color = if (availability.hasAccelerometer) NormalRoadEmerald else RecordRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bento Grid of sensor status chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SensorStatusBadge("ACCEL", availability.hasAccelerometer, Modifier.weight(1f))
                SensorStatusBadge("LIN-ACC", availability.hasLinearAcceleration, Modifier.weight(1.1f))
                SensorStatusBadge("GYRO", availability.hasGyroscope, Modifier.weight(1f))
                SensorStatusBadge("GRAV", availability.hasGravity, Modifier.weight(1f))
                SensorStatusBadge("ROTATION", availability.hasRotationVector, Modifier.weight(1.2f))
            }
        }
    }
}

@Composable
fun SensorStatusBadge(
    name: String,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor = if (isAvailable) NormalRoadEmerald else RecordRed
    val bgColor = if (isAvailable) NormalRoadEmerald.copy(alpha = 0.12f) else RecordRed.copy(alpha = 0.12f)
    val borderColor = if (isAvailable) NormalRoadEmerald.copy(alpha = 0.35f) else RecordRed.copy(alpha = 0.35f)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.testTag("sensor_badge_$name")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun GpsStatusCard(
    gpsData: LiveGpsData,
    hasPermission: Boolean,
    onGrantPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gps_status_card"),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = (if (gpsData.isAvailable) NormalRoadEmerald else Slate700).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (gpsData.isAvailable) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                            contentDescription = "GPS Status",
                            tint = if (gpsData.isAvailable) NormalRoadEmerald else Slate400,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                    Text(
                        text = "GPS TELEMETRY & SPEED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = Slate400
                    )
                }

                Surface(
                    color = if (!hasPermission) RecordRed.copy(alpha = 0.2f)
                           else if (gpsData.isAvailable) NormalRoadEmerald.copy(alpha = 0.2f)
                           else BentoCardInner,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (!hasPermission) RecordRed.copy(alpha = 0.4f)
                        else if (gpsData.isAvailable) NormalRoadEmerald.copy(alpha = 0.4f)
                        else BentoBorderSubtle
                    )
                ) {
                    Text(
                        text = if (!hasPermission) "PERMISSION REQUIRED"
                               else if (gpsData.isAvailable) "● FIX ACQUIRED"
                               else "SEARCHING GPS...",
                        color = if (!hasPermission) RecordRed
                                else if (gpsData.isAvailable) NormalRoadEmerald
                                else Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bento Modular Speed & Accuracy Panel
            Surface(
                color = BentoCardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BentoBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Display
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CURRENT SPEED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = Slate400
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", gpsData.speedKmh ?: 0f),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = PrimarySkyLight
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "km/h",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate400,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "(${String.format(Locale.US, "%.1f", gpsData.speedMps ?: 0f)} m/s)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Slate500,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // Accuracy Tile
                    Surface(
                        color = BentoCardBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BentoBorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "ACCURACY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Slate400
                            )
                            Text(
                                text = if (gpsData.accuracyMeters != null) "±${String.format(Locale.US, "%.1f", gpsData.accuracyMeters)}m" else "--",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if ((gpsData.accuracyMeters ?: 99f) <= 10f) NormalRoadEmerald else Slate200
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bento Coordinates Tile (Lat & Long)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = BentoCardInner,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BentoBorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "LATITUDE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = gpsData.latitude?.let { String.format(Locale.US, "%.7f°", it) } ?: "Searching...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }

                Surface(
                    color = BentoCardInner,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BentoBorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "LONGITUDE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = gpsData.longitude?.let { String.format(Locale.US, "%.7f°", it) } ?: "Searching...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveMotionReadingsCard(
    motionData: LiveMotionData,
    availability: SensorAvailability,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("motion_readings_card"),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE MOTION SENSORS (RAW)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = Slate400
                )
                Surface(
                    color = BentoCardInner,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, BentoBorderSubtle)
                ) {
                    Text(
                        text = "m/s² & rad/s",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Accelerometer
            SensorAxisRow(
                label = "Accelerometer",
                unit = "m/s²",
                x = motionData.accelX,
                y = motionData.accelY,
                z = motionData.accelZ,
                isAvailable = availability.hasAccelerometer
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Linear Acceleration
            SensorAxisRow(
                label = "Linear Acceleration",
                unit = "m/s²",
                x = motionData.linAccelX,
                y = motionData.linAccelY,
                z = motionData.linAccelZ,
                isAvailable = availability.hasLinearAcceleration
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gyroscope
            SensorAxisRow(
                label = "Gyroscope",
                unit = "rad/s",
                x = motionData.gyroX,
                y = motionData.gyroY,
                z = motionData.gyroZ,
                isAvailable = availability.hasGyroscope
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gravity
            SensorAxisRow(
                label = "Gravity",
                unit = "m/s²",
                x = motionData.gravityX,
                y = motionData.gravityY,
                z = motionData.gravityZ,
                isAvailable = availability.hasGravity
            )

            if (availability.hasRotationVector) {
                Spacer(modifier = Modifier.height(8.dp))
                // Orientation
                SensorAxisRow(
                    label = "Orientation (Pitch/Roll/Yaw)",
                    unit = "°",
                    x = motionData.pitchDeg,
                    y = motionData.rollDeg,
                    z = motionData.azimuthDeg,
                    isAvailable = true,
                    labels = Triple("PITCH", "ROLL", "AZIM")
                )
            }
        }
    }
}

@Composable
fun SensorAxisRow(
    label: String,
    unit: String,
    x: Float,
    y: Float,
    z: Float,
    isAvailable: Boolean,
    labels: Triple<String, String, String> = Triple("X", "Y", "Z")
) {
    Surface(
        color = BentoCardInner,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BentoBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = if (isAvailable) unit else "Unavailable",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (isAvailable) Slate400 else RecordRed
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AxisValueBox(labels.first, x, isAvailable)
                AxisValueBox(labels.second, y, isAvailable)
                AxisValueBox(labels.third, z, isAvailable)
            }
        }
    }
}

@Composable
fun AxisValueBox(axis: String, value: Float, isAvailable: Boolean) {
    Surface(
        color = BentoCardBg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, BentoBorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$axis:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )
            Text(
                text = if (isAvailable) String.format(Locale.US, "%+6.2f", value) else "--",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (!isAvailable) Slate500 else if (value >= 0) PrimarySkyLight else Color(0xFFF472B6)
            )
        }
    }
}

@Composable
fun RoadRoughnessLiveCard(
    motionData: LiveMotionData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("road_roughness_card"),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ROAD ROUGHNESS & G-FORCE ANALYSIS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = Slate400
                )
                Surface(
                    color = when (motionData.roughnessLevel) {
                        com.example.roadguard.data.models.RoadRoughnessLevel.SEVERE -> RecordRed.copy(alpha = 0.2f)
                        com.example.roadguard.data.models.RoadRoughnessLevel.ROUGH -> com.example.ui.theme.PotholeOrange.copy(alpha = 0.2f)
                        com.example.roadguard.data.models.RoadRoughnessLevel.MODERATE -> com.example.ui.theme.SpeedBreakerAmber.copy(alpha = 0.2f)
                        com.example.roadguard.data.models.RoadRoughnessLevel.SMOOTH -> NormalRoadEmerald.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        when (motionData.roughnessLevel) {
                            com.example.roadguard.data.models.RoadRoughnessLevel.SEVERE -> RecordRed
                            com.example.roadguard.data.models.RoadRoughnessLevel.ROUGH -> com.example.ui.theme.PotholeOrange
                            com.example.roadguard.data.models.RoadRoughnessLevel.MODERATE -> com.example.ui.theme.SpeedBreakerAmber
                            com.example.roadguard.data.models.RoadRoughnessLevel.SMOOTH -> NormalRoadEmerald
                        }
                    )
                ) {
                    Text(
                        text = motionData.roughnessLevel.label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = when (motionData.roughnessLevel) {
                            com.example.roadguard.data.models.RoadRoughnessLevel.SEVERE -> RecordRed
                            com.example.roadguard.data.models.RoadRoughnessLevel.ROUGH -> com.example.ui.theme.PotholeOrange
                            com.example.roadguard.data.models.RoadRoughnessLevel.MODERATE -> com.example.ui.theme.SpeedBreakerAmber
                            com.example.roadguard.data.models.RoadRoughnessLevel.SMOOTH -> NormalRoadEmerald
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = BentoCardInner,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BentoBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LIVE G-FORCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f G", motionData.verticalGForce),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (motionData.verticalGForce > 1.3f) com.example.ui.theme.PotholeOrange else PrimarySkyLight
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PEAK RECORDED G",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f G", motionData.peakGForce),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (motionData.peakGForce > 1.4f) RecordRed else PrimaryCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "VIBRATION RMS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f m/s²", motionData.roughnessScore),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


