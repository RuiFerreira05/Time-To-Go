package com.timetogo.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UserPreferences] default values and data class behavior.
 */
class UserPreferencesTest {

    @Test
    fun `default preferences have correct initial values`() {
        val prefs = UserPreferences()

        assertFalse(prefs.isSignedIn)
        assertEquals("", prefs.userName)
        assertEquals("", prefs.userEmail)
        assertEquals("", prefs.homeAddress)
        assertEquals(0.0, prefs.homeLatitude, 0.001)
        assertEquals(0.0, prefs.homeLongitude, 0.001)
        assertEquals(17, prefs.alarmHour)
        assertEquals(30, prefs.alarmMinute)
        assertFalse(prefs.alarmEnabled)
        assertFalse(prefs.isRecurring)
        assertTrue(prefs.isDetailedNotification)
        assertEquals("", prefs.lastRouteJson)
        assertEquals(0L, prefs.lastRouteFetchTime)
        assertFalse(prefs.lastFetchFailed)
        assertEquals("", prefs.lastFetchError)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = UserPreferences(
            isSignedIn = true,
            userName = "Test User",
            userEmail = "test@example.com",
            homeAddress = "123 Main St",
            alarmHour = 8,
            alarmMinute = 15
        )

        val updated = original.copy(alarmEnabled = true)

        assertTrue(updated.isSignedIn)
        assertEquals("Test User", updated.userName)
        assertEquals("test@example.com", updated.userEmail)
        assertEquals("123 Main St", updated.homeAddress)
        assertEquals(8, updated.alarmHour)
        assertEquals(15, updated.alarmMinute)
        assertTrue(updated.alarmEnabled)
    }

    @Test
    fun `equality works correctly`() {
        val a = UserPreferences(isSignedIn = true, userName = "Alice")
        val b = UserPreferences(isSignedIn = true, userName = "Alice")
        val c = UserPreferences(isSignedIn = true, userName = "Bob")

        assertEquals(a, b)
        assertFalse(a == c)
    }
}
