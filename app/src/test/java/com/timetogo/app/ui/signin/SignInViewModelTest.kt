package com.timetogo.app.ui.signin

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SignInViewModel] state transitions and authentication flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockPreferencesRepository: UserPreferencesRepository
    private lateinit var viewModel: SignInViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockAuthRepository = mock()
        mockPreferencesRepository = mock()
        viewModel = SignInViewModel(mockAuthRepository, mockPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── checkExistingSignIn ─────────────────────────────────────────────

    @Test
    fun `checkExistingSignIn sets isSignedIn when signed in`() = runTest {
        whenever(mockPreferencesRepository.getCurrentPreferences())
            .thenReturn(UserPreferences(isSignedIn = true))

        viewModel.checkExistingSignIn()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun `checkExistingSignIn does not set isSignedIn when not signed in`() = runTest {
        whenever(mockPreferencesRepository.getCurrentPreferences())
            .thenReturn(UserPreferences(isSignedIn = false))

        viewModel.checkExistingSignIn()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSignedIn)
    }

    // ── skipSignIn ──────────────────────────────────────────────────────

    @Test
    fun `skipSignIn sets Guest user`() = runTest {
        viewModel.skipSignIn()
        advanceUntilIdle()

        verify(mockPreferencesRepository).setSignedIn("Guest", "")
        assertTrue(viewModel.uiState.value.isSignedIn)
    }

    // ── clearError ──────────────────────────────────────────────────────

    @Test
    fun `clearError resets error message`() = runTest {
        // Manually set an error state first by accessing internal state
        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.noAccountFound)
    }

    // ── Initial state ───────────────────────────────────────────────────

    @Test
    fun `initial state is not loading and not signed in`() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isSignedIn)
        assertNull(state.errorMessage)
        assertFalse(state.noAccountFound)
    }
}
