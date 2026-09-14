package com.dosparta.triviagame2.reminder

import android.content.Context
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class ReminderPreferencesStoreTest {

    private lateinit var context: Context
    private lateinit var store: ReminderPreferencesStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("daily_reminder_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = ReminderPreferencesStore(context)
    }

    @Test
    fun `load returns defaults when no values are saved`() {
        val settings = store.load()

        assertFalse(settings.enabled)
        assertEquals(ReminderPreferencesStore.DEFAULT_HOUR, settings.hour)
        assertEquals(ReminderPreferencesStore.DEFAULT_MINUTE, settings.minute)
    }

    @Test
    fun `saveEnabled persists enabled flag`() {
        store.saveEnabled(true)

        assertTrue(store.load().enabled)
    }

    @Test
    fun `saveTime persists hour and minute`() {
        store.saveTime(8, 30)

        val settings = store.load()
        assertEquals(8, settings.hour)
        assertEquals(30, settings.minute)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveTime rejects out-of-range values`() {
        store.saveTime(25, 0)
    }
}
