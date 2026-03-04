package com.timetogo.app.data.repository

import com.timetogo.app.data.model.RouteStep
import com.timetogo.app.data.remote.RoutesApiService
import com.timetogo.app.data.remote.dto.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [DirectionsRepository] route parsing logic.
 * Uses a mocked [RoutesApiService] to provide controlled API responses.
 */
class DirectionsRepositoryTest {

    private lateinit var mockApiService: RoutesApiService
    private lateinit var repository: DirectionsRepository

    @Before
    fun setUp() {
        mockApiService = mock()
        repository = DirectionsRepository(mockApiService)
    }

    // ── Helper to build a RoutesResponse ────────────────────────────────

    private fun transitStep(
        busNumber: String = "755",
        departureStop: String = "Stop A",
        arrivalStop: String = "Stop B",
        departureTime: String = "2026-03-03T22:52:40Z",
        arrivalTime: String = "2026-03-03T23:04:18Z",
        stopCount: Int = 10,
        duration: String = "600s"
    ) = Step(
        travelMode = "TRANSIT",
        staticDuration = duration,
        transitDetails = TransitDetails(
            stopDetails = StopDetails(
                departureStop = TransitStop(name = departureStop),
                arrivalStop = TransitStop(name = arrivalStop),
                departureTime = departureTime,
                arrivalTime = arrivalTime
            ),
            transitLine = TransitLine(nameShort = busNumber),
            stopCount = stopCount
        )
    )

    private fun walkStep(durationSeconds: Int) = Step(
        travelMode = "WALK",
        staticDuration = "${durationSeconds}s",
        transitDetails = null
    )

    private fun response(
        steps: List<Step>,
        routeDuration: String = "1200s",
        legDuration: String? = null
    ): RoutesResponse {
        return RoutesResponse(
            routes = listOf(
                Route(
                    legs = listOf(Leg(steps = steps, duration = legDuration)),
                    duration = routeDuration
                )
            )
        )
    }

    // ── Tests ───────────────────────────────────────────────────────────

    @Test
    fun `single bus with walking merges walk steps correctly`() = runTest {
        // Simulates: walk 13s + walk 91s + walk 283s → bus → walk 79s
        val apiResponse = response(
            steps = listOf(
                walkStep(13),
                walkStep(91),
                walkStep(283),
                transitStep("755", departureStop = "Lg. Frei Heitor Pinto"),
                walkStep(79)
            ),
            routeDuration = "1378s"
        )

        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        val result = repository.fetchTransitRoute(38.76, -9.14, 38.75, -9.12)

        // 13 + 91 + 283 = 387s → 6 min walking before bus
        // 79s → 1 min walking after bus
        val walkingSteps = result.steps.filterIsInstance<RouteStep.Walking>()
        val transitSteps = result.steps.filterIsInstance<RouteStep.Transit>()

        assertEquals("Should have exactly 2 walking steps", 2, walkingSteps.size)
        assertEquals("Should have exactly 1 transit step", 1, transitSteps.size)

        // First walk: accumulated 387s → 6 min
        assertEquals(6, walkingSteps[0].durationMinutes)
        assertEquals("Lg. Frei Heitor Pinto", walkingSteps[0].destination)

        // Transit step
        assertEquals("755", transitSteps[0].busStep.busNumber)
        assertFalse(transitSteps[0].busStep.isTransfer)

        // Final walk: 79s → 1 min
        assertEquals(1, walkingSteps[1].durationMinutes)
        assertEquals("destination", walkingSteps[1].destination)
    }

    @Test
    fun `very short walk steps are accumulated but skipped if total under 60s`() = runTest {
        val apiResponse = response(
            steps = listOf(
                walkStep(10),
                walkStep(20),
                walkStep(15),
                transitStep("755", departureStop = "Stop A")
            ),
            routeDuration = "600s"
        )

        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        val result = repository.fetchTransitRoute(38.76, -9.14, 38.75, -9.12)

        // 10 + 20 + 15 = 45s → 0 min → should be skipped
        val walkingSteps = result.steps.filterIsInstance<RouteStep.Walking>()
        assertEquals("Walk < 1 min should be omitted", 0, walkingSteps.size)
    }

    @Test
    fun `transfer routes set isTransfer correctly`() = runTest {
        val apiResponse = response(
            steps = listOf(
                walkStep(120),
                transitStep("755", departureStop = "A", arrivalStop = "B"),
                walkStep(60),
                transitStep("758", departureStop = "C", arrivalStop = "D")
            ),
            routeDuration = "2400s"
        )

        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        val result = repository.fetchTransitRoute(38.76, -9.14, 38.75, -9.12)

        val transitSteps = result.steps.filterIsInstance<RouteStep.Transit>()
        assertEquals(2, transitSteps.size)
        assertFalse("First bus should not be marked as transfer", transitSteps[0].busStep.isTransfer)
        assertTrue("Second bus should be marked as transfer", transitSteps[1].busStep.isTransfer)
    }

    @Test(expected = NoRouteFoundException::class)
    fun `empty routes list throws NoRouteFoundException`() = runTest {
        val apiResponse = RoutesResponse(routes = emptyList())
        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        repository.fetchTransitRoute(38.76, -9.14, 38.75, -9.12)
    }

    @Test(expected = NoRouteFoundException::class)
    fun `null routes throws NoRouteFoundException`() = runTest {
        val apiResponse = RoutesResponse(routes = null)
        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        repository.fetchTransitRoute(38.76, -9.14, 38.75, -9.12)
    }

    @Test
    fun `duration parsing converts seconds to minutes`() = runTest {
        val apiResponse = response(
            steps = listOf(transitStep("755")),
            routeDuration = "1380s" // 23 minutes
        )

        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        val result = repository.fetchTransitRoute(38.76, -9.14, 38.75, -9.12)

        assertEquals(23, result.totalDurationMinutes)
    }

    @Test
    fun `walking only route has no transit steps`() = runTest {
        val apiResponse = response(
            steps = listOf(
                walkStep(120),
                walkStep(180),
                walkStep(300)
            ),
            routeDuration = "600s"
        )

        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        val result = repository.fetchTransitRoute(38.76, -9.14, 38.75, -9.12)

        val transitSteps = result.steps.filterIsInstance<RouteStep.Transit>()
        val walkingSteps = result.steps.filterIsInstance<RouteStep.Walking>()

        assertTrue("Should have no transit steps", transitSteps.isEmpty())
        assertEquals("Should have 1 merged walking step", 1, walkingSteps.size)
        // 120 + 180 + 300 = 600s → 10 min
        assertEquals(10, walkingSteps[0].durationMinutes)
    }

    @Test
    fun `origin and destination coordinates are stored correctly`() = runTest {
        val apiResponse = response(
            steps = listOf(transitStep("755")),
            routeDuration = "600s"
        )

        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        val result = repository.fetchTransitRoute(38.7591329, -9.1387768, 38.7574345, -9.1174885)

        assertEquals("38.7591329,-9.1387768", result.originLatLng)
        assertEquals("38.7574345,-9.1174885", result.destinationLatLng)
    }

    @Test
    fun `transit line falls back to long name when nameShort is null`() = runTest {
        val step = Step(
            travelMode = "TRANSIT",
            staticDuration = "600s",
            transitDetails = TransitDetails(
                stopDetails = StopDetails(
                    departureStop = TransitStop(name = "A"),
                    arrivalStop = TransitStop(name = "B"),
                    departureTime = "2026-03-03T22:00:00Z",
                    arrivalTime = "2026-03-03T22:10:00Z"
                ),
                transitLine = TransitLine(name = "Long Route Name", nameShort = null),
                stopCount = 5
            )
        )

        val apiResponse = response(steps = listOf(step), routeDuration = "600s")
        whenever(mockApiService.computeRoutes(any(), any())).thenReturn(apiResponse)
        val result = repository.fetchTransitRoute(38.76, -9.14, 38.75, -9.12)

        val transitSteps = result.steps.filterIsInstance<RouteStep.Transit>()
        assertEquals("Long Route Name", transitSteps[0].busStep.busNumber)
    }
}
