package com.timetogo.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar
import javax.inject.Inject

/**
 * Wrapper around AlarmManager for scheduling and cancelling exact alarms.
 * Uses setExactAndAllowWhileIdle() for reliable delivery even in Doze mode.
 */
class AlarmScheduler @Inject constructor(
    private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val ALARM_REQUEST_CODE = 100
    }

    /**
     * Schedule an exact alarm at the specified hour and minute.
     * If the time has already passed today, schedules for tomorrow.
     *
     * @param hour Hour of day (0-23)
     * @param minute Minute of hour (0-59)
     * @return The scheduled time in milliseconds, or -1 if scheduling failed
     */
    fun scheduleAlarm(hour: Int, minute: Int): Long {
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms — permission not granted")
            return -1
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the time is in the past today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val pendingIntent = getAlarmPendingIntent()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d(TAG, "Alarm scheduled for ${calendar.time}")
        return calendar.timeInMillis
    }

    /**
     * Cancel any currently scheduled alarm.
     */
    fun cancelAlarm() {
        val pendingIntent = getAlarmPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarm cancelled")
    }

    /**
     * Schedule the next alarm for tomorrow at the given time.
     * Used after a recurring alarm fires to set up the next day's alarm.
     *
     * @param hour Hour of day (0-23)
     * @param minute Minute of hour (0-59)
     */
    fun scheduleNextDayAlarm(hour: Int, minute: Int): Long {
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms — permission not granted")
            return -1
        }

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val pendingIntent = getAlarmPendingIntent()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d(TAG, "Next day alarm scheduled for ${calendar.time}")
        return calendar.timeInMillis
    }

    /**
     * Check if exact alarm permission is granted.
     */
    fun canScheduleExactAlarms(): Boolean {
        return alarmManager.canScheduleExactAlarms()
    }

    private fun getAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.timetogo.app.ALARM_TRIGGER"
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
