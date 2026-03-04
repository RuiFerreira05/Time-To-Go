package com.timetogo.app.notification

import android.app.NotificationManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [NotificationHelper] — constants, channel config, and instantiation.
 *
 * Note: showRouteNotification(), showErrorNotification(), etc. use
 * NotificationCompat.Builder and PendingIntent which return null in JVM unit tests.
 * Full notification display tests require instrumented (Android) tests.
 * These tests verify constants, channel properties, and class structure.
 */
class NotificationHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockNotificationManager: NotificationManager

    @Before
    fun setUp() {
        mockContext = mock()
        mockNotificationManager = mock()
        whenever(mockContext.getSystemService(Context.NOTIFICATION_SERVICE))
            .thenReturn(mockNotificationManager)
        whenever(mockContext.packageName).thenReturn("com.timetogo.app")
    }

    // ── Constants ───────────────────────────────────────────────────────

    @Test
    fun `CHANNEL_ID is correct`() {
        assertEquals("time_to_go_route", NotificationHelper.CHANNEL_ID)
    }

    @Test
    fun `CHANNEL_NAME is correct`() {
        assertEquals("Route Notifications", NotificationHelper.CHANNEL_NAME)
    }

    @Test
    fun `CHANNEL_DESCRIPTION is correct`() {
        assertEquals("Daily bus route notifications", NotificationHelper.CHANNEL_DESCRIPTION)
    }

    @Test
    fun `NOTIFICATION_ID and RETRY_NOTIFICATION_ID are distinct`() {
        assertTrue(
            "IDs must be different",
            NotificationHelper.NOTIFICATION_ID != NotificationHelper.RETRY_NOTIFICATION_ID
        )
    }

    @Test
    fun `NOTIFICATION_ID has expected value`() {
        assertEquals(1001, NotificationHelper.NOTIFICATION_ID)
    }

    @Test
    fun `RETRY_NOTIFICATION_ID has expected value`() {
        assertEquals(1002, NotificationHelper.RETRY_NOTIFICATION_ID)
    }

    @Test
    fun `ACTION_RETRY has correct value`() {
        assertEquals("com.timetogo.app.ACTION_RETRY", NotificationHelper.ACTION_RETRY)
    }

    // ── Instantiation ───────────────────────────────────────────────

    @Test
    fun `NotificationHelper can be instantiated with mock context`() {
        val helper = NotificationHelper(mockContext)
        assertNotNull(helper)
    }

    @Test
    fun `NotificationHelper is open for mocking`() {
        val mockedHelper: NotificationHelper = mock()
        assertNotNull(mockedHelper)
    }

    // ── Channel creation ────────────────────────────────────────────

    @Test
    fun `createNotificationChannel does not crash`() {
        val helper = NotificationHelper(mockContext)
        // This should call notificationManager.createNotificationChannel()
        // Without crash — the mock NotificationManager accepts any channel
        helper.createNotificationChannel()
        // No exception means success
    }
}
