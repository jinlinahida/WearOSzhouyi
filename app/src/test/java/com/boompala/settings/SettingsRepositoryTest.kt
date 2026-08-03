package com.boompala.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {
    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setUp() {
        dataStoreFile = File.createTempFile("boompala-settings-", ".preferences_pb").apply {
            delete()
        }
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    @Test
    fun `defaults and user selections are read from preferences datastore`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = SettingsRepository(dataStore)

        assertEquals(AppSettings.DEFAULT, repository.settings.first())

        repository.setScreenMode(ScreenMode.ROUND)
        repository.setContentSize(ContentSize.LARGE)
        repository.setAnimationsEnabled(false)
        repository.setRotaryScrollingEnabled(false)

        assertEquals(
            AppSettings(
                screenMode = ScreenMode.ROUND,
                contentSize = ContentSize.LARGE,
                animationsEnabled = false,
                rotaryScrollingEnabled = false,
            ),
            SettingsRepository(dataStore).settings.first(),
        )
    }
}
