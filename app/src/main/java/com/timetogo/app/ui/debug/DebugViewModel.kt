package com.timetogo.app.ui.debug

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetogo.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    data class DebugUiState(
        val logs: String = "",
        val appState: String = ""
    )

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    private val logEntries = mutableListOf<String>()

    init {
        loadAppState()
    }

    fun clearLogs() {
        logEntries.clear()
        _uiState.value = _uiState.value.copy(logs = "")
    }

    private fun addLog(level: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$timestamp] $level: $message"
        logEntries.add(entry)
        _uiState.value = _uiState.value.copy(logs = logEntries.joinToString("\n"))
    }

    private fun loadAppState() {
        viewModelScope.launch {
            try {
                val prefs = preferencesRepository.getCurrentPreferences()
                val stateText = buildString {
                    appendLine("Signed In: ${prefs.isSignedIn}")
                    appendLine("User: ${prefs.userName.ifEmpty { "Guest" }}")
                    appendLine("Email: ${prefs.userEmail.ifEmpty { "N/A" }}")
                    appendLine("Home: ${prefs.homeAddress.ifEmpty { "Not set" }}")
                    appendLine("Home Coords: ${prefs.homeLatitude}, ${prefs.homeLongitude}")
                    appendLine("Alarm: ${"%02d:%02d".format(prefs.alarmHour, prefs.alarmMinute)}")
                    appendLine("Alarm Enabled: ${prefs.alarmEnabled}")
                    appendLine("Recurring: ${prefs.isRecurring}")
                    appendLine("Detailed Notif: ${prefs.isDetailedNotification}")
                    appendLine("Last Fetch Failed: ${prefs.lastFetchFailed}")
                    if (prefs.lastFetchError.isNotEmpty()) {
                        appendLine("Last Error: ${prefs.lastFetchError}")
                    }
                }
                _uiState.value = _uiState.value.copy(appState = stateText.trimEnd())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(appState = "Error loading state: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "DebugViewModel"
    }
}
