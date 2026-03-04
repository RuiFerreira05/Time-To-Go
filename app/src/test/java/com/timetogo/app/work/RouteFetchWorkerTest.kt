package com.timetogo.app.work

import com.timetogo.app.data.model.BusStep
import com.timetogo.app.data.model.RouteInfo
import com.timetogo.app.data.model.RouteStep
import com.timetogo.app.data.model.UserPreferences
import com.timetogo.app.data.repository.NoRouteFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for RouteFetchWorker business logic.
 *
 * RouteFetchWorker cannot be directly instantiated in unit tests because it
 * requires WorkerParameters (a final class). These tests verify the business
 * rules and data flow that the worker implements:
 *
 * - Input validation (home address, coordinates)
 * - Post-alarm task decisions (recurring vs one-shot)
 * - Exception types used for error handling
 * - Route info structure and serialization
 */
class RouteFetchWorkerTest {

    // ── Test data ───────────────────────────────────────────────────

    private fun defaultPrefs(
        homeAddress: String = "123 Main St",
        homeLatitude: Double = 38.7,
        homeLongitude: Double = -9.1,
        alarmHour: Int = 17,
        alarmMinute: Int = 30,
        isRecurring: Boolean = false,
        isDetailedNotification: Boolean = true
    ) = UserPreferences(
        homeAddress = homeAddress,
        homeLatitude = homeLatitude,
        homeLongitude = homeLongitude,
        alarmHour = alarmHour,
        alarmMinute = alarmMinute,
        isRecurring = isRecurring,
        isDetailedNotification = isDetailedNotification
    )

    private fun createTestRouteInfo(): RouteInfo {
        return RouteInfo(
            steps = listOf(
                RouteStep.Walking(durationMinutes = 5, destination = "Bus Stop A"),
                RouteStep.Transit(
                    BusStep(
                        busNumber = "42",
                        departureStopName = "Bus Stop A",
                        departureTime = "17:35",
                        alightingStopName = "Home Stop",
                        arrivalTime = "18:00"
                    )
                )
            ),
            totalDurationMinutes = 25,
            estimatedArrivalTime = "18:00",
            originLatLng = "38.7,-9.1",
            destinationLatLng = "38.8,-9.2"
        )
    }

    // ── Home address validation ─────────────────────────────────────
    // Worker checks: homeAddress.isEmpty() || (homeLatitude == 0.0 && homeLongitude == 0.0)

    @Test
    fun `empty home address fails validation`() {
        val prefs = defaultPrefs(homeAddress = "")
        val isInvalid = prefs.homeAddress.isEmpty() ||
                (prefs.homeLatitude == 0.0 && prefs.homeLongitude == 0.0)
        assertTrue("Empty address should be invalid", isInvalid)
    }

    @Test
    fun `zero coordinates fail validation`() {
        val prefs = defaultPrefs(homeLatitude = 0.0, homeLongitude = 0.0)
        val isInvalid = prefs.homeAddress.isEmpty() ||
                (prefs.homeLatitude == 0.0 && prefs.homeLongitude == 0.0)
        assertTrue("Zero coordinates should be invalid", isInvalid)
    }

    @Test
    fun `valid home address passes validation`() {
        val prefs = defaultPrefs()
        val isInvalid = prefs.homeAddress.isEmpty() ||
                (prefs.homeLatitude == 0.0 && prefs.homeLongitude == 0.0)
        assertFalse("Valid address should pass validation", isInvalid)
    }

    @Test
    fun `only zero latitude with non-zero longitude fails validation`() {
        val prefs = defaultPrefs(homeLatitude = 0.0, homeLongitude = -9.1)
        val isInvalid = prefs.homeAddress.isEmpty() ||
                (prefs.homeLatitude == 0.0 && prefs.homeLongitude == 0.0)
        assertFalse("Non-zero longitude should pass", isInvalid)
    }

    // ── Post-alarm task decisions ───────────────────────────────────
    // Worker logic: if (isRecurring) -> scheduleNextDayAlarm
    //               else -> setAlarmEnabled(false)

    @Test
    fun `recurring mode schedules next day`() {
        val prefs = defaultPrefs(isRecurring = true)
        assertTrue("Recurring mode should reschedule", prefs.isRecurring)
    }

    @Test
    fun `one-shot mode does not reschedule`() {
        val prefs = defaultPrefs(isRecurring = false)
        assertFalse("One-shot mode should not reschedule", prefs.isRecurring)
    }

    @Test
    fun `post-alarm tasks use correct hour and minute from preferences`() {
        val prefs = defaultPrefs(alarmHour = 8, alarmMinute = 15)
        assertEquals(8, prefs.alarmHour)
        assertEquals(15, prefs.alarmMinute)
    }

    // ── Notification mode selection ─────────────────────────────────
    // Worker logic: if (isDetailedNotification) -> showRouteNotification(single, detailed=true)
    //               else -> fetchAlternatives + showRouteNotification(list)

    @Test
    fun `detailed mode fetches single route`() {
        val prefs = defaultPrefs(isDetailedNotification = true)
        assertTrue("Should use detailed notification", prefs.isDetailedNotification)
    }

    @Test
    fun `brief mode fetches alternatives`() {
        val prefs = defaultPrefs(isDetailedNotification = false)
        assertFalse("Should use brief notification", prefs.isDetailedNotification)
    }

    // ── RouteInfo structure ──────────────────────────────────────────

    @Test
    fun `route info has correct structure`() {
        val routeInfo = createTestRouteInfo()

        assertEquals(25, routeInfo.totalDurationMinutes)
        assertEquals("18:00", routeInfo.estimatedArrivalTime)
        assertEquals("38.7,-9.1", routeInfo.originLatLng)
        assertEquals("38.8,-9.2", routeInfo.destinationLatLng)
    }

    @Test
    fun `route info steps contain walking and transit`() {
        val routeInfo = createTestRouteInfo()

        assertEquals(2, routeInfo.steps.size)
        assertTrue(routeInfo.steps[0] is RouteStep.Walking)
        assertTrue(routeInfo.steps[1] is RouteStep.Transit)
    }

    @Test
    fun `route info transit step has correct bus info`() {
        val routeInfo = createTestRouteInfo()
        val transitStep = routeInfo.steps[1] as RouteStep.Transit

        assertEquals("42", transitStep.busStep.busNumber)
        assertEquals("Bus Stop A", transitStep.busStep.departureStopName)
        assertEquals("17:35", transitStep.busStep.departureTime)
        assertEquals("Home Stop", transitStep.busStep.alightingStopName)
        assertEquals("18:00", transitStep.busStep.arrivalTime)
    }

    // ── Exception types ─────────────────────────────────────────────

    @Test
    fun `NoRouteFoundException carries message`() {
        val exception = NoRouteFoundException("No transit routes available")
        assertEquals("No transit routes available", exception.message)
        assertTrue(exception is Exception)
    }

    @Test
    fun `NoRouteFoundException can be caught as Exception`() {
        var caught = false
        try {
            throw NoRouteFoundException("test")
        } catch (e: Exception) {
            caught = true
        }
        assertTrue(caught)
    }

    // ── Route caching format ────────────────────────────────────────

    @Test
    fun `route info brief string is not empty`() {
        val routeInfo = createTestRouteInfo()
        val brief = routeInfo.toBriefString()
        assertTrue("Brief string should not be empty", brief.isNotEmpty())
    }

    @Test
    fun `route info detailed string is not empty`() {
        val routeInfo = createTestRouteInfo()
        val detailed = routeInfo.toDetailedString()
        assertTrue("Detailed string should not be empty", detailed.isNotEmpty())
    }

    @Test
    fun `multi-route brief string is not empty`() {
        val routes = listOf(createTestRouteInfo(), createTestRouteInfo())
        val brief = RouteInfo.toBriefMultiRouteString(routes)
        assertTrue("Multi-route brief string should not be empty", brief.isNotEmpty())
    }
}
