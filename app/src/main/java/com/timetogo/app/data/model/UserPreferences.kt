package com.timetogo.app.data.model

/**
 * Domain model representing the user's saved preferences and state.
 */
data class UserPreferences(
    val isSignedIn: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val homeAddress: String = "",
    val homeLatitude: Double = 0.0,
    val homeLongitude: Double = 0.0,
    val alarmHour: Int = 17,
    val alarmMinute: Int = 30,
    val alarmEnabled: Boolean = false,
    val isRecurring: Boolean = false,
    val isDetailedNotification: Boolean = true,
    val lastRouteJson: String = "",
    val lastRouteFetchTime: Long = 0L,
    val lastFetchFailed: Boolean = false,
    val lastFetchError: String = ""
)
