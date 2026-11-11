package com.example.data

import com.example.model.BusInfo
import com.example.model.BusStatus
import com.example.model.GeoPoint
import com.example.model.ManeuverType
import com.example.model.NavigationStep
import com.example.model.TransitStation
import kotlin.math.roundToInt

object RouteRepository {

    // Realistic waypoints along Route Nationale 17 (RN 17) between Mascara and Sig
    val routeCoordinates: List<GeoPoint> = listOf(
        GeoPoint(35.3968, 0.1412), // 0: Gare Routière Mascara (Bab Ali)
        GeoPoint(35.4025, 0.1360), // 1: Rond-point sortie Mascara Nord
        GeoPoint(35.4110, 0.1280), // 2: Début RN 17 vers El Mamounia
        GeoPoint(35.4230, 0.1190), // 3: Tronçon agricole
        GeoPoint(35.4365, 0.1035), // 4: El Mamounia centre (المأمونية)
        GeoPoint(35.4490, 0.0920), // 5: Sortie Ouest El Mamounia
        GeoPoint(35.4610, 0.0810), // 6: Pont sur Oued El Hammam
        GeoPoint(35.4745, 0.0715), // 7: Carrefour Bouhanifia / Ain Fares
        GeoPoint(35.4890, 0.0630), // 8: Tronçon vallonné des Beni Chougrane
        GeoPoint(35.5030, 0.0540), // 9: Zone de virages panoramiques
        GeoPoint(35.5180, 0.0460), // 10: Approche El Ghomri
        GeoPoint(35.5340, 0.0385), // 11: El Ghomri (الغمري)
        GeoPoint(35.5480, 0.0430), // 12: Plaine de l'Habra
        GeoPoint(35.5620, 0.0520), // 13: Échangeur Autoroute Est-Ouest A3
        GeoPoint(35.5760, 0.0610), // 14: Entrée Sud Mohammadia
        GeoPoint(35.5885, 0.0695), // 15: Mohammadia Centre (محطة المحمدية)
        GeoPoint(35.5940, 0.0510), // 16: Sortie Nord-Ouest Mohammadia vers RN 17 / RN 97
        GeoPoint(35.5860, 0.0240), // 17: Tronçon des orangeraies
        GeoPoint(35.5750, -0.0150), // 18: Vallée de l'Oued Sig
        GeoPoint(35.5640, -0.0580), // 19: Commune rurale
        GeoPoint(35.5520, -0.1020), // 20: Approche Est de Sig
        GeoPoint(35.5410, -0.1450), // 21: Rond-point entrée de Sig
        GeoPoint(35.5320, -0.1780), // 22: Boulevard principal de Sig
        GeoPoint(35.5285, -0.1930)  // 23: Gare Routière de Sig (محطة سيق)
    )

    // Stations list with cultural and geographic landmarks in Mascara province
    val stations: List<TransitStation> = listOf(
        TransitStation(
            id = "st_mascara",
            nameAr = "محطة معسكر المركزية (باب علي)",
            nameFr = "Gare Routière Mascara",
            point = routeCoordinates.first(),
            isTerminus = true,
            iconDescription = "انطلاق خط معسكر"
        ),
        TransitStation(
            id = "st_mamounia",
            nameAr = "المأمونية (المحطة البلدية)",
            nameFr = "El Mamounia",
            point = routeCoordinates[4],
            iconDescription = "توقف منتظم"
        ),
        TransitStation(
            id = "st_ghomri",
            nameAr = "الغمري - مفرق بوحنيفية",
            nameFr = "El Ghomri",
            point = routeCoordinates[11],
            iconDescription = "محطة تبادل"
        ),
        TransitStation(
            id = "st_mohammadia",
            nameAr = "المحمدية - محطة النقل البري",
            nameFr = "Mohammadia",
            point = routeCoordinates[15],
            iconDescription = "محطة رئيسية (مدينة البرتقال)"
        ),
        TransitStation(
            id = "st_sig",
            nameAr = "محطة سيق لنقل المسافرين",
            nameFr = "Gare Routière Sig",
            point = routeCoordinates.last(),
            isTerminus = true,
            iconDescription = "المحطة النهائية"
        )
    )

    // User's simulated waiting location (near Mohammadia stop along the route)
    val defaultUserLocation = GeoPoint(35.5810, 0.0650)

    /**
     * Interpolates position along the route for progress in [0.0, 1.0].
     * Returns the GeoPoint and tangent angle in degrees.
     */
    fun getPositionAlongRoute(progress: Float): Pair<GeoPoint, Float> {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val n = routeCoordinates.size - 1
        val scaledIndex = clampedProgress * n
        val index = scaledIndex.toInt().coerceIn(0, n - 1)
        val fraction = (scaledIndex - index).coerceIn(0f, 1f)

        val p1 = routeCoordinates[index]
        val p2 = routeCoordinates[(index + 1).coerceAtMost(routeCoordinates.size - 1)]

        val lat = p1.lat + (p2.lat - p1.lat) * fraction
        val lng = p1.lng + (p2.lng - p1.lng) * fraction
        val currentPoint = GeoPoint(lat, lng)
        val heading = p1.bearingTo(p2)

        return Pair(currentPoint, heading)
    }

    /**
     * Returns total route distance in km.
     */
    fun getTotalRouteDistanceKm(): Double {
        var total = 0.0
        for (i in 0 until routeCoordinates.size - 1) {
            total += routeCoordinates[i].distanceTo(routeCoordinates[i + 1])
        }
        return total
    }

    /**
     * Initial fleet of buses.
     */
    fun getInitialBuses(): List<BusInfo> {
        val pos12 = getPositionAlongRoute(0.46f)
        val pos08 = getPositionAlongRoute(0.04f)
        val pos15 = getPositionAlongRoute(0.82f)

        return listOf(
            BusInfo(
                id = "bus_12",
                number = "12",
                lineName = "سريع معسكر ⟵ سيق",
                origin = "معسكر",
                destination = "سيق",
                driverName = "أحمد مرابط",
                plateNumber = "00124-129-29",
                status = BusStatus.IN_SERVICE,
                progress = 0.46f,
                speedKmh = 68,
                passengers = 24,
                maxCapacity = 32,
                departingInMinutes = 0,
                priceDzd = 150,
                currentPoint = pos12.first,
                headingDegrees = pos12.second,
                distanceToUserKm = 7.6,
                etaMinutes = 14,
                isAcOn = true
            ),
            BusInfo(
                id = "bus_08",
                number = "08",
                lineName = "معسكر ⟵ المحمدية ⟵ سيق",
                origin = "معسكر",
                destination = "سيق",
                driverName = "كريم بن عودة",
                plateNumber = "01850-118-29",
                status = BusStatus.WAITING_PASSENGERS,
                progress = 0.04f,
                speedKmh = 0,
                passengers = 9,
                maxCapacity = 30,
                departingInMinutes = 8,
                priceDzd = 150,
                currentPoint = pos08.first,
                headingDegrees = pos08.second,
                distanceToUserKm = 24.2,
                etaMinutes = 35,
                isAcOn = true
            ),
            BusInfo(
                id = "bus_15",
                number = "15",
                lineName = "سيق ⟵ معسكر (رحلة العودة)",
                origin = "سيق",
                destination = "معسكر",
                driverName = "مولاي عبد القادر",
                plateNumber = "00789-122-29",
                status = BusStatus.IN_SERVICE,
                progress = 0.82f,
                speedKmh = 72,
                passengers = 28,
                maxCapacity = 35,
                departingInMinutes = 0,
                priceDzd = 150,
                currentPoint = pos15.first,
                headingDegrees = (pos15.second + 180f) % 360f,
                distanceToUserKm = 18.5,
                etaMinutes = 22,
                isAcOn = true
            )
        )
    }

    /**
     * Computes real-time dynamic Google Maps navigation instruction based on progress.
     */
    fun getNavigationStep(progress: Float): NavigationStep {
        return when {
            progress < 0.15f -> NavigationStep(
                instructionAr = "انعطف يميناً نحو مخرج معسكر الشمالي (RN 17)",
                distanceMeters = 350,
                maneuver = ManeuverType.TURN_RIGHT,
                roadName = "الطريق الوطني رقم 17",
                nextUpcomingAr = "ثم بعد 2.4 كم واصل السير مستقيماً نحو المأمونية"
            )
            progress < 0.35f -> NavigationStep(
                instructionAr = "واصل السير على الطريق الوطني 17 عبر المأمونية",
                distanceMeters = 800,
                maneuver = ManeuverType.STRAIGHT,
                roadName = "RN 17 - المأمونية",
                nextUpcomingAr = "ثم بعد 5 كم اقترب من وادي الحمام ومفرق بوحنيفية"
            )
            progress < 0.55f -> NavigationStep(
                instructionAr = "ادخل المدار واخرج في المخرج الثاني نحو المحمدية",
                distanceMeters = 400,
                maneuver = ManeuverType.ROUNDABOUT,
                roadName = "تقاطع الغمري / المحمدية",
                nextUpcomingAr = "ثم بعد 1.2 كم توجه مباشرة إلى مدخل المحمدية"
            )
            progress < 0.75f -> NavigationStep(
                instructionAr = "انعطف يساراً باتجاه مسار سيق السريع",
                distanceMeters = 250,
                maneuver = ManeuverType.TURN_LEFT,
                roadName = "طريق المحمدية - سيق",
                nextUpcomingAr = "ثم واصل لمسافة 14 كم عبر سهل الهبرة"
            )
            progress < 0.92f -> NavigationStep(
                instructionAr = "واصل السير مستقيماً واقترب من مدخل مدينة سيق",
                distanceMeters = 600,
                maneuver = ManeuverType.STRAIGHT,
                roadName = "مدخل سيق الشرقي",
                nextUpcomingAr = "ثم ادخل المحطة البرية لنقل المسافرين"
            )
            else -> NavigationStep(
                instructionAr = "الوصول إلى الوجهة: محطة سيق المركزية",
                distanceMeters = 50,
                maneuver = ManeuverType.DESTINATION,
                roadName = "محطة سيق لنقل المسافرين",
                nextUpcomingAr = "تمت الرحلة بنجاح • شكراً لاختياركم ماسكير ترانزيت"
            )
        }
    }
}
