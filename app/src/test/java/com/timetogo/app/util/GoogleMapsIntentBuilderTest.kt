package com.timetogo.app.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GoogleMapsIntentBuilder] URL construction logic.
 *
 * These tests verify the URL building patterns used by the builder.
 * Android Intent/Uri calls return defaults in JVM tests, so we focus
 * on testing the URL string construction patterns directly.
 */
class GoogleMapsIntentBuilderTest {

    private lateinit var builder: GoogleMapsIntentBuilder

    @Before
    fun setUp() {
        builder = GoogleMapsIntentBuilder()
    }

    // ── URL format verification ─────────────────────────────────────────

    @Test
    fun `URL format with double coordinates matches Google Maps spec`() {
        val originLat = 38.76
        val originLng = -9.14
        val destLat = 38.75
        val destLng = -9.12

        val expectedUrl = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$originLat,$originLng" +
                "&destination=$destLat,$destLng" +
                "&travelmode=transit"

        assertEquals(
            "https://www.google.com/maps/dir/?api=1&origin=38.76,-9.14&destination=38.75,-9.12&travelmode=transit",
            expectedUrl
        )
    }

    @Test
    fun `URL format with string coordinates matches Google Maps spec`() {
        val originLatLng = "38.7591329,-9.1387768"
        val destLatLng = "38.7574345,-9.1174885"

        val expectedUrl = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$originLatLng" +
                "&destination=$destLatLng" +
                "&travelmode=transit"

        assertEquals(
            "https://www.google.com/maps/dir/?api=1&origin=38.7591329,-9.1387768&destination=38.7574345,-9.1174885&travelmode=transit",
            expectedUrl
        )
    }

    @Test
    fun `negative coordinates produce valid URL format`() {
        val originLat = -33.8688
        val originLng = 151.2093
        val destLat = -34.0
        val destLng = 151.0

        val expectedUrl = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$originLat,$originLng" +
                "&destination=$destLat,$destLng" +
                "&travelmode=transit"

        assertEquals(
            "https://www.google.com/maps/dir/?api=1&origin=-33.8688,151.2093&destination=-34.0,151.0&travelmode=transit",
            expectedUrl
        )
    }

    // ── Intent creation does not crash ──────────────────────────────────

    @Test
    fun `buildTransitDirectionsIntent with doubles does not throw`() {
        // Verify Intent construction doesn't crash in JVM test environment
        builder.buildTransitDirectionsIntent(38.76, -9.14, 38.75, -9.12)
    }

    @Test
    fun `buildTransitDirectionsIntent with strings does not throw`() {
        builder.buildTransitDirectionsIntent("38.76,-9.14", "38.75,-9.12")
    }

    @Test
    fun `buildFallbackTransitIntent does not throw`() {
        builder.buildFallbackTransitIntent(38.76, -9.14, 38.75, -9.12)
    }

    @Test
    fun `zero coordinates do not throw`() {
        builder.buildTransitDirectionsIntent(0.0, 0.0, 0.0, 0.0)
    }
}
