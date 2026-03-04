package com.timetogo.app.data.repository

import com.timetogo.app.data.local.UserPreferencesDataStore
import com.timetogo.app.data.model.UserPreferences
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [UserPreferencesRepository] delegation to [UserPreferencesDataStore].
 */
class UserPreferencesRepositoryTest {

    private lateinit var mockDataStore: UserPreferencesDataStore
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        mockDataStore = mock()
        repository = UserPreferencesRepository(mockDataStore)
    }

    // ── userPreferences flow ────────────────────────────────────────────

    @Test
    fun `userPreferences returns flow from data store`() = runTest {
        val prefs = UserPreferences(isSignedIn = true, userName = "Test")
        whenever(mockDataStore.userPreferences).thenReturn(flowOf(prefs))

        val repository = UserPreferencesRepository(mockDataStore)
        val result = repository.userPreferences.first()

        assertTrue(result.isSignedIn)
        assertEquals("Test", result.userName)
    }

    // ── getCurrentPreferences ───────────────────────────────────────────

    @Test
    fun `getCurrentPreferences returns first value from flow`() = runTest {
        val prefs = UserPreferences(alarmHour = 8, alarmMinute = 15)
        whenever(mockDataStore.userPreferences).thenReturn(flowOf(prefs))

        val repository = UserPreferencesRepository(mockDataStore)
        val result = repository.getCurrentPreferences()

        assertEquals(8, result.alarmHour)
        assertEquals(15, result.alarmMinute)
    }

    // ── Delegation methods ──────────────────────────────────────────────

    @Test
    fun `setSignedIn delegates to data store`() = runTest {
        repository.setSignedIn("Alice", "alice@test.com")
        verify(mockDataStore).setSignedIn(signedIn = true, name = "Alice", email = "alice@test.com")
    }

    @Test
    fun `signOut delegates to clearSignIn`() = runTest {
        repository.signOut()
        verify(mockDataStore).clearSignIn()
    }

    @Test
    fun `setHomeAddress delegates to data store`() = runTest {
        repository.setHomeAddress("123 Main St", 38.76, -9.14)
        verify(mockDataStore).setHomeAddress("123 Main St", 38.76, -9.14)
    }

    @Test
    fun `clearHomeAddress delegates to data store`() = runTest {
        repository.clearHomeAddress()
        verify(mockDataStore).clearHomeAddress()
    }

    @Test
    fun `setAlarmTime delegates to data store`() = runTest {
        repository.setAlarmTime(17, 30)
        verify(mockDataStore).setAlarmTime(17, 30)
    }

    @Test
    fun `setAlarmEnabled delegates to data store`() = runTest {
        repository.setAlarmEnabled(true)
        verify(mockDataStore).setAlarmEnabled(true)
    }

    @Test
    fun `setRecurring delegates to data store`() = runTest {
        repository.setRecurring(true)
        verify(mockDataStore).setRecurring(true)
    }

    @Test
    fun `setDetailedNotification delegates to data store`() = runTest {
        repository.setDetailedNotification(false)
        verify(mockDataStore).setDetailedNotification(false)
    }

    @Test
    fun `cacheRoute delegates to data store`() = runTest {
        repository.cacheRoute("{\"route\":\"data\"}")
        verify(mockDataStore).cacheRoute("{\"route\":\"data\"}")
    }

    @Test
    fun `setFetchFailed delegates to data store`() = runTest {
        repository.setFetchFailed("Network error")
        verify(mockDataStore).setFetchFailed("Network error")
    }

    @Test
    fun `clearFetchError delegates to data store`() = runTest {
        repository.clearFetchError()
        verify(mockDataStore).clearFetchError()
    }
}
