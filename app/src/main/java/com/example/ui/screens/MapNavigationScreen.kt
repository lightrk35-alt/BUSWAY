package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusStatus
import com.example.model.UserRole
import com.example.ui.components.BottomInfoCard
import com.example.ui.components.BusDetailBottomSheet
import com.example.ui.components.DriverHudPanel
import com.example.ui.components.MapFloatingControls
import com.example.ui.components.NavigationCanvasMap
import com.example.ui.components.TopNavigationHeader
import com.example.ui.viewmodel.TransitViewModel

@Composable
fun MapNavigationScreen(
    viewModel: TransitViewModel,
    modifier: Modifier = Modifier
) {
    val buses by viewModel.buses.collectAsState()
    val activeBusId by viewModel.activeBusId.collectAsState()
    val selectedBus by viewModel.selectedBus.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isSimulationPlaying by viewModel.isSimulationPlaying.collectAsState()
    val simulationSpeed by viewModel.simulationSpeed.collectAsState()
    val mapStyle by viewModel.mapStyle.collectAsState()
    val is3DTilt by viewModel.is3DTilt.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val approachingAlert by viewModel.approachingAlert.collectAsState()
    val navigationStep by viewModel.navigationStep.collectAsState()

    // Driver mode states
    val isDriverTripStarted by viewModel.driverTripStarted.collectAsState()
    val driverDestination by viewModel.driverDestination.collectAsState()
    val driverPassengerCount by viewModel.driverPassengerCount.collectAsState()
    val driverBusStatus by viewModel.driverBusStatus.collectAsState()

    val activeBus = buses.find { it.id == activeBusId } ?: buses.firstOrNull()

    // BackHandler to return to welcome screen cleanly
    BackHandler {
        viewModel.navigateToWelcome()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Fullscreen Google Maps Navigation 3D Satellite Canvas
        NavigationCanvasMap(
            buses = buses,
            activeBusId = activeBusId,
            userLocation = userLocation,
            is3DTilt = is3DTilt,
            mapStyle = mapStyle,
            onBusClick = { bus ->
                viewModel.selectBus(bus)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Navigation Header & Alert Banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
        ) {
            // Top Bar with Back Button, DEMO pill and Role Switcher
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F2620).copy(alpha = 0.85f),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.navigateToWelcome() },
                        modifier = Modifier.testTag("back_to_welcome_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع للرئيسية",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // DEMO Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0E382B).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        text = "DEMO • محاكاة حية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Role Switcher Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F2620).copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF264E40)),
                    modifier = Modifier.clickable {
                        val newRole = if (userRole == UserRole.PASSENGER) UserRole.DRIVER else UserRole.PASSENGER
                        viewModel.selectRole(newRole)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (userRole == UserRole.DRIVER) Icons.Default.DirectionsBus else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (userRole == UserRole.DRIVER) "وضع السائق" else "وضع الراكب",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Google Maps Navigation Header Card
            TopNavigationHeader(
                step = navigationStep,
                currentSpeedKmh = activeBus?.speedKmh ?: 68,
                approachingAlert = approachingAlert,
                onDismissAlert = { viewModel.dismissAlert() }
            )
        }

        // 3. Floating Side Controls (3D, Layers, Recenter)
        MapFloatingControls(
            is3DTilt = is3DTilt,
            mapStyle = mapStyle,
            onToggle3DTilt = { viewModel.toggle3DTilt() },
            onCycleMapStyle = { viewModel.cycleMapStyle() },
            onRecenter = {
                // Toggling tilt or resetting re-centers
                viewModel.setActiveBus(activeBusId)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 130.dp)
        )

        // 4. Bottom Controls: Driver HUD or Passenger Card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            if (userRole == UserRole.DRIVER) {
                DriverHudPanel(
                    isTripStarted = isDriverTripStarted,
                    destination = driverDestination,
                    passengerCount = driverPassengerCount,
                    currentStatus = driverBusStatus,
                    onStartTrip = { viewModel.startDriverTrip() },
                    onStopTrip = { viewModel.stopDriverTrip() },
                    onToggleDestination = { viewModel.toggleDriverDestination() },
                    onStatusChange = { viewModel.setDriverBusStatus(it) },
                    onPassengerDelta = { viewModel.adjustDriverPassengers(it) },
                    onSwitchToPassengerMode = { viewModel.selectRole(UserRole.PASSENGER) }
                )
            } else {
                BottomInfoCard(
                    activeBus = activeBus,
                    isSimulationPlaying = isSimulationPlaying,
                    simulationSpeed = simulationSpeed,
                    onTogglePlayPause = { viewModel.toggleSimulation() },
                    onCycleSpeed = { viewModel.cycleSimulationSpeed() },
                    onOpenBusDetails = {
                        if (activeBus != null) viewModel.selectBus(activeBus)
                    },
                    onCloseNavigation = { viewModel.navigateToWelcome() }
                )
            }
        }

        // 5. Bus Details Modal Bottom Sheet
        if (selectedBus != null) {
            BusDetailBottomSheet(
                bus = selectedBus!!,
                onDismiss = { viewModel.selectBus(null) },
                onTrackBus = { busId ->
                    viewModel.setActiveBus(busId)
                }
            )
        }
    }
}
