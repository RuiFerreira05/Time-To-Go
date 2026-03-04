package com.timetogo.app.di
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.timetogo.app.alarm.AlarmScheduler
import com.timetogo.app.data.local.UserPreferencesDataStore
import com.timetogo.app.data.local.dataStore
import com.timetogo.app.data.repository.AuthRepository
import com.timetogo.app.data.repository.UserPreferencesRepository
import com.timetogo.app.notification.NotificationHelper
import com.timetogo.app.util.GoogleMapsIntentBuilder
import com.timetogo.app.util.LocationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(dataStore: DataStore<Preferences>): UserPreferencesDataStore {
        return UserPreferencesDataStore(dataStore)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(dataStore: UserPreferencesDataStore): UserPreferencesRepository {
        return UserPreferencesRepository(dataStore)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository {
        return AuthRepository(context)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler {
        return AlarmScheduler(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideLocationHelper(@ApplicationContext context: Context): LocationHelper {
        return LocationHelper(context)
    }

    @Provides
    @Singleton
    fun provideGoogleMapsIntentBuilder(): GoogleMapsIntentBuilder {
        return GoogleMapsIntentBuilder()
    }
}
