package com.timetogo.app.util

import android.Manifest
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
 * Unit tests for [PermissionHelper] — permission checks and constant validation.
 *
 * Note: Intent builders (getExactAlarmSettingsIntent, etc.) use Uri.parse()
 * which returns null in JVM unit tests. Full intent testing requires
 * instrumented (Android) tests. These tests verify permission checks and constants.
 */
class PermissionHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockAlarmManager: AlarmManager

    @Before
    fun setUp() {
        mockContext = mock()
        mockAlarmManager = mock()
        whenever(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager)
        whenever(mockContext.packageName).thenReturn("com.timetogo.app")
    }

    // ── Exact alarm permission ─────────────────────────────────────────

    @Test
    fun `hasExactAlarmPermission returns true when granted`() {
        whenever(mockAlarmManager.canScheduleExactAlarms()).thenReturn(true)

        assertTrue(PermissionHelper.hasExactAlarmPermission(mockContext))
    }

    @Test
    fun `hasExactAlarmPermission returns false when not granted`() {
        whenever(mockAlarmManager.canScheduleExactAlarms()).thenReturn(false)

        assertFalse(PermissionHelper.hasExactAlarmPermission(mockContext))
    }

    // ── Location permissions ────────────────────────────────────────
    // ContextCompat.checkSelfPermission returns 0 (GRANTED) with returnDefaultValues

    @Test
    fun `hasAnyLocationPermission returns boolean without crashing`() {
        val result = PermissionHelper.hasAnyLocationPermission(mockContext)
        assertTrue(result)
    }

    @Test
    fun `hasFineLocationPermission returns boolean without crashing`() {
        val result = PermissionHelper.hasFineLocationPermission(mockContext)
        assertTrue(result)
    }

    @Test
    fun `hasCoarseLocationPermission returns boolean without crashing`() {
        val result = PermissionHelper.hasCoarseLocationPermission(mockContext)
        assertTrue(result)
    }

    @Test
    fun `hasBackgroundLocationPermission returns boolean without crashing`() {
        val result = PermissionHelper.hasBackgroundLocationPermission(mockContext)
        assertTrue(result)
    }

    @Test
    fun `hasNotificationPermission returns boolean without crashing`() {
        val result = PermissionHelper.hasNotificationPermission(mockContext)
        assertTrue(result)
    }

    // ── REQUIRED_PERMISSIONS ────────────────────────────────────────

    @Test
    fun `REQUIRED_PERMISSIONS contains 3 permissions`() {
        assertEquals(3, PermissionHelper.REQUIRED_PERMISSIONS.size)
    }

    @Test
    fun `REQUIRED_PERMISSIONS contains POST_NOTIFICATIONS`() {
        assertTrue(PermissionHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun `REQUIRED_PERMISSIONS contains ACCESS_FINE_LOCATION`() {
        assertTrue(PermissionHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @Test
    fun `REQUIRED_PERMISSIONS contains ACCESS_COARSE_LOCATION`() {
        assertTrue(PermissionHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // ── hasAnyLocationPermission composition ─────────────────────────

    @Test
    fun `hasAnyLocationPermission is an OR of fine and coarse`() {
        // Both return true with mock defaults, so the OR should also be true
        val fine = PermissionHelper.hasFineLocationPermission(mockContext)
        val coarse = PermissionHelper.hasCoarseLocationPermission(mockContext)
        val any = PermissionHelper.hasAnyLocationPermission(mockContext)

        assertEquals(fine || coarse, any)
    }
}
