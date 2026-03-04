package com.timetogo.app.data.repository

import com.timetogo.app.data.local.UserPreferencesDataStore
import com.timetogo.app.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Repository providing access to user preferences stored in DataStore.
 */
class UserPreferencesRepository @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    val userPreferences: Flow<UserPreferences> = dataStore.userPreferences

    suspend fun getCurrentPreferences(): UserPreferences {
        return dataStore.userPreferences.first()
    }

    suspend fun setSignedIn(name: String, email: String) {
        dataStore.setSignedIn(signedIn = true, name = name, email = email)
    }

    suspend fun signOut() {
        dataStore.clearSignIn()
    }

    suspend fun setHomeAddress(address: String, latitude: Double, longitude: Double) {
        dataStore.setHomeAddress(address, latitude, longitude)
    }

    suspend fun clearHomeAddress() {
        dataStore.clearHomeAddress()
    }

    suspend fun setAlarmTime(hour: Int, minute: Int) {
        dataStore.setAlarmTime(hour, minute)
    }

    suspend fun setAlarmEnabled(enabled: Boolean) {
        dataStore.setAlarmEnabled(enabled)
    }

    suspend fun setRecurring(recurring: Boolean) {
        dataStore.setRecurring(recurring)
    }

    suspend fun setDetailedNotification(detailed: Boolean) {
        dataStore.setDetailedNotification(detailed)
    }

    suspend fun cacheRoute(routeJson: String) {
        dataStore.cacheRoute(routeJson)
    }

    suspend fun setFetchFailed(error: String) {
        dataStore.setFetchFailed(error)
    }

    suspend fun clearFetchError() {
        dataStore.clearFetchError()
    }
}
