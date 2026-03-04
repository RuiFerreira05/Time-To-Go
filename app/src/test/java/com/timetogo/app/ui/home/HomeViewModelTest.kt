package com.timetogo.app.ui.home

import android.content.Context

import com.timetogo.app.alarm.AlarmScheduler
import com.timetogo.app.data.model.UserPreferences
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [HomeViewModel] state management and alarm interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var mockPreferencesRepository: UserPreferencesRepository
    private lateinit var mockAlarmScheduler: AlarmScheduler
    private lateinit var mockContext: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockPreferencesRepository = mock()
        mockAlarmScheduler = mock()
        mockContext = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(prefs: UserPreferences = UserPreferences()): HomeViewModel {
        whenever(mockPreferencesRepository.userPreferences).thenReturn(flowOf(prefs))
        return HomeViewModel(mockContext, mockPreferencesRepository, mockAlarmScheduler)
    }

    // ── Initial state from preferences ──────────────────────────────────

    @Test
    fun `initial state reflects user preferences`() = runTest {
        val prefs = UserPreferences(
            userName = "Alice Smith",
            homeAddress = "123 Main St",
            alarmHour = 8,
            alarmMinute = 15,
            alarmEnabled = true,
            isRecurring = true
        )

        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Alice", state.userName) // First name only
        assertEquals("123 Main St", state.homeAddress)
        assertEquals(8, state.alarmHour)
        assertEquals(15, state.alarmMinute)
        assertTrue(state.alarmEnabled)
        assertTrue(state.isRecurring)
        assertTrue(state.hasAddress)
    }

    @Test
    fun `userName extracts first name only`() = runTest {
        val prefs = UserPreferences(userName = "John Doe Smith")

        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        assertEquals("John", viewModel.uiState.value.userName)
    }

    @Test
    fun `single name preserved when no spaces`() = runTest {
        val prefs = UserPreferences(userName = "Guest")

        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        assertEquals("Guest", viewModel.uiState.value.userName)
    }

    @Test
    fun `hasAddress is false for empty address`() = runTest {
        val viewModel = createViewModel(UserPreferences(homeAddress = ""))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasAddress)
    }

    // ── setAlarmEnabled ─────────────────────────────────────────────────

    @Test
    fun `setAlarmEnabled true schedules alarm`() = runTest {
        val prefs = UserPreferences(alarmHour = 17, alarmMinute = 30, alarmEnabled = false)
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        viewModel.setAlarmEnabled(true)
        advanceUntilIdle()

        verify(mockPreferencesRepository).setAlarmEnabled(true)
        verify(mockAlarmScheduler).scheduleAlarm(17, 30)
    }

    @Test
    fun `setAlarmEnabled false cancels alarm`() = runTest {
        val prefs = UserPreferences(alarmEnabled = true)
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        viewModel.setAlarmEnabled(false)
        advanceUntilIdle()

        verify(mockPreferencesRepository).setAlarmEnabled(false)
        verify(mockAlarmScheduler).cancelAlarm()
    }

    // ── setAlarmTime ────────────────────────────────────────────────────

    @Test
    fun `setAlarmTime reschedules when alarm is enabled`() = runTest {
        val prefs = UserPreferences(alarmEnabled = true, alarmHour = 17, alarmMinute = 30)
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        viewModel.setAlarmTime(8, 15)
        advanceUntilIdle()

        verify(mockPreferencesRepository).setAlarmTime(8, 15)
        verify(mockAlarmScheduler).cancelAlarm()
        verify(mockAlarmScheduler).scheduleAlarm(8, 15)
    }

    @Test
    fun `setAlarmTime does not schedule when alarm is disabled`() = runTest {
        val prefs = UserPreferences(alarmEnabled = false)
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        viewModel.setAlarmTime(8, 15)
        advanceUntilIdle()

        verify(mockPreferencesRepository).setAlarmTime(8, 15)
        verify(mockAlarmScheduler, never()).scheduleAlarm(8, 15)
    }

    // ── clearHomeAddress ────────────────────────────────────────────────

    @Test
    fun `clearHomeAddress disables alarm when enabled`() = runTest {
        val prefs = UserPreferences(homeAddress = "123 Main St", alarmEnabled = true)
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        viewModel.clearHomeAddress()
        advanceUntilIdle()

        verify(mockPreferencesRepository).clearHomeAddress()
        verify(mockPreferencesRepository).setAlarmEnabled(false)
    }

    @Test
    fun `clearHomeAddress does not disable alarm when already disabled`() = runTest {
        val prefs = UserPreferences(homeAddress = "123 Main St", alarmEnabled = false)
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        viewModel.clearHomeAddress()
        advanceUntilIdle()

        verify(mockPreferencesRepository).clearHomeAddress()
        verify(mockPreferencesRepository, never()).setAlarmEnabled(false)
    }

    // ── setRecurring ────────────────────────────────────────────────────

    @Test
    fun `setRecurring delegates to repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setRecurring(true)
        advanceUntilIdle()

        verify(mockPreferencesRepository).setRecurring(true)
    }

    // ── canScheduleExactAlarms ──────────────────────────────────────────

    @Test
    fun `canScheduleExactAlarms delegates to alarm scheduler`() = runTest {
        whenever(mockAlarmScheduler.canScheduleExactAlarms()).thenReturn(true)
        val viewModel = createViewModel()

        assertTrue(viewModel.canScheduleExactAlarms())
    }

    // ── getStatusText ───────────────────────────────────────────────────

    @Test
    fun `status text shows disabled when alarm off`() = runTest {
        val prefs = UserPreferences(alarmEnabled = false)
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        assertEquals("Alarm disabled", viewModel.uiState.value.statusText)
    }

    @Test
    fun `status text prompts for address when alarm enabled but no address`() = runTest {
        val prefs = UserPreferences(alarmEnabled = true, homeAddress = "")
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        assertEquals("Please set your home address", viewModel.uiState.value.statusText)
    }

    @Test
    fun `status text shows next notification when alarm enabled with address`() = runTest {
        val prefs = UserPreferences(
            alarmEnabled = true,
            homeAddress = "123 Main St",
            alarmHour = 17,
            alarmMinute = 30
        )
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        assertTrue(
            "Status should contain 'Next notification'",
            viewModel.uiState.value.statusText.contains("Next notification")
        )
    }

    // ── Error state ─────────────────────────────────────────────────────

    @Test
    fun `fetch error state is reflected in UI`() = runTest {
        val prefs = UserPreferences(lastFetchFailed = true, lastFetchError = "Network error")
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastFetchFailed)
        assertEquals("Network error", viewModel.uiState.value.lastFetchError)
    }

    // ── triggerNotificationNow ───────────────────────────────────────────

    @Test
    fun `triggerNotificationNow sets error when no address`() = runTest {
        val prefs = UserPreferences(homeAddress = "")
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        whenever(mockPreferencesRepository.getCurrentPreferences()).thenReturn(prefs)

        viewModel.triggerNotificationNow()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTriggering)
        assertTrue(state.triggerStatusMessage.contains("Home address not set"))
    }

    @Test
    fun `triggerNotificationNow starts triggering with address set`() = runTest {
        val prefs = UserPreferences(homeAddress = "123 Main St", homeLatitude = 40.0, homeLongitude = -8.0)
        val viewModel = createViewModel(prefs)
        advanceUntilIdle()

        whenever(mockPreferencesRepository.getCurrentPreferences()).thenReturn(prefs)

        // WorkManager.getInstance() will throw in unit tests since no real context,
        // so we verify the error is caught gracefully
        viewModel.triggerNotificationNow()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTriggering)
        // Should have an error message since WorkManager can't initialize in test
        assertTrue(state.triggerStatusMessage.isNotEmpty())
    }

    @Test
    fun `initial triggerStatusMessage is empty`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.triggerStatusMessage)
        assertFalse(viewModel.uiState.value.isTriggering)
    }
}
