package com.timetogo.app.receiver

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.timetogo.app.data.local.UserPreferencesDataStore
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Unit tests for [BootReceiver] — verifies boot-completed handling logic.
 *
 * Note: BootReceiver reads DataStore directly (no Hilt injection), so full
 * integration testing requires an instrumented test with a real DataStore.
 * These unit tests verify the receiver's structure and intent filtering.
 */
class BootReceiverTest {

    private lateinit var bootReceiver: BootReceiver

    @Before
    fun setUp() {
        bootReceiver = BootReceiver()
    }

    @Test
    fun `BootReceiver can be instantiated`() {
        assertNotNull(bootReceiver)
    }

    @Test
    fun `onReceive ignores non-BOOT_COMPLETED intents`() {
        val mockContext: Context = mock()
        val nonBootIntent = Intent("android.intent.action.SOME_OTHER_ACTION")

        // Should return early without crashing
        bootReceiver.onReceive(mockContext, nonBootIntent)
    }

    @Test
    fun `onReceive ignores null intent`() {
        val mockContext: Context = mock()

        // Should return early without crashing
        bootReceiver.onReceive(mockContext, null)
    }

    @Test
    fun `DataStore preference keys exist`() {
        // Verify the keys used by BootReceiver are accessible
        assertNotNull(UserPreferencesDataStore.ALARM_ENABLED)
        assertNotNull(UserPreferencesDataStore.ALARM_HOUR)
        assertNotNull(UserPreferencesDataStore.ALARM_MINUTE)
    }
}
