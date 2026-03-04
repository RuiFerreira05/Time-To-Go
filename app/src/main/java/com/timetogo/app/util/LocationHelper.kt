package com.timetogo.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Wrapper around FusedLocationProviderClient for obtaining the device's current location.
 * Designed to work reliably both in foreground and from background (e.g. WorkManager).
 */
class LocationHelper @Inject constructor(
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationHelper"
        private const val LOCATION_TIMEOUT_MS = 30_000L       // 30s total timeout
        private const val STALE_THRESHOLD_MS = 10 * 60 * 1000L // 10 minutes
    }

    /**
     * Get the current location with a multi-step fallback strategy:
     * 1. Try last known location (if fresh enough)
     * 2. Try getCurrentLocation() API (best for one-shot background requests)
     * 3. Fall back to requestLocationUpdates with balanced accuracy
     *
     * @return Location or null if unavailable
     * @throws SecurityException if location permission is not granted
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        // Step 1: Try the last known location
        val lastLocation = getLastKnownLocation()
        if (lastLocation != null && !isLocationStale(lastLocation)) {
            Log.d(TAG, "Using last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
            return lastLocation
        }

        // Step 2: Try the getCurrentLocation API (one-shot, best for background)
        Log.d(TAG, "Last known location stale or unavailable, trying getCurrentLocation API...")
        val currentLocation = requestCurrentLocation()
        if (currentLocation != null) {
            Log.d(TAG, "getCurrentLocation succeeded: ${currentLocation.latitude}, ${currentLocation.longitude}")
            return currentLocation
        }

        // Step 3: Fall back to requestLocationUpdates with balanced accuracy
        Log.d(TAG, "getCurrentLocation returned null, falling back to location updates...")
        return requestFreshLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to get last known location", e)
                    continuation.resume(null)
                }
        }
    }

    /**
     * Uses the getCurrentLocation() API which is optimized for one-shot location
     * requests, especially from background processes. It will actively power up
     * location hardware if needed.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(): Location? {
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener { e ->
                    Log.w(TAG, "getCurrentLocation failed", e)
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        }
    }

    /**
     * Fallback: request location updates with balanced power accuracy.
     * More likely to succeed in background since it can use Wi-Fi/cell.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? {
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    1000L
                )
                    .setMaxUpdates(1)
                    .setMaxUpdateDelayMillis(LOCATION_TIMEOUT_MS)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        val location = result.lastLocation
                        Log.d(TAG, "Fresh location received: ${location?.latitude}, ${location?.longitude}")
                        continuation.resume(location)
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )

                continuation.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(callback)
                }
            }
        }
    }

    private fun isLocationStale(location: Location): Boolean {
        val age = System.currentTimeMillis() - location.time
        return age > STALE_THRESHOLD_MS
    }
}
