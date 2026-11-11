package com.example.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic coordinate with geospatial helper utilities.
 */
data class GeoPoint(
    val lat: Double,
    val lng: Double
) {
    /**
     * Calculates distance in kilometers between two points using the Haversine formula.
     */
    fun distanceTo(other: GeoPoint): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(other.lat - lat)
        val dLng = Math.toRadians(other.lng - lng)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates bearing in degrees from this point to other point (0° = North, 90° = East).
     */
    fun bearingTo(other: GeoPoint): Float {
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(other.lat)
        val dLng = Math.toRadians(other.lng - lng)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val brng = Math.toDegrees(atan2(y, x))
        return ((brng + 360) % 360).toFloat()
    }
}

data class TransitStation(
    val id: String,
    val nameAr: String,
    val nameFr: String,
    val point: GeoPoint,
    val isTerminus: Boolean = false,
    val iconDescription: String = ""
)

enum class BusStatus(val labelAr: String, val isGreen: Boolean) {
    IN_SERVICE("في الخدمة", true),
    WAITING_PASSENGERS("تنتظر الركاب", false),
    TEMPORARY_STOP("متوقف مؤقتاً", false)
}

data class BusInfo(
    val id: String,
    val number: String,
    val lineName: String,
    val origin: String,
    val destination: String,
    val driverName: String,
    val plateNumber: String,
    var status: BusStatus,
    var progress: Float, // 0.0f (Mascara) to 1.0f (Sig)
    var speedKmh: Int,
    var passengers: Int,
    val maxCapacity: Int = 32,
    val departingInMinutes: Int = 0,
    val priceDzd: Int = 150,
    var currentPoint: GeoPoint,
    var headingDegrees: Float = 0f,
    var distanceToUserKm: Double = 0.0,
    var etaMinutes: Int = 0,
    val isAcOn: Boolean = true
)

enum class ManeuverType {
    STRAIGHT,
    TURN_RIGHT,
    TURN_LEFT,
    ROUNDABOUT,
    MERGE,
    DESTINATION
}

data class NavigationStep(
    val instructionAr: String,
    val distanceMeters: Int,
    val maneuver: ManeuverType,
    val roadName: String,
    val nextUpcomingAr: String
)

enum class UserRole {
    PASSENGER,
    DRIVER
}

enum class MapStyle(val titleAr: String) {
    SATELLITE("أقمار صناعية"),
    NAVIGATION_DARK("ملاحة ليلية"),
    VECTOR_DAY("خريطة نهارية")
}
