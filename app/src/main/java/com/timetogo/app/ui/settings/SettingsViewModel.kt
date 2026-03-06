package com.timetogo.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetogo.app.alarm.AlarmScheduler
import com.timetogo.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    data class SettingsUiState(
        val isDetailedNotification: Boolean = true
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                _uiState.value = SettingsUiState(
                    isDetailedNotification = prefs.isDetailedNotification
                )
            }
        }
    }

    fun setDetailedNotification(detailed: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDetailedNotification(detailed)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm()
            preferencesRepository.signOut()
            preferencesRepository.setAlarmEnabled(false)
        }
    }
}
