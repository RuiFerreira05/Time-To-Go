package com.timetogo.app.data.remote.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Moshi serialization of [RoutesRequest] and related DTOs.
 * Ensures that the request body sent to the Google Routes API is well-formed.
 */
class RoutesRequestSerializationTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    // ── RoutesRequest ───────────────────────────────────────────────────

    @Test
    fun `RoutesRequest serializes with correct structure`() {
        val request = RoutesRequest(
            origin = Waypoint(location = LocationPoint(latLng = LatLng(38.76, -9.14))),
            destination = Waypoint(location = LocationPoint(latLng = LatLng(38.75, -9.12)))
        )

        val adapter = moshi.adapter(RoutesRequest::class.java)
        val json = adapter.toJson(request)

        assertNotNull(json)
        assertTrue("Should contain origin", json.contains("\"origin\""))
        assertTrue("Should contain destination", json.contains("\"destination\""))
        assertTrue("Should contain travelMode", json.contains("\"travelMode\""))
        assertTrue("Should contain TRANSIT", json.contains("TRANSIT"))
    }

    @Test
    fun `RoutesRequest default travelMode is TRANSIT`() {
        val request = RoutesRequest(
            origin = Waypoint(location = LocationPoint(latLng = LatLng(0.0, 0.0))),
            destination = Waypoint(location = LocationPoint(latLng = LatLng(0.0, 0.0)))
        )

        assertEquals("TRANSIT", request.travelMode)
    }

    @Test
    fun `RoutesRequest default computeAlternativeRoutes is false`() {
        val request = RoutesRequest(
            origin = Waypoint(location = LocationPoint(latLng = LatLng(0.0, 0.0))),
            destination = Waypoint(location = LocationPoint(latLng = LatLng(0.0, 0.0)))
        )

        assertEquals(false, request.computeAlternativeRoutes)
    }

    @Test
    fun `RoutesRequest default departureTime is null`() {
        val request = RoutesRequest(
            origin = Waypoint(location = LocationPoint(latLng = LatLng(0.0, 0.0))),
            destination = Waypoint(location = LocationPoint(latLng = LatLng(0.0, 0.0)))
        )

        assertEquals(null, request.departureTime)
    }

    // ── TransitPreferences ──────────────────────────────────────────────

    @Test
    fun `TransitPreferences defaults to BUS mode`() {
        val prefs = TransitPreferences()

        assertEquals(listOf("BUS"), prefs.allowedTravelModes)
    }

    @Test
    fun `TransitPreferences defaults to FEWER_TRANSFERS`() {
        val prefs = TransitPreferences()

        assertEquals("FEWER_TRANSFERS", prefs.routingPreference)
    }

    @Test
    fun `TransitPreferences serializes correctly`() {
        val prefs = TransitPreferences()
        val adapter = moshi.adapter(TransitPreferences::class.java)
        val json = adapter.toJson(prefs)

        assertTrue("Should contain allowedTravelModes", json.contains("\"allowedTravelModes\""))
        assertTrue("Should contain BUS", json.contains("BUS"))
        assertTrue("Should contain routingPreference", json.contains("\"routingPreference\""))
        assertTrue("Should contain FEWER_TRANSFERS", json.contains("FEWER_TRANSFERS"))
    }

    // ── LatLng ──────────────────────────────────────────────────────────

    @Test
    fun `LatLng serializes with correct coordinates`() {
        val latLng = LatLng(38.7591329, -9.1387768)
        val adapter = moshi.adapter(LatLng::class.java)
        val json = adapter.toJson(latLng)

        assertTrue("Should contain latitude", json.contains("38.7591329"))
        assertTrue("Should contain longitude", json.contains("-9.1387768"))
    }

    @Test
    fun `LatLng round-trip serialization preserves values`() {
        val original = LatLng(38.7591329, -9.1387768)
        val adapter = moshi.adapter(LatLng::class.java)
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(original.latitude, deserialized!!.latitude, 0.0000001)
        assertEquals(original.longitude, deserialized.longitude, 0.0000001)
    }

    // ── Full request round-trip ─────────────────────────────────────────

    @Test
    fun `full RoutesRequest round-trip serialization`() {
        val original = RoutesRequest(
            origin = Waypoint(location = LocationPoint(latLng = LatLng(38.76, -9.14))),
            destination = Waypoint(location = LocationPoint(latLng = LatLng(38.75, -9.12))),
            travelMode = "TRANSIT",
            computeAlternativeRoutes = true,
            departureTime = "2026-03-03T22:00:00Z"
        )

        val adapter = moshi.adapter(RoutesRequest::class.java)
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(original.travelMode, deserialized!!.travelMode)
        assertEquals(original.computeAlternativeRoutes, deserialized.computeAlternativeRoutes)
        assertEquals(original.departureTime, deserialized.departureTime)
        assertEquals(
            original.origin.location!!.latLng.latitude,
            deserialized.origin.location!!.latLng.latitude,
            0.001
        )
    }
}
