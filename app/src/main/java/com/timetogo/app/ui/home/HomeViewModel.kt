package com.timetogo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetogo.app.alarm.AlarmScheduler
import com.timetogo.app.data.model.UserPreferences
import com.timetogo.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    data class HomeUiState(
        val userName: String = "",
        val homeAddress: String = "",
        val alarmHour: Int = 17,
        val alarmMinute: Int = 30,
        val alarmEnabled: Boolean = false,
        val isRecurring: Boolean = false,
        val statusText: String = "Alarm disabled",
        val hasAddress: Boolean = false,
        val lastFetchFailed: Boolean = false,
        val lastFetchError: String = ""
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                _uiState.value = HomeUiState(
                    userName = prefs.userName.split(" ").firstOrNull() ?: prefs.userName,
                    homeAddress = prefs.homeAddress,
                    alarmHour = prefs.alarmHour,
                    alarmMinute = prefs.alarmMinute,
                    alarmEnabled = prefs.alarmEnabled,
                    isRecurring = prefs.isRecurring,
                    statusText = getStatusText(prefs),
                    hasAddress = prefs.homeAddress.isNotEmpty(),
                    lastFetchFailed = prefs.lastFetchFailed,
                    lastFetchError = prefs.lastFetchError
                )
            }
        }
    }

    fun setHomeAddress(address: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            preferencesRepository.setHomeAddress(address, latitude, longitude)
        }
    }

    fun clearHomeAddress() {
        viewModelScope.launch {
            preferencesRepository.clearHomeAddress()
            // Also disable alarm if address is cleared
            if (_uiState.value.alarmEnabled) {
                setAlarmEnabled(false)
            }
        }
    }

    fun setAlarmTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setAlarmTime(hour, minute)
            // Reschedule alarm if enabled
            if (_uiState.value.alarmEnabled) {
                alarmScheduler.cancelAlarm()
                alarmScheduler.scheduleAlarm(hour, minute)
            }
        }
    }

    fun setAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAlarmEnabled(enabled)
            if (enabled) {
                val state = _uiState.value
                alarmScheduler.scheduleAlarm(state.alarmHour, state.alarmMinute)
            } else {
                alarmScheduler.cancelAlarm()
            }
        }
    }

    fun setRecurring(recurring: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRecurring(recurring)
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return alarmScheduler.canScheduleExactAlarms()
    }

    private fun getStatusText(prefs: UserPreferences): String {
        if (!prefs.alarmEnabled) return "Alarm disabled"
        if (prefs.homeAddress.isEmpty()) return "Please set your home address"

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, prefs.alarmHour)
            set(Calendar.MINUTE, prefs.alarmMinute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dateFormat = SimpleDateFormat("EEEE, HH:mm", Locale.getDefault())
        return "Next notification: ${dateFormat.format(Date(calendar.timeInMillis))}"
    }
}
