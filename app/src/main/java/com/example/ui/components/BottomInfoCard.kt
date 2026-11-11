package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusInfo
import com.example.model.BusStatus

@Composable
fun BottomInfoCard(
    activeBus: BusInfo?,
    isSimulationPlaying: Boolean,
    simulationSpeed: Float,
    onTogglePlayPause: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenBusDetails: () -> Unit,
    onCloseNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val etaMinutes = activeBus?.etaMinutes ?: 14
    val distanceKm = activeBus?.distanceToUserKm ?: 7.6
    val currentProgress = activeBus?.progress ?: 0.46f

    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .testTag("bottom_info_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Row: Big Remaining Time & Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$etaMinutes دقيقة",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF047857) // Crisp green ETA
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = "حركة سير سلسة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "%.1f كم • الوصول 16:55 • طريق معسكر - سيق".format(distanceKm),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }

                // Close Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = onCloseNavigation,
                        modifier = Modifier.testTag("close_navigation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route Progress Bar
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF00B37E),
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Control Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Simulation
                FilledTonalButton(
                    onClick = onTogglePlayPause,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSimulationPlaying) Color(0xFFF1F5F9) else Color(0xFFDCFCE7),
                        contentColor = if (isSimulationPlaying) Color(0xFF334155) else Color(0xFF15803D)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("simulation_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isSimulationPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSimulationPlaying) "إيقاف مؤقت" else "استئناف",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Simulation Speed Toggle (1x, 2x, 4x)
                FilledTonalButton(
                    onClick = onCycleSpeed,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(46.dp)
                        .testTag("simulation_speed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${simulationSpeed.toInt()}x سرعة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bus Details Button
                Button(
                    onClick = onOpenBusDetails,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D3328),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .testTag("bus_details_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF34D399)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "بيانات الحافلة",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
