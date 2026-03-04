package com.timetogo.app.ui.signin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetogo.app.data.repository.AuthRepository
import com.timetogo.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    data class SignInUiState(
        val isLoading: Boolean = false,
        val isSignedIn: Boolean = false,
        val errorMessage: String? = null,
        val noAccountFound: Boolean = false
    )

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun checkExistingSignIn() {
        viewModelScope.launch {
            val prefs = preferencesRepository.getCurrentPreferences()
            if (prefs.isSignedIn) {
                _uiState.value = SignInUiState(isSignedIn = true)
            }
        }
    }

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = SignInUiState(isLoading = true)

            val result = authRepository.signIn(activityContext)

            if (result.success) {
                preferencesRepository.setSignedIn(result.name, result.email)
                _uiState.value = SignInUiState(isSignedIn = true)
            } else {
                _uiState.value = SignInUiState(
                    isLoading = false,
                    errorMessage = result.errorMessage,
                    noAccountFound = result.noAccountFound
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, noAccountFound = false)
    }

    fun skipSignIn() {
        viewModelScope.launch {
            preferencesRepository.setSignedIn("Guest", "")
            _uiState.value = SignInUiState(isSignedIn = true)
        }
    }
}
