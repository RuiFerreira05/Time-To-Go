package com.timetogo.app.data.model

/**
 * Domain model representing a parsed transit route from the Routes API.
 */
data class RouteInfo(
    val steps: List<RouteStep>,
    val totalDurationMinutes: Int,
    val estimatedArrivalTime: String,
    val originLatLng: String,
    val destinationLatLng: String,
    val fetchTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get all bus steps in this route.
     */
    val busSteps: List<BusStep>
        get() = steps.filterIsInstance<RouteStep.Transit>().map { it.busStep }

    /**
     * Format a brief notification string: bus numbers + departure times.
     */
    fun toBriefString(): String {
        val busInfo = busSteps.joinToString(" → ") { step ->
            "Bus ${step.busNumber} at ${step.departureTime}"
        }
        return if (busInfo.isNotEmpty()) {
            "\uD83D\uDE8C $busInfo\n\uD83C\uDFE0 ~$totalDurationMinutes min total"
        } else {
            "\uD83D\uDEB6 Walking route ~$totalDurationMinutes min total"
        }
    }

    /**
     * Format a detailed notification string: step-by-step walking + bus + transfers.
     */
    fun toDetailedString(): String {
        val sb = StringBuilder()
        for (step in steps) {
            when (step) {
                is RouteStep.Walking -> {
                    sb.appendLine("\uD83D\uDEB6 Walk ${step.durationMinutes} min to ${step.destination}")
                }
                is RouteStep.Transit -> {
                    val bus = step.busStep
                    if (bus.isTransfer) {
                        sb.appendLine("\uD83D\uDD04 Transfer: Bus ${bus.busNumber} at ${bus.departureTime} → ${bus.alightingStopName}")
                    } else {
                        sb.appendLine("\uD83D\uDE8C Bus ${bus.busNumber} at ${bus.departureTime} → ${bus.alightingStopName}")
                    }
                }
            }
        }
        sb.appendLine("\uD83C\uDFE0 Arrive home at ~$estimatedArrivalTime (Total: $totalDurationMinutes min)")
        return sb.toString().trim()
    }

    companion object {
        /**
         * Format a brief notification for multiple route alternatives.
         * Shows each option with bus info, departure time, and total duration.
         */
        fun toBriefMultiRouteString(routes: List<RouteInfo>): String {
            if (routes.isEmpty()) return "No bus routes found"
            if (routes.size == 1) return routes[0].toBriefString()

            val sb = StringBuilder()
            routes.forEachIndexed { index, route ->
                val busInfo = route.busSteps.joinToString(" → ") { step ->
                    "Bus ${step.busNumber} at ${step.departureTime}"
                }
                val label = if (index == 0) "Next" else "After"
                if (busInfo.isNotEmpty()) {
                    sb.appendLine("\uD83D\uDE8C $label: $busInfo (~${route.totalDurationMinutes} min)")
                } else {
                    sb.appendLine("\uD83D\uDEB6 $label: Walking route (~${route.totalDurationMinutes} min)")
                }
            }
            return sb.toString().trim()
        }
    }
}

/**
 * Represents a single step in a route — either walking or transit.
 */
sealed class RouteStep {
    data class Walking(
        val durationMinutes: Int,
        val destination: String
    ) : RouteStep()

    data class Transit(
        val busStep: BusStep
    ) : RouteStep()
}

/**
 * Domain model for a single bus/transit leg of a route.
 */
data class BusStep(
    val busNumber: String,
    val departureStopName: String,
    val departureTime: String,
    val alightingStopName: String,
    val arrivalTime: String,
    val isTransfer: Boolean = false
)
