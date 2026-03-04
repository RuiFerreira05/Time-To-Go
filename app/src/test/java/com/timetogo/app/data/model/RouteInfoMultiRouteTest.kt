package com.timetogo.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RouteInfo.Companion.toBriefMultiRouteString].
 */
class RouteInfoMultiRouteTest {

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun busStep(
        number: String = "755",
        departureStop: String = "Stop A",
        departureTime: String = "08:30",
        arrivalStop: String = "Stop B",
        arrivalTime: String = "08:50",
        isTransfer: Boolean = false
    ) = BusStep(number, departureStop, departureTime, arrivalStop, arrivalTime, isTransfer)

    private fun routeInfo(
        steps: List<RouteStep>,
        totalMinutes: Int = 23,
        arrivalTime: String = "09:00"
    ) = RouteInfo(
        steps = steps,
        totalDurationMinutes = totalMinutes,
        estimatedArrivalTime = arrivalTime,
        originLatLng = "38.76,-9.14",
        destinationLatLng = "38.75,-9.12"
    )

    // ── toBriefMultiRouteString ─────────────────────────────────────────

    @Test
    fun `empty list returns no routes message`() {
        val result = RouteInfo.toBriefMultiRouteString(emptyList())
        assertEquals("No bus routes found", result)
    }

    @Test
    fun `single route delegates to toBriefString`() {
        val route = routeInfo(
            steps = listOf(
                RouteStep.Transit(busStep("755", departureTime = "22:52"))
            ),
            totalMinutes = 23
        )

        val result = RouteInfo.toBriefMultiRouteString(listOf(route))
        assertEquals(route.toBriefString(), result)
    }

    @Test
    fun `two routes shows Next and After labels`() {
        val route1 = routeInfo(
            steps = listOf(RouteStep.Transit(busStep("755", departureTime = "08:00"))),
            totalMinutes = 20
        )
        val route2 = routeInfo(
            steps = listOf(RouteStep.Transit(busStep("758", departureTime = "08:30"))),
            totalMinutes = 25
        )

        val result = RouteInfo.toBriefMultiRouteString(listOf(route1, route2))

        assertTrue("Should contain Next label", result.contains("Next"))
        assertTrue("Should contain After label", result.contains("After"))
        assertTrue("Should contain first bus", result.contains("Bus 755"))
        assertTrue("Should contain second bus", result.contains("Bus 758"))
        assertTrue("Should contain first duration", result.contains("20 min"))
        assertTrue("Should contain second duration", result.contains("25 min"))
    }

    @Test
    fun `walking-only multi route shows walking icon`() {
        val route1 = routeInfo(
            steps = listOf(RouteStep.Walking(10, "Home")),
            totalMinutes = 10
        )
        val route2 = routeInfo(
            steps = listOf(RouteStep.Walking(15, "Home")),
            totalMinutes = 15
        )

        val result = RouteInfo.toBriefMultiRouteString(listOf(route1, route2))

        assertTrue("Should contain walking indicator", result.contains("Walking route"))
    }

    @Test
    fun `mixed bus and walking multi routes`() {
        val busRoute = routeInfo(
            steps = listOf(RouteStep.Transit(busStep("755", departureTime = "08:00"))),
            totalMinutes = 20
        )
        val walkRoute = routeInfo(
            steps = listOf(RouteStep.Walking(30, "Home")),
            totalMinutes = 30
        )

        val result = RouteInfo.toBriefMultiRouteString(listOf(busRoute, walkRoute))

        assertTrue("Should contain bus info", result.contains("Bus 755"))
        assertTrue("Should contain walking info", result.contains("Walking route"))
    }
}
