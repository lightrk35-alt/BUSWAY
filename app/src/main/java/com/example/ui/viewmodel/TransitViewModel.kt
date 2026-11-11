package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RouteRepository
import com.example.model.BusInfo
import com.example.model.BusStatus
import com.example.model.GeoPoint
import com.example.model.MapStyle
import com.example.model.NavigationStep
import com.example.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class AppScreen {
    WELCOME,
    MAP_NAVIGATION
}

class TransitViewModel : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.WELCOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _userRole = MutableStateFlow(UserRole.PASSENGER)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _buses = MutableStateFlow(RouteRepository.getInitialBuses())
    val buses: StateFlow<List<BusInfo>> = _buses.asStateFlow()

    private val _activeBusId = MutableStateFlow("bus_12")
    val activeBusId: StateFlow<String> = _activeBusId.asStateFlow()

    private val _selectedBus = MutableStateFlow<BusInfo?>(null)
    val selectedBus: StateFlow<BusInfo?> = _selectedBus.asStateFlow()

    private val _isSimulationPlaying = MutableStateFlow(true)
    val isSimulationPlaying: StateFlow<Boolean> = _isSimulationPlaying.asStateFlow()

    private val _simulationSpeed = MutableStateFlow(1f) // 1x, 2x, 4x
    val simulationSpeed: StateFlow<Float> = _simulationSpeed.asStateFlow()

    private val _mapStyle = MutableStateFlow(MapStyle.SATELLITE)
    val mapStyle: StateFlow<MapStyle> = _mapStyle.asStateFlow()

    private val _is3DTilt = MutableStateFlow(true)
    val is3DTilt: StateFlow<Boolean> = _is3DTilt.asStateFlow()

    private val _userLocation = MutableStateFlow(RouteRepository.defaultUserLocation)
    val userLocation: StateFlow<GeoPoint> = _userLocation.asStateFlow()

    private val _approachingAlert = MutableStateFlow<String?>(null)
    val approachingAlert: StateFlow<String?> = _approachingAlert.asStateFlow()

    private val _navigationStep = MutableStateFlow(RouteRepository.getNavigationStep(0.46f))
    val navigationStep: StateFlow<NavigationStep> = _navigationStep.asStateFlow()

    // Driver Mode State
    private val _driverTripStarted = MutableStateFlow(false)
    val driverTripStarted: StateFlow<Boolean> = _driverTripStarted.asStateFlow()

    private val _driverDestination = MutableStateFlow("سيق")
    val driverDestination: StateFlow<String> = _driverDestination.asStateFlow()

    private val _driverPassengerCount = MutableStateFlow(24)
    val driverPassengerCount: StateFlow<Int> = _driverPassengerCount.asStateFlow()

    private val _driverBusStatus = MutableStateFlow(BusStatus.IN_SERVICE)
    val driverBusStatus: StateFlow<BusStatus> = _driverBusStatus.asStateFlow()

    init {
        startSimulationLoop()
    }

    fun selectRole(role: UserRole) {
        _userRole.value = role
        _currentScreen.value = AppScreen.MAP_NAVIGATION
        if (role == UserRole.DRIVER) {
            _driverTripStarted.value = true
        }
    }

    fun enterDirectly() {
        _currentScreen.value = AppScreen.MAP_NAVIGATION
    }

    fun navigateToWelcome() {
        _currentScreen.value = AppScreen.WELCOME
    }

    fun toggleSimulation() {
        _isSimulationPlaying.update { !it }
    }

    fun setSimulationSpeed(multiplier: Float) {
        _simulationSpeed.value = multiplier
    }

    fun cycleSimulationSpeed() {
        _simulationSpeed.update { current ->
            when (current) {
                1f -> 2f
                2f -> 4f
                else -> 1f
            }
        }
    }

    fun toggle3DTilt() {
        _is3DTilt.update { !it }
    }

    fun cycleMapStyle() {
        _mapStyle.update { current ->
            when (current) {
                MapStyle.SATELLITE -> MapStyle.NAVIGATION_DARK
                MapStyle.NAVIGATION_DARK -> MapStyle.VECTOR_DAY
                MapStyle.VECTOR_DAY -> MapStyle.SATELLITE
            }
        }
    }

    fun selectBus(bus: BusInfo?) {
        _selectedBus.value = bus
    }

    fun setActiveBus(busId: String) {
        _activeBusId.value = busId
        val bus = _buses.value.find { it.id == busId }
        _selectedBus.value = bus
    }

    fun dismissAlert() {
        _approachingAlert.value = null
    }

    fun startDriverTrip() {
        _driverTripStarted.value = true
        _driverBusStatus.value = BusStatus.IN_SERVICE
    }

    fun stopDriverTrip() {
        _driverTripStarted.value = false
        _driverBusStatus.value = BusStatus.TEMPORARY_STOP
    }

    fun toggleDriverDestination() {
        _driverDestination.update { if (it == "سيق") "معسكر" else "سيق" }
    }

    fun setDriverBusStatus(status: BusStatus) {
        _driverBusStatus.value = status
        _buses.update { list ->
            list.map { if (it.id == "bus_12") it.copy(status = status) else it }
        }
    }

    fun adjustDriverPassengers(delta: Int) {
        _driverPassengerCount.update { (it + delta).coerceIn(0, 32) }
    }

    private fun startSimulationLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(120) // ~8 fps physics update for silky smooth animation
                if (_isSimulationPlaying.value) {
                    tickSimulation()
                }
            }
        }
    }

    private fun tickSimulation() {
        val speedFactor = _simulationSpeed.value
        val stepIncrement = 0.0012f * speedFactor

        _buses.update { list ->
            list.map { bus ->
                if (bus.status == BusStatus.IN_SERVICE) {
                    var newProgress = bus.progress + stepIncrement
                    if (newProgress > 1f) {
                        newProgress = 0f // Loop or reverse trip
                    }
                    val (newPoint, newHeading) = RouteRepository.getPositionAlongRoute(newProgress)
                    val distToUser = newPoint.distanceTo(_userLocation.value)
                    val roundedDist = (distToUser * 10).roundToInt() / 10.0
                    val eta = ((distToUser / (bus.speedKmh.coerceAtLeast(40).toDouble())) * 60).roundToInt().coerceAtLeast(1)

                    bus.copy(
                        progress = newProgress,
                        currentPoint = newPoint,
                        headingDegrees = newHeading,
                        distanceToUserKm = roundedDist,
                        etaMinutes = eta
                    )
                } else {
                    bus
                }
            }
        }

        // Update active bus navigation step
        val activeBus = _buses.value.find { it.id == _activeBusId.value }
        if (activeBus != null) {
            _navigationStep.value = RouteRepository.getNavigationStep(activeBus.progress)

            // Check if approaching user: alert when within 1.5 km
            if (activeBus.distanceToUserKm <= 1.5 && activeBus.distanceToUserKm > 0.1) {
                if (_approachingAlert.value == null) {
                    _approachingAlert.value = "حافلة رقم ${activeBus.number} تتجه نحوك الآن! (المسافة: ${activeBus.distanceToUserKm} كم)"
                }
            }
        }
    }
}
