package com.timetogo.app.data.repository

import com.timetogo.app.data.model.BusStep
import com.timetogo.app.data.model.RouteInfo
import com.timetogo.app.data.model.RouteStep
import com.timetogo.app.data.remote.RoutesApiService
import com.timetogo.app.data.remote.dto.LatLng
import com.timetogo.app.data.remote.dto.LocationPoint
import com.timetogo.app.data.remote.dto.RoutesRequest
import com.timetogo.app.data.remote.dto.RoutesResponse
import com.timetogo.app.data.remote.dto.Step
import com.timetogo.app.data.remote.dto.Waypoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Repository that handles fetching and parsing transit directions from the Routes API.
 *
 * BILLABLE API CALL: The computeRoutes method triggers a billable Google Maps Platform API call.
 * This repository should only be invoked from RouteFetchWorker (alarm-triggered) or retry actions.
 */
open class DirectionsRepository @Inject constructor(
    private val routesApiService: RoutesApiService
) {
    /**
     * Fetch transit route from current location to home.
     *
     * @param originLat Current location latitude
     * @param originLng Current location longitude
     * @param destLat Home address latitude
     * @param destLng Home address longitude
     * @return Parsed RouteInfo or throws an exception on failure
     */
    suspend fun fetchTransitRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): RouteInfo {
        val request = RoutesRequest(
            origin = Waypoint(
                location = LocationPoint(LatLng(originLat, originLng))
            ),
            destination = Waypoint(
                location = LocationPoint(LatLng(destLat, destLng))
            ),
            departureTime = Instant.now().toString()
        )

        // BILLABLE API CALL: This is the single API call per alarm trigger.
        val response = routesApiService.computeRoutes(request)
        return parseResponse(response, originLat, originLng, destLat, destLng)
    }

    /**
     * Fetch up to 2 alternative transit routes (for brief notification mode).
     * Uses the same single billable API call with computeAlternativeRoutes enabled.
     *
     * @return List of up to 2 RouteInfo objects, sorted by departure time
     */
    suspend fun fetchTransitRouteAlternatives(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): List<RouteInfo> {
        val request = RoutesRequest(
            origin = Waypoint(
                location = LocationPoint(LatLng(originLat, originLng))
            ),
            destination = Waypoint(
                location = LocationPoint(LatLng(destLat, destLng))
            ),
            departureTime = Instant.now().toString(),
            computeAlternativeRoutes = true
        )

        // BILLABLE API CALL: Same single call, returns multiple route alternatives.
        val response = routesApiService.computeRoutes(request)

        val routes = response.routes
            ?: throw NoRouteFoundException("No transit routes found to your home from your current location.")

        if (routes.isEmpty()) {
            throw NoRouteFoundException("No transit routes found to your home from your current location.")
        }

        return routes.take(3).map { route ->
            parseResponse(
                RoutesResponse(routes = listOf(route)),
                originLat, originLng, destLat, destLng
            )
        }
    }

    private fun parseResponse(
        response: RoutesResponse,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): RouteInfo {
        val route = response.routes?.firstOrNull()
            ?: throw NoRouteFoundException("No transit routes found to your home from your current location.")

        val steps = mutableListOf<RouteStep>()
        var transitStepIndex = 0

        route.legs?.forEach { leg ->
            var accumulatedWalkingSeconds = 0

            leg.steps?.forEach { step ->
                when (step.travelMode) {
                    "WALK" -> {
                        val durationSeconds = step.staticDuration?.removeSuffix("s")?.toIntOrNull() ?: 0
                        accumulatedWalkingSeconds += durationSeconds
                    }
                    "TRANSIT" -> {
                        // If we had accumulated walking time before this transit step, emit a Walk step
                        val walkingMinutes = accumulatedWalkingSeconds / 60
                        if (walkingMinutes > 0) {
                            // Extract stop name from the upcoming transit step
                            val nextStopName = step.transitDetails?.stopDetails?.departureStop?.name ?: "next stop"
                            steps.add(RouteStep.Walking(walkingMinutes, nextStopName))
                        }
                        accumulatedWalkingSeconds = 0 // Reset

                        // Parse the transit step
                        val transitDetails = step.transitDetails
                        if (transitDetails != null) {
                            val stopDetails = transitDetails.stopDetails

                            val busNumber = transitDetails.transitLine?.nameShort
                                ?: transitDetails.transitLine?.name
                                ?: "Unknown"

                            val departureStopName = stopDetails?.departureStop?.name ?: "Unknown stop"
                            val alightingStopName = stopDetails?.arrivalStop?.name ?: "Unknown stop"

                            val departureTime = formatTimeFromTimestamp(stopDetails?.departureTime)
                            val arrivalTime = formatTimeFromTimestamp(stopDetails?.arrivalTime)

                            steps.add(
                                RouteStep.Transit(
                                    BusStep(
                                        busNumber = busNumber,
                                        departureStopName = departureStopName,
                                        departureTime = departureTime,
                                        alightingStopName = alightingStopName,
                                        arrivalTime = arrivalTime,
                                        isTransfer = transitStepIndex > 0
                                    )
                                )
                            )
                            transitStepIndex++
                        }
                    }
                }
            }

            // If there's leftover walking time at the end of the leg (e.g. walk from final bus stop to home)
            val finalWalkingMinutes = accumulatedWalkingSeconds / 60
            if (finalWalkingMinutes > 0) {
                steps.add(RouteStep.Walking(finalWalkingMinutes, "destination"))
            }
        }

        // Parse total duration from the route's duration string (format: "XXXs")
        val totalDurationSeconds = route.duration?.removeSuffix("s")?.toIntOrNull() ?: 0
        val totalDurationMinutes = totalDurationSeconds / 60

        // Calculate estimated arrival time
        val arrivalInstant = Instant.now().plusSeconds(totalDurationSeconds.toLong())
        val arrivalTime = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(arrivalInstant)

        return RouteInfo(
            steps = steps,
            totalDurationMinutes = totalDurationMinutes,
            estimatedArrivalTime = arrivalTime,
            originLatLng = "$originLat,$originLng",
            destinationLatLng = "$destLat,$destLng"
        )
    }

    private fun formatTimeFromTimestamp(timestamp: String?): String {
        if (timestamp.isNullOrEmpty()) return "??:??"
        return try {
            val instant = Instant.parse(timestamp)
            DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            timestamp.takeLast(5)
        }
    }
}

class NoRouteFoundException(message: String) : Exception(message)
