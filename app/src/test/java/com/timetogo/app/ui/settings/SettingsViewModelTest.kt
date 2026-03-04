package com.timetogo.app.ui.settings

import com.timetogo.app.alarm.AlarmScheduler
import com.timetogo.app.data.model.UserPreferences
import com.timetogo.app.data.repository.AuthRepository
import com.timetogo.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SettingsViewModel] — preference observation and sign-out orchestration.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var mockPreferencesRepository: UserPreferencesRepository
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockAlarmScheduler: AlarmScheduler
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockPreferencesRepository = mock()
        mockAuthRepository = mock()
        mockAlarmScheduler = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(prefs: UserPreferences = UserPreferences()): SettingsViewModel {
        whenever(mockPreferencesRepository.userPreferences).thenReturn(flowOf(prefs))
        return SettingsViewModel(mockPreferencesRepository, mockAuthRepository, mockAlarmScheduler)
    }

    // ── Initial state from preferences ──────────────────────────────────

    @Test
    fun `initial state reflects user preferences`() = runTest {
        val prefs = UserPreferences(
            isDetailedNotification = false,
            userName = "Alice Smith",
            userEmail = "alice@test.com"
        )

        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isDetailedNotification)
        assertEquals("Alice Smith", state.userName)
        assertEquals("alice@test.com", state.userEmail)
    }

    // ── setDetailedNotification ─────────────────────────────────────────

    @Test
    fun `setDetailedNotification delegates to repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setDetailedNotification(false)
        advanceUntilIdle()

        verify(mockPreferencesRepository).setDetailedNotification(false)
    }

    // ── signOut ─────────────────────────────────────────────────────────

    @Test
    fun `signOut cancels alarm`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        verify(mockAlarmScheduler).cancelAlarm()
    }

    @Test
    fun `signOut signs out from auth repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        verify(mockAuthRepository).signOut()
    }

    @Test
    fun `signOut clears preferences`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        verify(mockPreferencesRepository).signOut()
    }

    @Test
    fun `signOut disables alarm`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        verify(mockPreferencesRepository).setAlarmEnabled(false)
    }

    @Test
    fun `signOut sets isSignedOut flag`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignedOut)
    }
}
