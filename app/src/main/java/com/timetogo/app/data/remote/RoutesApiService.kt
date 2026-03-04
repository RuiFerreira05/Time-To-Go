package com.timetogo.app.data.remote

import com.timetogo.app.data.remote.dto.RoutesRequest
import com.timetogo.app.data.remote.dto.RoutesResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for the Google Routes API v2.
 *
 * BILLABLE API CALL: Every call to computeRoutes is billed.
 * Only called when the alarm fires or on explicit user retry (max 1 retry).
 */
interface RoutesApiService {

    /**
     * Compute transit routes from origin to destination.
     *
     * The X-Goog-FieldMask header specifies which response fields to include.
     * This is REQUIRED by the Routes API and helps minimize response size.
     */
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Body request: RoutesRequest,
        @Header("X-Goog-FieldMask") fieldMask: String = TRANSIT_FIELD_MASK
    ): RoutesResponse

    companion object {
        /**
         * Field mask requesting transit-relevant fields only.
         * Minimizes response payload and avoids requesting unnecessary data.
         */
        const val TRANSIT_FIELD_MASK =
            "routes.duration," +
            "routes.localizedValues," +
            "routes.legs.steps.travelMode," +
            "routes.legs.steps.staticDuration," +
            "routes.legs.steps.transitDetails.stopDetails," +
            "routes.legs.steps.transitDetails.localizedValues," +
            "routes.legs.steps.transitDetails.headsign," +
            "routes.legs.steps.transitDetails.transitLine," +
            "routes.legs.steps.transitDetails.stopCount," +
            "routes.legs.duration"
    }
}
