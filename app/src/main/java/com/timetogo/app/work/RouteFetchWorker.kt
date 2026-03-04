package com.timetogo.app.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.timetogo.app.R
import com.timetogo.app.alarm.AlarmScheduler
import com.timetogo.app.data.model.RouteInfo
import com.timetogo.app.data.repository.DirectionsRepository
import com.timetogo.app.data.repository.NoRouteFoundException
import com.timetogo.app.data.repository.UserPreferencesRepository
import com.timetogo.app.notification.NotificationHelper
import com.timetogo.app.util.LocationHelper
import com.timetogo.app.util.PermissionHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that fetches transit route and displays a notification.
 *
 * Triggered by AlarmReceiver when the scheduled alarm fires.
 * Uses expedited mode for timely execution.
 *
 * Flow:
 * 1. Check permissions
 * 2. Get current GPS location
 * 3. Read home address from preferences
 * 4. Call Routes API for transit directions (BILLABLE API CALL)
 * 5. Parse response into RouteInfo
 * 6. Cache result in DataStore
 * 7. Display notification (brief or detailed based on user preference)
 * 8. Reschedule alarm if recurring mode
 */
@HiltWorker
class RouteFetchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val directionsRepository: DirectionsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val locationHelper: LocationHelper,
    private val notificationHelper: NotificationHelper,
    private val alarmScheduler: AlarmScheduler,
    private val moshi: Moshi
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "RouteFetchWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "RouteFetchWorker started")

        val preferences = preferencesRepository.getCurrentPreferences()

        // Validate home address is set
        if (preferences.homeAddress.isEmpty() || (preferences.homeLatitude == 0.0 && preferences.homeLongitude == 0.0)) {
            Log.e(TAG, "Home address not set")
            notificationHelper.showErrorNotification(
                "Please set your home address in the app.",
                0.0, 0.0, 0.0, 0.0
            )
            handlePostAlarmTasks(preferences.isRecurring, preferences.alarmHour, preferences.alarmMinute)
            return Result.failure()
        }

        // Check location permission
        if (!PermissionHelper.hasAnyLocationPermission(applicationContext)) {
            Log.e(TAG, "Location permission not granted")
            notificationHelper.showLocationErrorNotification()
            preferencesRepository.setFetchFailed("Location permission not granted")
            handlePostAlarmTasks(preferences.isRecurring, preferences.alarmHour, preferences.alarmMinute)
            return Result.failure()
        }

        // Get current location
        val location = try {
            locationHelper.getCurrentLocation()
        } catch (e: SecurityException) {
            Log.e(TAG, "Location security exception", e)
            notificationHelper.showLocationErrorNotification()
            preferencesRepository.setFetchFailed("Location permission denied")
            handlePostAlarmTasks(preferences.isRecurring, preferences.alarmHour, preferences.alarmMinute)
            return Result.failure()
        }

        if (location == null) {
            Log.e(TAG, "Could not determine current location")
            notificationHelper.showLocationErrorNotification()
            preferencesRepository.setFetchFailed("Could not determine current location")
            handlePostAlarmTasks(preferences.isRecurring, preferences.alarmHour, preferences.alarmMinute)
            return Result.failure()
        }

        // Fetch transit route — BILLABLE API CALL
        return try {
            Log.d(TAG, "Fetching route from ${location.latitude},${location.longitude} to ${preferences.homeLatitude},${preferences.homeLongitude}")

            if (preferences.isDetailedNotification) {
                // Detailed mode: single route, full step-by-step display
                val routeInfo = directionsRepository.fetchTransitRoute(
                    originLat = location.latitude,
                    originLng = location.longitude,
                    destLat = preferences.homeLatitude,
                    destLng = preferences.homeLongitude
                )

                cacheRoute(routeInfo)
                notificationHelper.showRouteNotification(
                    routeInfo = routeInfo,
                    detailed = true
                )
            } else {
                // Brief mode: fetch up to 2 alternatives showing next buses
                val routes = directionsRepository.fetchTransitRouteAlternatives(
                    originLat = location.latitude,
                    originLng = location.longitude,
                    destLat = preferences.homeLatitude,
                    destLng = preferences.homeLongitude
                )

                cacheRoute(routes.first())
                notificationHelper.showRouteNotification(routes = routes)
            }

            Log.d(TAG, "Route fetch and notification successful")
            handlePostAlarmTasks(preferences.isRecurring, preferences.alarmHour, preferences.alarmMinute)
            Result.success()

        } catch (e: NoRouteFoundException) {
            Log.w(TAG, "No route found", e)
            notificationHelper.showErrorNotification(
                "No bus routes found to your home from your current location.",
                location.latitude, location.longitude,
                preferences.homeLatitude, preferences.homeLongitude
            )
            preferencesRepository.setFetchFailed(e.message ?: "No route found")
            handlePostAlarmTasks(preferences.isRecurring, preferences.alarmHour, preferences.alarmMinute)
            Result.failure()

        } catch (e: Exception) {
            Log.e(TAG, "Route fetch failed", e)
            notificationHelper.showErrorNotification(
                "Couldn't fetch your bus route. Check your internet connection.",
                location.latitude, location.longitude,
                preferences.homeLatitude, preferences.homeLongitude
            )
            preferencesRepository.setFetchFailed(e.message ?: "Unknown error")
            handlePostAlarmTasks(preferences.isRecurring, preferences.alarmHour, preferences.alarmMinute)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Fetching your route...")
            .setContentText("Getting bus directions to home")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        return ForegroundInfo(
            NotificationHelper.NOTIFICATION_ID + 100,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    /**
     * Cache a route for later display in the app.
     */
    private suspend fun cacheRoute(routeInfo: RouteInfo) {
        val routeJson = try {
            val adapter = moshi.adapter(RouteInfo::class.java)
            adapter.toJson(routeInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to serialize route for caching", e)
            ""
        }
        if (routeJson.isNotEmpty()) {
            preferencesRepository.cacheRoute(routeJson)
        }
        preferencesRepository.clearFetchError()
    }

    /**
     * Handle post-alarm tasks:
     * - If recurring: reschedule for tomorrow
     * - If one-shot: disable the alarm
     */
    private suspend fun handlePostAlarmTasks(isRecurring: Boolean, hour: Int, minute: Int) {
        if (isRecurring) {
            alarmScheduler.scheduleNextDayAlarm(hour, minute)
            Log.d(TAG, "Recurring alarm rescheduled for next day")
        } else {
            preferencesRepository.setAlarmEnabled(false)
            Log.d(TAG, "One-shot alarm — disabled after firing")
        }
    }
}
