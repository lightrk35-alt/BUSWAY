package com.example

import com.example.data.RouteRepository
import com.example.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun geoPointDistance_isAccurate() {
        // Mascara to Sig distance should be approx 40-55 km
        val mascara = RouteRepository.routeCoordinates.first()
        val sig = RouteRepository.routeCoordinates.last()
        val dist = mascara.distanceTo(sig)
        assertTrue("Distance between Mascara and Sig should be around 35-50 km, got $dist", dist in 30.0..60.0)
    }

    @Test
    fun routeInterpolation_returnsValidPoints() {
        val (pointStart, _) = RouteRepository.getPositionAlongRoute(0.0f)
        assertEquals(RouteRepository.routeCoordinates.first().lat, pointStart.lat, 0.001)

        val (pointMid, headingMid) = RouteRepository.getPositionAlongRoute(0.5f)
        assertTrue("Heading should be within 0..360", headingMid in 0f..360f)

        val (pointEnd, _) = RouteRepository.getPositionAlongRoute(1.0f)
        assertEquals(RouteRepository.routeCoordinates.last().lat, pointEnd.lat, 0.001)
    }

    @Test
    fun initialBuses_areConfigured() {
        val buses = RouteRepository.getInitialBuses()
        assertEquals(3, buses.size)
        assertTrue(buses.any { it.number == "12" })
        assertTrue(buses.any { it.number == "08" })
    }
}
