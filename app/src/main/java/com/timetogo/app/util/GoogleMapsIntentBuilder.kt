package com.timetogo.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

/**
 * Builds Intents to open Google Maps with transit directions pre-loaded.
 *
 * Deep-link format (verified against Google Maps URLs documentation):
 * https://www.google.com/maps/dir/?api=1&origin=LAT,LNG&destination=LAT,LNG&travelmode=transit
 */
class GoogleMapsIntentBuilder @Inject constructor() {

    /**
     * Build an Intent that opens Google Maps with transit directions
     * from the origin to the destination.
     *
     * @param originLat Origin latitude
     * @param originLng Origin longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @return Intent to launch Google Maps
     */
    fun buildTransitDirectionsIntent(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Intent {
        val url = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$originLat,$originLng" +
                "&destination=$destLat,$destLng" +
                "&travelmode=transit"

        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Build an Intent that opens Google Maps with transit directions
     * from the origin coordinates string to the destination coordinates string.
     *
     * @param originLatLng Comma-separated "lat,lng" string
     * @param destLatLng Comma-separated "lat,lng" string
     * @return Intent to launch Google Maps
     */
    fun buildTransitDirectionsIntent(originLatLng: String, destLatLng: String): Intent {
        val url = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$originLatLng" +
                "&destination=$destLatLng" +
                "&travelmode=transit"

        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Build a fallback Intent (without Google Maps package restriction)
     * in case Google Maps is not installed.
     */
    fun buildFallbackTransitIntent(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Intent {
        val url = "https://www.google.com/maps/dir/?api=1" +
                "&origin=$originLat,$originLng" +
                "&destination=$destLat,$destLng" +
                "&travelmode=transit"

        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
