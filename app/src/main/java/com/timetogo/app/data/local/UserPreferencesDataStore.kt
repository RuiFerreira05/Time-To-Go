package com.timetogo.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.timetogo.app.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Single, centralized DataStore delegate for the entire app.
 * Import this property wherever DataStore access is needed to guarantee
 * only one DataStore instance exists per process for "user_preferences".
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

open class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val HOME_ADDRESS = stringPreferencesKey("home_address")
        val HOME_LATITUDE = doublePreferencesKey("home_latitude")
        val HOME_LONGITUDE = doublePreferencesKey("home_longitude")
        val ALARM_HOUR = intPreferencesKey("alarm_hour")
        val ALARM_MINUTE = intPreferencesKey("alarm_minute")
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val IS_RECURRING = booleanPreferencesKey("is_recurring")
        val IS_DETAILED_NOTIFICATION = booleanPreferencesKey("is_detailed_notification")
        val LAST_ROUTE_JSON = stringPreferencesKey("last_route_json")
        val LAST_ROUTE_FETCH_TIME = longPreferencesKey("last_route_fetch_time")
        val LAST_FETCH_FAILED = booleanPreferencesKey("last_fetch_failed")
        val LAST_FETCH_ERROR = stringPreferencesKey("last_fetch_error")
    }

    open val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            isSignedIn = prefs[IS_SIGNED_IN] ?: false,
            userName = prefs[USER_NAME] ?: "",
            userEmail = prefs[USER_EMAIL] ?: "",
            homeAddress = prefs[HOME_ADDRESS] ?: "",
            homeLatitude = prefs[HOME_LATITUDE] ?: 0.0,
            homeLongitude = prefs[HOME_LONGITUDE] ?: 0.0,
            alarmHour = prefs[ALARM_HOUR] ?: 17,
            alarmMinute = prefs[ALARM_MINUTE] ?: 30,
            alarmEnabled = prefs[ALARM_ENABLED] ?: false,
            isRecurring = prefs[IS_RECURRING] ?: false,
            isDetailedNotification = prefs[IS_DETAILED_NOTIFICATION] ?: true,
            lastRouteJson = prefs[LAST_ROUTE_JSON] ?: "",
            lastRouteFetchTime = prefs[LAST_ROUTE_FETCH_TIME] ?: 0L,
            lastFetchFailed = prefs[LAST_FETCH_FAILED] ?: false,
            lastFetchError = prefs[LAST_FETCH_ERROR] ?: ""
        )
    }

    open suspend fun setSignedIn(signedIn: Boolean, name: String, email: String) {
        dataStore.edit { prefs ->
            prefs[IS_SIGNED_IN] = signedIn
            prefs[USER_NAME] = name
            prefs[USER_EMAIL] = email
        }
    }

    open suspend fun clearSignIn() {
        dataStore.edit { prefs ->
            prefs[IS_SIGNED_IN] = false
            prefs[USER_NAME] = ""
            prefs[USER_EMAIL] = ""
        }
    }

    open suspend fun setHomeAddress(address: String, latitude: Double, longitude: Double) {
        dataStore.edit { prefs ->
            prefs[HOME_ADDRESS] = address
            prefs[HOME_LATITUDE] = latitude
            prefs[HOME_LONGITUDE] = longitude
        }
    }

    open suspend fun clearHomeAddress() {
        dataStore.edit { prefs ->
            prefs[HOME_ADDRESS] = ""
            prefs[HOME_LATITUDE] = 0.0
            prefs[HOME_LONGITUDE] = 0.0
        }
    }

    open suspend fun setAlarmTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[ALARM_HOUR] = hour
            prefs[ALARM_MINUTE] = minute
        }
    }

    open suspend fun setAlarmEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[ALARM_ENABLED] = enabled
        }
    }

    open suspend fun setRecurring(recurring: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_RECURRING] = recurring
        }
    }

    open suspend fun setDetailedNotification(detailed: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_DETAILED_NOTIFICATION] = detailed
        }
    }

    open suspend fun cacheRoute(routeJson: String) {
        dataStore.edit { prefs ->
            prefs[LAST_ROUTE_JSON] = routeJson
            prefs[LAST_ROUTE_FETCH_TIME] = System.currentTimeMillis()
            prefs[LAST_FETCH_FAILED] = false
            prefs[LAST_FETCH_ERROR] = ""
        }
    }

    open suspend fun setFetchFailed(error: String) {
        dataStore.edit { prefs ->
            prefs[LAST_FETCH_FAILED] = true
            prefs[LAST_FETCH_ERROR] = error
        }
    }

    open suspend fun clearFetchError() {
        dataStore.edit { prefs ->
            prefs[LAST_FETCH_FAILED] = false
            prefs[LAST_FETCH_ERROR] = ""
        }
    }
}
