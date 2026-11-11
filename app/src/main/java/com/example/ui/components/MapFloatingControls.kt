package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MapStyle

@Composable
fun MapFloatingControls(
    is3DTilt: Boolean,
    mapStyle: MapStyle,
    onToggle3DTilt: () -> Unit,
    onCycleMapStyle: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        // 3D Perspective Toggle Button
        FloatingMapButton(
            onClick = onToggle3DTilt,
            testTag = "toggle_3d_tilt_button",
            tooltip = "تبديل منظور 3D"
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (is3DTilt) Color(0xFF00B37E) else Color.Transparent
            ) {
                Text(
                    text = if (is3DTilt) "3D" else "2D",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (is3DTilt) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Map Layer / Satellite Style Toggle Button
        FloatingMapButton(
            onClick = onCycleMapStyle,
            testTag = "cycle_map_style_button",
            tooltip = "تبديل نمط الخريطة"
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "طبقات الخريطة",
                tint = if (mapStyle == MapStyle.SATELLITE) Color(0xFF10B981) else Color(0xFF334155),
                modifier = Modifier.size(22.dp)
            )
        }

        // Recenter on Active Bus / User
        FloatingMapButton(
            onClick = onRecenter,
            testTag = "recenter_map_button",
            tooltip = "إعادة التمركز"
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "إعادة التمركز",
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun FloatingMapButton(
    onClick: () -> Unit,
    testTag: String,
    tooltip: String,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(46.dp)
            .shadow(6.dp, CircleShape)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
