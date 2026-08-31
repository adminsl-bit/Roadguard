package com.example.roadguard.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roadguard.ui.components.ActiveRecordingPanel
import com.example.roadguard.ui.components.GpsStatusCard
import com.example.roadguard.ui.components.LiveMotionReadingsCard
import com.example.roadguard.ui.components.PermissionRationaleCard
import com.example.roadguard.ui.components.SensorAvailabilityRow
import com.example.roadguard.ui.components.SessionItemCard
import com.example.roadguard.ui.components.SessionSavedDialog
import com.example.roadguard.ui.components.StartRecordingPanel
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoBorderSubtle
import com.example.ui.theme.BentoCardBg
import com.example.ui.theme.BentoCardInner
import com.example.ui.theme.NormalRoadEmerald
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimarySky
import com.example.ui.theme.PrimarySkyLight
import com.example.ui.theme.RecordRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainRoadGuardScreen(
    viewModel: RoadGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sensorAvailability by viewModel.sensorAvailability.collectAsStateWithLifecycle()
    val liveMotion by viewModel.liveMotion.collectAsStateWithLifecycle()
    val liveGps by viewModel.liveGps.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val sessionHistory by viewModel.sessionHistory.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val lastSavedSession by viewModel.lastSavedSession.collectAsStateWithLifecycle()
    val eventNotification by viewModel.eventNotification.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        viewModel.onLocationPermissionResult(fineLocation || coarseLocation)
    }

    // Check permission on launch
    LaunchedEffect(Unit) {
        val fineCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        val coarseCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED

        val isGranted = fineCheck || coarseCheck
        viewModel.onLocationPermissionResult(isGranted)
        if (!isGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Auto-switch to live recording tab if recording starts
    LaunchedEffect(activeSession.isRecording) {
        if (activeSession.isRecording) {
            selectedTab = 0
        }
    }

    // Predictive back handler: switch back to live sensors tab if on history tab
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    // Dismiss session saved dialog on back
    BackHandler(enabled = lastSavedSession != null) {
        viewModel.dismissSavedSessionDialog()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen"),
        containerColor = BentoBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .background(BentoCardBg)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = PrimarySky.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PrimarySkyLight.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = PrimarySkyLight,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(20.dp)
                                )
                            }
                            Text(
                                text = "RoadGuard Sensor Test",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                ),
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BentoCardBg
                    )
                )

                // Bento-style Segmented Switch: Live Sensors vs Session History
                Surface(
                    color = BentoCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = BentoCardBg,
                        contentColor = PrimarySkyLight,
                        divider = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(BentoBorder)
                            )
                        },
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = if (selectedTab == 0 && activeSession.isRecording) RecordRed else PrimarySkyLight,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = if (activeSession.isRecording) "● ACTIVE RECORDING" else "LIVE SENSORS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    letterSpacing = 0.6.sp,
                                    color = if (selectedTab == 0) (if (activeSession.isRecording) RecordRed else PrimarySkyLight) else Slate400
                                )
                            },
                            modifier = Modifier.testTag("tab_live_sensors")
                        )

                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "SESSION HISTORY",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        letterSpacing = 0.6.sp,
                                        color = if (selectedTab == 1) PrimarySkyLight else Slate400
                                    )
                                    if (sessionHistory.isNotEmpty()) {
                                        Surface(
                                            color = if (selectedTab == 1) PrimarySky else BentoCardInner,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, BentoBorderSubtle)
                                        ) {
                                            Text(
                                                text = "${sessionHistory.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("tab_session_history")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                // Live Sensor & Recording View in Bento Grid
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Location Permission warning banner if not granted
                    if (!hasLocationPermission) {
                        item {
                            PermissionRationaleCard(
                                onRequestPermission = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            )
                        }
                    }

                    // Recording controls (Bento START button or ACTIVE recording HUD)
                    item {
                        if (activeSession.isRecording) {
                            ActiveRecordingPanel(
                                sessionState = activeSession,
                                onMarkEvent = { eventType ->
                                    viewModel.markGroundTruth(eventType)
                                },
                                onStopClick = {
                                    viewModel.stopRecording()
                                }
                            )
                        } else {
                            StartRecordingPanel(
                                onStartClick = {
                                    if (!hasLocationPermission) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                    viewModel.startRecording()
                                },
                                autoDetectEnabled = activeSession.autoDetectEnabled,
                                sensitivity = activeSession.sensitivity,
                                onToggleAutoDetect = { enabled ->
                                    viewModel.toggleAutoDetect(enabled)
                                },
                                onSelectSensitivity = { sens ->
                                    viewModel.setSensitivity(sens)
                                }
                            )
                        }
                    }

                    // Road Roughness & Dynamic G-Force Analysis Card
                    item {
                        com.example.roadguard.ui.components.RoadRoughnessLiveCard(
                            motionData = liveMotion
                        )
                    }

                    // Sensor Hardware Status Bento Card
                    item {
                        SensorAvailabilityRow(availability = sensorAvailability)
                    }

                    // GPS Status & Live Speed Bento Card
                    item {
                        GpsStatusCard(
                            gpsData = liveGps,
                            hasPermission = hasLocationPermission,
                            onGrantPermissionClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )
                    }

                    // Live Motion Sensors Readings Bento Card
                    item {
                        LiveMotionReadingsCard(
                            motionData = liveMotion,
                            availability = sensorAvailability
                        )
                    }
                }
            } else {
                // Session History View
                if (sessionHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = BentoCardBg,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BentoBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = BentoCardInner,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BentoBorderSubtle)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = PrimarySkyLight,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .size(36.dp)
                                    )
                                }
                                Text(
                                    text = "No Recording Sessions Yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap 'START RECORDING' on the live sensors tab to collect road disturbance data.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                            .testTag("session_history_list"),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sessionHistory, key = { it.id }) { session ->
                            SessionItemCard(
                                session = session,
                                onExportCsv = {
                                    val intent = viewModel.exportCsv(session)
                                    intent?.let { context.startActivity(it) }
                                },
                                onExportJson = {
                                    val intent = viewModel.exportJson(session)
                                    intent?.let { context.startActivity(it) }
                                },
                                onDelete = {
                                    viewModel.deleteSession(session)
                                }
                            )
                        }
                    }
                }
            }

            // Floating Event Marked Toast / Notification
            AnimatedVisibility(
                visible = eventNotification != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                val bannerColor = when {
                    eventNotification?.contains("POTHOLE") == true -> com.example.ui.theme.PotholeOrange
                    eventNotification?.contains("SPEED BREAKER") == true -> com.example.ui.theme.SpeedBreakerAmber
                    eventNotification?.contains("ROUGH") == true -> com.example.ui.theme.RoughRoadViolet
                    else -> NormalRoadEmerald
                }

                Surface(
                    color = BentoCardBg,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, bannerColor),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = bannerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = eventNotification ?: "",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Saved Session Dialog
    lastSavedSession?.let { savedSession ->
        SessionSavedDialog(
            session = savedSession,
            onExportCsv = {
                val intent = viewModel.exportCsv(savedSession)
                intent?.let { context.startActivity(it) }
            },
            onExportJson = {
                val intent = viewModel.exportJson(savedSession)
                intent?.let { context.startActivity(it) }
            },
            onDismiss = {
                viewModel.dismissSavedSessionDialog()
            }
        )
    }
}

