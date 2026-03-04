package com.timetogo.app.data.repository

import com.timetogo.app.data.local.UserPreferencesDataStore
import com.timetogo.app.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Repository providing access to user preferences stored in DataStore.
 */
open class UserPreferencesRepository @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    open val userPreferences: Flow<UserPreferences> = dataStore.userPreferences

    open suspend fun getCurrentPreferences(): UserPreferences {
        return dataStore.userPreferences.first()
    }

    open suspend fun setSignedIn(name: String, email: String) {
        dataStore.setSignedIn(signedIn = true, name = name, email = email)
    }

    open suspend fun signOut() {
        dataStore.clearSignIn()
    }

    open suspend fun setHomeAddress(address: String, latitude: Double, longitude: Double) {
        dataStore.setHomeAddress(address, latitude, longitude)
    }

    open suspend fun clearHomeAddress() {
        dataStore.clearHomeAddress()
    }

    open suspend fun setAlarmTime(hour: Int, minute: Int) {
        dataStore.setAlarmTime(hour, minute)
    }

    open suspend fun setAlarmEnabled(enabled: Boolean) {
        dataStore.setAlarmEnabled(enabled)
    }

    open suspend fun setRecurring(recurring: Boolean) {
        dataStore.setRecurring(recurring)
    }

    open suspend fun setDetailedNotification(detailed: Boolean) {
        dataStore.setDetailedNotification(detailed)
    }

    open suspend fun cacheRoute(routeJson: String) {
        dataStore.cacheRoute(routeJson)
    }

    open suspend fun setFetchFailed(error: String) {
        dataStore.setFetchFailed(error)
    }

    open suspend fun clearFetchError() {
        dataStore.clearFetchError()
    }
}
