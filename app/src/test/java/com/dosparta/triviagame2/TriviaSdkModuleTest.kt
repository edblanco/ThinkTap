package com.dosparta.triviagame2

import android.os.Build
import com.dosparta.triviagame2.di.TriviaSdkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TriviaSdkModuleTest {
    @Test
    fun `trivia sdk is provided successfully in current flavor`() {
        val context = RuntimeEnvironment.getApplication()
        val sdk = TriviaSdkModule.provideTriviaSdk(context)
        assertNotNull(sdk)
    }

    @Test
    fun `build config matches flavor expectations`() {
        if (BuildConfig.FLAVOR == "free") {
            assertEquals(false, BuildConfig.SUPPORTS_EXTRA_LANGUAGES)
            assertEquals(false, BuildConfig.TRANSLATION_FRAMEWORKS_ENABLED)
        } else if (BuildConfig.FLAVOR == "pro") {
            assertEquals(true, BuildConfig.SUPPORTS_EXTRA_LANGUAGES)
            assertEquals(true, BuildConfig.TRANSLATION_FRAMEWORKS_ENABLED)
        }
    }
}
