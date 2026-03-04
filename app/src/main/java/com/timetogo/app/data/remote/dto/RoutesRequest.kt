package com.timetogo.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Routes API computeRoutes request body.
 * Endpoint: POST https://routes.googleapis.com/directions/v2:computeRoutes
 *
 * BILLABLE API CALL: Each call to computeRoutes is a billable Google Maps Platform API call.
 * This should ONLY be called when the alarm fires (or on explicit user retry).
 */
@JsonClass(generateAdapter = true)
data class RoutesRequest(
    @Json(name = "origin") val origin: Waypoint,
    @Json(name = "destination") val destination: Waypoint,
    @Json(name = "travelMode") val travelMode: String = "TRANSIT",
    @Json(name = "transitPreferences") val transitPreferences: TransitPreferences? = TransitPreferences(),
    @Json(name = "computeAlternativeRoutes") val computeAlternativeRoutes: Boolean = false,
    @Json(name = "departureTime") val departureTime: String? = null
)

@JsonClass(generateAdapter = true)
data class Waypoint(
    @Json(name = "location") val location: LocationPoint? = null
)

@JsonClass(generateAdapter = true)
data class LocationPoint(
    @Json(name = "latLng") val latLng: LatLng
)

@JsonClass(generateAdapter = true)
data class LatLng(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double
)

@JsonClass(generateAdapter = true)
data class TransitPreferences(
    @Json(name = "allowedTravelModes") val allowedTravelModes: List<String> = listOf("BUS"),
    @Json(name = "routingPreference") val routingPreference: String = "FEWER_TRANSFERS"
)
