package com.timetogo.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Routes API computeRoutes response.
 * Response schema verified against Google Routes API v2 documentation.
 * Field mask must include: routes.legs.steps.transitDetails,routes.legs.steps.staticDuration,
 * routes.legs.steps.travelMode,routes.duration,routes.legs.localizedValues
 */
@JsonClass(generateAdapter = true)
data class RoutesResponse(
    @Json(name = "routes") val routes: List<Route>? = null
)

@JsonClass(generateAdapter = true)
data class Route(
    @Json(name = "legs") val legs: List<Leg>? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "localizedValues") val localizedValues: LocalizedValues? = null
)

@JsonClass(generateAdapter = true)
data class LocalizedValues(
    @Json(name = "duration") val duration: LocalizedText? = null,
    @Json(name = "distance") val distance: LocalizedText? = null,
    @Json(name = "transitFare") val transitFare: LocalizedText? = null
)

@JsonClass(generateAdapter = true)
data class LocalizedText(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Leg(
    @Json(name = "steps") val steps: List<Step>? = null,
    @Json(name = "duration") val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class Step(
    @Json(name = "travelMode") val travelMode: String? = null,
    @Json(name = "staticDuration") val staticDuration: String? = null,
    @Json(name = "transitDetails") val transitDetails: TransitDetails? = null
)

@JsonClass(generateAdapter = true)
data class TransitDetails(
    @Json(name = "stopDetails") val stopDetails: StopDetails? = null,
    @Json(name = "localizedValues") val localizedValues: TransitLocalizedValues? = null,
    @Json(name = "headsign") val headsign: String? = null,
    @Json(name = "transitLine") val transitLine: TransitLine? = null,
    @Json(name = "stopCount") val stopCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class StopDetails(
    @Json(name = "departureStop") val departureStop: TransitStop? = null,
    @Json(name = "arrivalStop") val arrivalStop: TransitStop? = null,
    @Json(name = "departureTime") val departureTime: String? = null,
    @Json(name = "arrivalTime") val arrivalTime: String? = null
)

@JsonClass(generateAdapter = true)
data class TransitStop(
    @Json(name = "name") val name: String? = null,
    @Json(name = "location") val location: LocationPoint? = null
)

@JsonClass(generateAdapter = true)
data class TransitLocalizedValues(
    @Json(name = "departureTime") val departureTime: LocalizedText? = null,
    @Json(name = "arrivalTime") val arrivalTime: LocalizedText? = null
)

@JsonClass(generateAdapter = true)
data class TransitLine(
    @Json(name = "name") val name: String? = null,
    @Json(name = "nameShort") val nameShort: String? = null,
    @Json(name = "vehicle") val vehicle: TransitVehicle? = null,
    @Json(name = "agencies") val agencies: List<TransitAgency>? = null
)

@JsonClass(generateAdapter = true)
data class TransitVehicle(
    @Json(name = "name") val name: LocalizedText? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class TransitAgency(
    @Json(name = "name") val name: String? = null,
    @Json(name = "uri") val uri: String? = null
)
