package com.timetogo.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RouteInfo] formatting methods (toBriefString, toDetailedString)
 * and the busSteps property.
 */
class RouteInfoTest {

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

    // ── busSteps property ───────────────────────────────────────────────

    @Test
    fun `busSteps filters only transit steps`() {
        val route = routeInfo(
            steps = listOf(
                RouteStep.Walking(5, "Stop A"),
                RouteStep.Transit(busStep("755")),
                RouteStep.Walking(2, "destination"),
                RouteStep.Transit(busStep("758", isTransfer = true))
            )
        )
        assertEquals(2, route.busSteps.size)
        assertEquals("755", route.busSteps[0].busNumber)
        assertEquals("758", route.busSteps[1].busNumber)
    }

    @Test
    fun `busSteps returns empty for walking-only route`() {
        val route = routeInfo(steps = listOf(RouteStep.Walking(15, "Home")))
        assertTrue(route.busSteps.isEmpty())
    }

    // ── toBriefString ───────────────────────────────────────────────────

    @Test
    fun `toBriefString with bus shows bus number and time`() {
        val route = routeInfo(
            steps = listOf(
                RouteStep.Walking(5, "Stop A"),
                RouteStep.Transit(busStep("755", departureTime = "22:52"))
            ),
            totalMinutes = 23
        )
        val brief = route.toBriefString()

        assertTrue("Should contain bus number", brief.contains("Bus 755"))
        assertTrue("Should contain departure time", brief.contains("22:52"))
        assertTrue("Should contain total duration", brief.contains("23 min"))
    }

    @Test
    fun `toBriefString with multiple buses shows arrow separator`() {
        val route = routeInfo(
            steps = listOf(
                RouteStep.Transit(busStep("755", departureTime = "08:00")),
                RouteStep.Transit(busStep("758", departureTime = "08:20", isTransfer = true))
            ),
            totalMinutes = 40
        )
        val brief = route.toBriefString()

        assertTrue("Should contain arrow separator", brief.contains("→"))
        assertTrue("Should contain first bus", brief.contains("Bus 755"))
        assertTrue("Should contain second bus", brief.contains("Bus 758"))
    }

    @Test
    fun `toBriefString walking-only route shows walking message`() {
        val route = routeInfo(
            steps = listOf(RouteStep.Walking(15, "Home")),
            totalMinutes = 15
        )
        val brief = route.toBriefString()

        assertTrue("Should contain walking icon/text", brief.contains("Walking route"))
        assertTrue("Should contain duration", brief.contains("15 min"))
    }

    // ── toDetailedString ────────────────────────────────────────────────

    @Test
    fun `toDetailedString includes walk, bus, and arrival lines`() {
        val route = routeInfo(
            steps = listOf(
                RouteStep.Walking(5, "Largo do Rato"),
                RouteStep.Transit(busStep("755", departureTime = "22:52", arrivalStop = "ISEL")),
                RouteStep.Walking(1, "destination")
            ),
            totalMinutes = 23,
            arrivalTime = "23:05"
        )
        val detailed = route.toDetailedString()

        assertTrue("Should mention walking", detailed.contains("Walk 5 min to Largo do Rato"))
        assertTrue("Should mention bus", detailed.contains("Bus 755 at 22:52"))
        assertTrue("Should mention arrival stop", detailed.contains("ISEL"))
        assertTrue("Should mention arrival time", detailed.contains("23:05"))
        assertTrue("Should mention total", detailed.contains("23 min"))
    }

    @Test
    fun `toDetailedString marks transfers`() {
        val route = routeInfo(
            steps = listOf(
                RouteStep.Transit(busStep("755")),
                RouteStep.Transit(busStep("758", isTransfer = true))
            )
        )
        val detailed = route.toDetailedString()

        assertTrue("First bus should not say Transfer", !detailed.lines()[0].contains("Transfer"))
        assertTrue("Second bus should say Transfer", detailed.contains("Transfer"))
    }

    @Test
    fun `toDetailedString with no steps only shows arrival`() {
        val route = routeInfo(steps = emptyList(), totalMinutes = 0, arrivalTime = "12:00")
        val detailed = route.toDetailedString()

        assertTrue("Should show arrival", detailed.contains("Arrive home"))
    }
}
