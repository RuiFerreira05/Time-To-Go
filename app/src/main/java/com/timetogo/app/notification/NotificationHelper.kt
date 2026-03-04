package com.timetogo.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.timetogo.app.MainActivity
import com.timetogo.app.R
import com.timetogo.app.data.model.RouteInfo
import com.timetogo.app.util.GoogleMapsIntentBuilder
import javax.inject.Inject

/**
 * Helper for creating and displaying notifications.
 * Handles both success (brief/detailed route) and failure notifications.
 */
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "time_to_go_route"
        const val CHANNEL_NAME = "Route Notifications"
        const val CHANNEL_DESCRIPTION = "Daily bus route notifications"
        const val NOTIFICATION_ID = 1001
        const val RETRY_NOTIFICATION_ID = 1002

        const val ACTION_RETRY = "com.timetogo.app.ACTION_RETRY"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val mapsIntentBuilder = GoogleMapsIntentBuilder()

    /**
     * Create the notification channel. Must be called on app startup.
     */
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Show a success notification with route information.
     *
     * @param routeInfo Parsed route data
     * @param detailed If true, show BigTextStyle with full step-by-step route
     */
    fun showRouteNotification(routeInfo: RouteInfo, detailed: Boolean) {
        val contentText = if (detailed) {
            routeInfo.toDetailedString()
        } else {
            routeInfo.toBriefString()
        }

        // Tap action: open Google Maps with transit directions
        val mapIntent = mapsIntentBuilder.buildTransitDirectionsIntent(
            routeInfo.originLatLng,
            routeInfo.destinationLatLng
        )
        val mapPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to go home! \uD83D\uDE8C")
            .setContentText(if (detailed) "Tap to view route in Google Maps" else contentText)
            .setContentIntent(mapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (detailed) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
                    .setBigContentTitle("Time to go home! \uD83D\uDE8C")
            )
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * Show a brief notification with multiple route alternatives.
     *
     * @param routes List of up to 2 parsed route alternatives
     */
    fun showRouteNotification(routes: List<RouteInfo>) {
        val contentText = RouteInfo.toBriefMultiRouteString(routes)
        val firstRoute = routes.firstOrNull() ?: return

        // Tap action: open Google Maps with transit directions
        val mapIntent = mapsIntentBuilder.buildTransitDirectionsIntent(
            firstRoute.originLatLng,
            firstRoute.destinationLatLng
        )
        val mapPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to go home! \uD83D\uDE8C")
            .setContentText("Tap to view route in Google Maps")
            .setContentIntent(mapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
                    .setBigContentTitle("Time to go home! \uD83D\uDE8C")
            )

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * Show an error notification when route fetch fails.
     * Includes a "Retry" action button.
     */
    fun showErrorNotification(errorMessage: String, originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
        // Tap action: open Google Maps anyway
        val mapIntent = mapsIntentBuilder.buildTransitDirectionsIntent(
            originLat, originLng, destLat, destLng
        )
        val mapPendingIntent = PendingIntent.getActivity(
            context,
            1,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Retry action
        val retryIntent = Intent(context, com.timetogo.app.alarm.AlarmReceiver::class.java).apply {
            action = ACTION_RETRY
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to go — Route unavailable")
            .setContentText(errorMessage)
            .setContentIntent(mapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .addAction(
                R.drawable.ic_notification,
                "Retry",
                retryPendingIntent
            )

        notificationManager.notify(RETRY_NOTIFICATION_ID, builder.build())
    }

    /**
     * Show an error notification when location is unavailable.
     */
    fun showLocationErrorNotification() {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            3,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to go — Location unavailable")
            .setContentText("Couldn't determine your current location. Please check your location permissions and try again.")
            .setContentIntent(appPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)

        notificationManager.notify(RETRY_NOTIFICATION_ID, builder.build())
    }

    /**
     * Show a notification prompting the user to re-sign in.
     */
    fun showReSignInNotification() {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            4,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to Go — Sign in required")
            .setContentText("Please re-sign in to Time to Go.")
            .setContentIntent(appPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(RETRY_NOTIFICATION_ID, builder.build())
    }
}
