package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.RouteRepository
import com.example.model.BusInfo
import com.example.model.BusStatus
import com.example.model.GeoPoint
import com.example.model.MapStyle
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun NavigationCanvasMap(
    buses: List<BusInfo>,
    activeBusId: String,
    userLocation: GeoPoint,
    is3DTilt: Boolean,
    mapStyle: MapStyle,
    onBusClick: (BusInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // Zoom and pan state
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Pulse animation for user location and active bus
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    // Chevron flow animation along route
    val chevronPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ChevronPhase"
    )

    // Bounding box for RN17 Mascara - Sig area
    // Mascara ~ 35.39, Sig ~ 35.53; Longitude ~ 0.14 to -0.19
    val minLat = 35.36
    val maxLat = 35.63
    val minLng = -0.23
    val maxLng = 0.18

    // Center point
    val centerLat = (minLat + maxLat) / 2.0
    val centerLng = (minLng + maxLng) / 2.0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (mapStyle == MapStyle.SATELLITE) Color(0xFF0B1914) else Color(0xFF131D24))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.6f, 3.8f)
                    panOffset += pan
                }
            }
            .pointerInput(buses, zoomScale, panOffset, is3DTilt) {
                detectTapGestures { tapOffset ->
                    // Find tapped bus within ~40dp hit radius
                    val hitRadius = 50f
                    var clickedBus: BusInfo? = null
                    for (bus in buses) {
                        val busPos = projectGeo(
                            geo = bus.currentPoint,
                            centerLat = centerLat,
                            centerLng = centerLng,
                            canvasSize = Size(size.width.toFloat(), size.height.toFloat()),
                            zoom = zoomScale,
                            pan = panOffset,
                            is3DTilt = is3DTilt
                        )
                        if (hypot(tapOffset.x - busPos.x, tapOffset.y - busPos.y) <= hitRadius) {
                            clickedBus = bus
                            break
                        }
                    }
                    if (clickedBus != null) {
                        onBusClick(clickedBus)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Satellite Earth Background / Terrain features
            drawSatelliteTerrain(
                canvasW = canvasW,
                canvasH = canvasH,
                style = mapStyle,
                is3DTilt = is3DTilt,
                zoom = zoomScale,
                pan = panOffset
            )

            // 2. Draw Secondary Roads & Regional network
            drawRegionalRoads(
                centerLat = centerLat,
                centerLng = centerLng,
                canvasSize = size,
                zoom = zoomScale,
                pan = panOffset,
                is3DTilt = is3DTilt
            )

            // 3. Draw Waterways: Oued El Hammam & Oued Sig
            drawWaterways(
                centerLat = centerLat,
                centerLng = centerLng,
                canvasSize = size,
                zoom = zoomScale,
                pan = panOffset,
                is3DTilt = is3DTilt
            )

            // 4. Draw Towns / Settlements & Cultural Labels
            drawTownAreas(
                centerLat = centerLat,
                centerLng = centerLng,
                canvasSize = size,
                zoom = zoomScale,
                pan = panOffset,
                is3DTilt = is3DTilt,
                style = mapStyle
            )

            // 5. Draw Navigation Route Polyline (RN 17) with 3D Casing & Chevrons
            drawNavigationRoute(
                route = RouteRepository.routeCoordinates,
                centerLat = centerLat,
                centerLng = centerLng,
                canvasSize = size,
                zoom = zoomScale,
                pan = panOffset,
                is3DTilt = is3DTilt,
                chevronPhase = chevronPhase
            )

            // 6. Draw Stations & Interchanges along RN 17
            drawStations(
                stations = RouteRepository.stations,
                centerLat = centerLat,
                centerLng = centerLng,
                canvasSize = size,
                zoom = zoomScale,
                pan = panOffset,
                is3DTilt = is3DTilt
            )

            // 7. Draw User Location Dot with Radar Pulse
            val userPos = projectGeo(
                geo = userLocation,
                centerLat = centerLat,
                centerLng = centerLng,
                canvasSize = size,
                zoom = zoomScale,
                pan = panOffset,
                is3DTilt = is3DTilt
            )
            drawUserLocation(userPos, pulseRadius, pulseAlpha)

            // 8. Draw Buses with Directional Heading, Status Ring & Badge
            for (bus in buses) {
                val busPos = projectGeo(
                    geo = bus.currentPoint,
                    centerLat = centerLat,
                    centerLng = centerLng,
                    canvasSize = size,
                    zoom = zoomScale,
                    pan = panOffset,
                    is3DTilt = is3DTilt
                )
                val isSelected = (bus.id == activeBusId)
                drawBusMarker(
                    pos = busPos,
                    bus = bus,
                    isSelected = isSelected,
                    pulseRadius = if (isSelected) pulseRadius else 0f
                )
            }

            // 9. Draw 3D Atmospheric Horizon Glow when tilted
            if (is3DTilt) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE091410),
                            Color(0x880C1E18),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = canvasH * 0.28f
                    ),
                    topLeft = Offset.Zero,
                    size = Size(canvasW, canvasH * 0.28f)
                )
            }
        }
    }
}

/**
 * Projects (lat, lng) to 2D screen coordinates with optional 3D tilt perspective.
 */
private fun projectGeo(
    geo: GeoPoint,
    centerLat: Double,
    centerLng: Double,
    canvasSize: Size,
    zoom: Float,
    pan: Offset,
    is3DTilt: Boolean
): Offset {
    val cx = canvasSize.width / 2f + pan.x
    val cy = canvasSize.height / 2f + pan.y

    // Scale factor: Algerians coordinates Mascara (35.4) to Sig (35.53) is North-West orientation
    // We map Longitude to X (inverted so West/Sig is on right/top for convenient vertical navigation view)
    val baseScale = canvasSize.height * 2.8f * zoom

    // Route orientation: Sig is North-West of Mascara.
    // In Google Maps navigation, the driving direction points upwards.
    // Let's rotate coordinates so the journey from Mascara to Sig ascends upwards!
    val dLat = (geo.lat - centerLat)
    val dLng = (geo.lng - centerLng)

    // Rotate ~45 degrees to align highway RN17 vertically on phone screen
    val angleRad = Math.toRadians(-45.0)
    val rotX = (dLng * cos(angleRad) - dLat * sin(angleRad)) * baseScale * 1.3
    val rotY = -(dLng * sin(angleRad) + dLat * cos(angleRad)) * baseScale

    var px = (cx + rotX).toFloat()
    var py = (cy + rotY).toFloat()

    // 3D perspective foreshortening
    if (is3DTilt) {
        val horizonY = canvasSize.height * 0.18f
        val relY = (py - horizonY).coerceAtLeast(40f)
        val perspectiveRatio = (relY / (canvasSize.height * 0.82f)).coerceIn(0.35f, 1.4f)
        px = cx + (px - cx) * perspectiveRatio
        py = horizonY + (py - horizonY) * 0.72f
    }

    return Offset(px, py)
}

/**
 * Draws Satellite terrain patches, agricultural grids and mountainous shaded relief.
 */
private fun DrawScope.drawSatelliteTerrain(
    canvasW: Float,
    canvasH: Float,
    style: MapStyle,
    is3DTilt: Boolean,
    zoom: Float,
    pan: Offset
) {
    if (style == MapStyle.SATELLITE) {
        // Deep satellite terrain base
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF142920), // Fertile plain
                    Color(0xFF0F1E18), // Shaded slopes
                    Color(0xFF08120E)  // Deep terrain
                ),
                center = Offset(canvasW * 0.5f, canvasH * 0.45f),
                radius = canvasW * 1.2f
            )
        )

        // Shaded relief of Beni Chougrane mountains (Mascara highlands)
        val mountainBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF1B2E24), Color(0xFF283B2E), Color(0xFF14221B))
        )
        drawCircle(
            brush = mountainBrush,
            radius = canvasW * 0.65f * zoom,
            center = Offset(canvasW * 0.35f + pan.x * 0.3f, canvasH * 0.75f + pan.y * 0.3f)
        )

        // Agricultural plots of Habra plain (Mohammadia & Sig citrus groves)
        val groveColor = Color(0x221E5335)
        for (i in 0..6) {
            val ox = (canvasW * 0.2f + i * 85f * zoom + pan.x * 0.5f) % canvasW
            val oy = (canvasH * 0.3f + (i % 3) * 110f * zoom + pan.y * 0.5f) % canvasH
            drawRoundRect(
                color = groveColor,
                topLeft = Offset(ox, oy),
                size = Size(65f * zoom, 45f * zoom),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    } else if (style == MapStyle.NAVIGATION_DARK) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0D171C), Color(0xFF121F26), Color(0xFF0A1217))
            )
        )
    } else {
        // VECTOR_DAY
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE8ECE9), Color(0xFFF1F5F2))
            )
        )
    }
}

/**
 * Draws regional connecting highways (East-West Autoroute A3, CW 12, CW 14).
 */
private fun DrawScope.drawRegionalRoads(
    centerLat: Double,
    centerLng: Double,
    canvasSize: Size,
    zoom: Float,
    pan: Offset,
    is3DTilt: Boolean
) {
    val a3Interchange = GeoPoint(35.5620, 0.0520)
    val a3Left = GeoPoint(35.5800, -0.1500)
    val a3Right = GeoPoint(35.5450, 0.2200)

    val pA3Left = projectGeo(a3Left, centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)
    val pA3Mid = projectGeo(a3Interchange, centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)
    val pA3Right = projectGeo(a3Right, centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)

    // Autoroute Est-Ouest A3 (Highway double lane)
    val a3Path = Path().apply {
        moveTo(pA3Left.x, pA3Left.y)
        quadraticTo(pA3Mid.x, pA3Mid.y, pA3Right.x, pA3Right.y)
    }

    drawPath(
        path = a3Path,
        color = Color(0x553C4E46),
        style = Stroke(width = 12f, cap = StrokeCap.Round)
    )
    drawPath(
        path = a3Path,
        color = Color(0xAA71877E),
        style = Stroke(width = 6f, cap = StrokeCap.Round)
    )
}

/**
 * Draws Oued El Hammam and Oued Sig riverbeds.
 */
private fun DrawScope.drawWaterways(
    centerLat: Double,
    centerLng: Double,
    canvasSize: Size,
    zoom: Float,
    pan: Offset,
    is3DTilt: Boolean
) {
    val ouedP1 = projectGeo(GeoPoint(35.42, 0.16), centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)
    val ouedP2 = projectGeo(GeoPoint(35.4610, 0.0810), centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)
    val ouedP3 = projectGeo(GeoPoint(35.5750, -0.0150), centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)
    val ouedP4 = projectGeo(GeoPoint(35.62, -0.12), centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)

    val riverPath = Path().apply {
        moveTo(ouedP1.x, ouedP1.y)
        quadraticTo(ouedP2.x, ouedP2.y, ouedP3.x, ouedP3.y)
        lineTo(ouedP4.x, ouedP4.y)
    }

    drawPath(
        path = riverPath,
        color = Color(0x331E6091),
        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Draws towns along the route: Mascara, El Mamounia, Mohammadia, Sig.
 */
private fun DrawScope.drawTownAreas(
    centerLat: Double,
    centerLng: Double,
    canvasSize: Size,
    zoom: Float,
    pan: Offset,
    is3DTilt: Boolean,
    style: MapStyle
) {
    data class CityMarker(val nameAr: String, val point: GeoPoint, val radius: Float)
    val cities = listOf(
        CityMarker("معسكر (Mascara)", GeoPoint(35.3968, 0.1412), 34f),
        CityMarker("المأمونية", GeoPoint(35.4365, 0.1035), 20f),
        CityMarker("الغمري", GeoPoint(35.5340, 0.0385), 18f),
        CityMarker("المحمدية (مدينة البرتقال)", GeoPoint(35.5885, 0.0695), 32f),
        CityMarker("سيق (Sig)", GeoPoint(35.5285, -0.1930), 30f)
    )

    val labelPaint = android.graphics.Paint().apply {
        color = if (style == MapStyle.VECTOR_DAY) android.graphics.Color.DKGRAY else android.graphics.Color.WHITE
        textSize = 28f
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
    }

    for (city in cities) {
        val pos = projectGeo(city.point, centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)

        // Urban cluster glow
        drawCircle(
            color = Color(0x2A526A5E),
            radius = city.radius * zoom * 1.6f,
            center = pos
        )
        drawCircle(
            color = Color(0x55A3B899),
            radius = city.radius * zoom * 0.9f,
            center = pos
        )

        // City Label text
        drawContext.canvas.nativeCanvas.drawText(
            city.nameAr,
            pos.x + 18f,
            pos.y - 12f,
            labelPaint
        )
    }
}

/**
 * Draws the high-visibility Google Maps Navigation Route polyline.
 */
private fun DrawScope.drawNavigationRoute(
    route: List<GeoPoint>,
    centerLat: Double,
    centerLng: Double,
    canvasSize: Size,
    zoom: Float,
    pan: Offset,
    is3DTilt: Boolean,
    chevronPhase: Float
) {
    if (route.size < 2) return

    val projectedPoints = route.map {
        projectGeo(it, centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)
    }

    val path = Path().apply {
        moveTo(projectedPoints[0].x, projectedPoints[0].y)
        for (i in 1 until projectedPoints.size) {
            lineTo(projectedPoints[i].x, projectedPoints[i].y)
        }
    }

    // 1. Dark glowing highway casing
    drawPath(
        path = path,
        color = Color(0x88081A2A),
        style = Stroke(width = 24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 2. High-contrast electric blue outer line
    drawPath(
        path = path,
        color = Color(0xFF1565C0),
        style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 3. Vibrant Google Maps Navigation Royal Blue core
    drawPath(
        path = path,
        color = Color(0xFF2979FF),
        style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 4. Inner bright pulse / directional dashes
    val dashEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(24f, 28f),
        phase = -chevronPhase
    )
    drawPath(
        path = path,
        color = Color(0xFF82B1FF),
        style = Stroke(width = 4f, cap = StrokeCap.Round, pathEffect = dashEffect)
    )
}

/**
 * Draws transit stations along the route with round badges.
 */
private fun DrawScope.drawStations(
    stations: List<com.example.model.TransitStation>,
    centerLat: Double,
    centerLng: Double,
    canvasSize: Size,
    zoom: Float,
    pan: Offset,
    is3DTilt: Boolean
) {
    for (st in stations) {
        val pos = projectGeo(st.point, centerLat, centerLng, canvasSize, zoom, pan, is3DTilt)
        val outerRadius = if (st.isTerminus) 9f else 6f
        val innerRadius = if (st.isTerminus) 5f else 3.5f

        // White halo
        drawCircle(
            color = Color.White,
            radius = outerRadius,
            center = pos
        )
        // Red / Emerald core
        drawCircle(
            color = if (st.isTerminus) Color(0xFFEF4444) else Color(0xFF10B981),
            radius = innerRadius,
            center = pos
        )
    }
}

/**
 * Draws the user's location dot with animated radar pulse waves.
 */
private fun DrawScope.drawUserLocation(
    pos: Offset,
    pulseRadius: Float,
    pulseAlpha: Float
) {
    // Radar wave ripple
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = pulseAlpha),
        radius = pulseRadius,
        center = pos,
        style = Stroke(width = 3.5f)
    )

    // Outer white border
    drawCircle(
        color = Color.White,
        radius = 11f,
        center = pos
    )

    // High visibility blue dot
    drawCircle(
        color = Color(0xFF0091EA),
        radius = 7.5f,
        center = pos
    )

    // Inner bright specular highlight
    drawCircle(
        color = Color.White,
        radius = 2.5f,
        center = Offset(pos.x - 2f, pos.y - 2f)
    )
}

/**
 * Draws a high-fidelity 3D transit bus marker aligned with its heading.
 */
private fun DrawScope.drawBusMarker(
    pos: Offset,
    bus: BusInfo,
    isSelected: Boolean,
    pulseRadius: Float
) {
    val busColor = if (bus.status == BusStatus.IN_SERVICE) {
        Color(0xFF10B981) // Emerald Green for active
    } else {
        Color(0xFFD97706) // Amber/Brown for waiting
    }

    // Selected pulse halo
    if (isSelected && pulseRadius > 0f) {
        drawCircle(
            color = busColor.copy(alpha = 0.35f),
            radius = 26f + (pulseRadius * 0.4f),
            center = pos
        )
    }

    // Bus Heading Rotation
    withTransform({
        rotate(degrees = bus.headingDegrees, pivot = pos)
    }) {
        // Bus body shadow
        drawRoundRect(
            color = Color(0x66000000),
            topLeft = Offset(pos.x - 13f, pos.y - 21f),
            size = Size(26f, 44f),
            cornerRadius = CornerRadius(7f, 7f)
        )

        // Bus body outline
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(pos.x - 12f, pos.y - 20f),
            size = Size(24f, 40f),
            cornerRadius = CornerRadius(6f, 6f),
            style = Fill
        )

        // Bus colored roof
        drawRoundRect(
            color = busColor,
            topLeft = Offset(pos.x - 10f, pos.y - 18f),
            size = Size(20f, 36f),
            cornerRadius = CornerRadius(5f, 5f),
            style = Fill
        )

        // Front Windshield (curved dark cyan glass)
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(pos.x - 8f, pos.y - 16f),
            size = Size(16f, 8f),
            cornerRadius = CornerRadius(2.5f, 2.5f)
        )

        // Headlights (glowing yellow/white)
        drawCircle(color = Color(0xFFFEF08A), radius = 2f, center = Offset(pos.x - 6f, pos.y - 17f))
        drawCircle(color = Color(0xFFFEF08A), radius = 2f, center = Offset(pos.x + 6f, pos.y - 17f))

        // Side mirrors
        drawRect(color = Color.White, topLeft = Offset(pos.x - 13f, pos.y - 14f), size = Size(2f, 4f))
        drawRect(color = Color.White, topLeft = Offset(pos.x + 11f, pos.y - 14f), size = Size(2f, 4f))

        // Rear window
        drawRect(color = Color(0xFF0F172A), topLeft = Offset(pos.x - 7f, pos.y + 11f), size = Size(14f, 3f))
    }

    // Floating Badge (Bus Number e.g. "12") - drawn unrotated for readability
    val badgePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    // Badge pill background
    val pillCenterY = pos.y + 30f
    drawRoundRect(
        color = if (isSelected) Color(0xFF092E24) else Color(0xFF1E293B),
        topLeft = Offset(pos.x - 22f, pillCenterY - 11f),
        size = Size(44f, 22f),
        cornerRadius = CornerRadius(11f, 11f)
    )
    drawRoundRect(
        color = busColor,
        topLeft = Offset(pos.x - 22f, pillCenterY - 11f),
        size = Size(44f, 22f),
        cornerRadius = CornerRadius(11f, 11f),
        style = Stroke(width = 2f)
    )

    drawContext.canvas.nativeCanvas.drawText(
        bus.number,
        pos.x,
        pillCenterY + 7f,
        badgePaint
    )
}
