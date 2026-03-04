package com.timetogo.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.timetogo.app.alarm.AlarmScheduler
import com.timetogo.app.data.local.UserPreferencesDataStore
import com.timetogo.app.data.local.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for BOOT_COMPLETED.
 * Re-schedules the alarm after device reboot if it was previously enabled.
 *
 * Note: This receiver cannot use Hilt injection because it's a manifest-registered
 * BroadcastReceiver. It reads DataStore directly to check alarm state.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed — checking if alarm needs rescheduling")

        val alarmScheduler = AlarmScheduler(context)

        // Use goAsync() to get more time for the DataStore read
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.applicationContext.dataStore.data.first()
                val alarmEnabled = prefs[UserPreferencesDataStore.ALARM_ENABLED] ?: false
                val alarmHour = prefs[UserPreferencesDataStore.ALARM_HOUR] ?: 17
                val alarmMinute = prefs[UserPreferencesDataStore.ALARM_MINUTE] ?: 30

                if (alarmEnabled) {
                    val scheduledTime = alarmScheduler.scheduleAlarm(alarmHour, alarmMinute)
                    Log.d(TAG, "Alarm rescheduled after boot at $alarmHour:$alarmMinute (time=$scheduledTime)")
                } else {
                    Log.d(TAG, "Alarm was not enabled, skipping reschedule")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarm after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
