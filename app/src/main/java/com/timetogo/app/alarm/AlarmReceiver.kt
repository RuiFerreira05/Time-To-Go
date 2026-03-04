package com.timetogo.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.timetogo.app.notification.NotificationHelper
import com.timetogo.app.work.RouteFetchWorker

/**
 * BroadcastReceiver that fires when the scheduled alarm triggers.
 * Enqueues an expedited OneTimeWorkRequest via WorkManager to fetch the route
 * and display a notification. This avoids long-running operations in the receiver.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val WORK_NAME = "route_fetch_work"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Alarm received! Action: ${intent?.action}")

        val isRetry = intent?.action == NotificationHelper.ACTION_RETRY

        // Enqueue an expedited work request to fetch the route
        val workRequest = OneTimeWorkRequestBuilder<RouteFetchWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(if (isRetry) "retry" else "alarm")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        Log.d(TAG, "RouteFetchWorker enqueued (retry=$isRetry)")
    }
}
