package com.timetogo.app.alarm

import android.app.AlarmManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [AlarmScheduler] — permission checks and time calculation logic.
 *
 * Note: scheduleAlarm() and cancelAlarm() internally use PendingIntent.getBroadcast()
 * which returns null in JVM unit tests. Full alarm scheduling tests require
 * instrumented (Android) tests. These tests verify the permission-gating logic
 * and ensure the scheduler handles edge cases correctly.
 */
class AlarmSchedulerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAlarmManager: AlarmManager
    private lateinit var alarmScheduler: AlarmScheduler

    @Before
    fun setUp() {
        mockContext = mock()
        mockAlarmManager = mock()
        whenever(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager)
        whenever(mockContext.packageName).thenReturn("com.timetogo.app")
        alarmScheduler = AlarmScheduler(mockContext)
    }

    // ── canScheduleExactAlarms ────────────────────────────────────────

    @Test
    fun `canScheduleExactAlarms returns true when granted`() {
        whenever(mockAlarmManager.canScheduleExactAlarms()).thenReturn(true)

        assertTrue(alarmScheduler.canScheduleExactAlarms())
    }

    @Test
    fun `canScheduleExactAlarms returns false when not granted`() {
        whenever(mockAlarmManager.canScheduleExactAlarms()).thenReturn(false)

        assertFalse(alarmScheduler.canScheduleExactAlarms())
    }

    // ── scheduleAlarm permission gating ──────────────────────────────

    @Test
    fun `scheduleAlarm returns -1 when exact alarm permission not granted`() {
        whenever(mockAlarmManager.canScheduleExactAlarms()).thenReturn(false)

        val result = alarmScheduler.scheduleAlarm(17, 30)

        assertEquals(-1L, result)
    }

    @Test
    fun `scheduleNextDayAlarm returns -1 when exact alarm permission not granted`() {
        whenever(mockAlarmManager.canScheduleExactAlarms()).thenReturn(false)

        val result = alarmScheduler.scheduleNextDayAlarm(8, 0)

        assertEquals(-1L, result)
    }

    // ── AlarmScheduler instantiation ─────────────────────────────────

    @Test
    fun `AlarmScheduler can be instantiated with mock context`() {
        assertNotNull(alarmScheduler)
    }

    @Test
    fun `AlarmScheduler is open for mocking`() {
        val mockedScheduler: AlarmScheduler = mock()
        assertNotNull(mockedScheduler)
    }

    // ── WORK_NAME from AlarmReceiver ─────────────────────────────────

    @Test
    fun `AlarmReceiver WORK_NAME is correct`() {
        assertEquals("route_fetch_work", AlarmReceiver.WORK_NAME)
    }
}
