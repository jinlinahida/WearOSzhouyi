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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        repository.setHapticFeedbackEnabled(false)
        repository.setHapticIntensity(HapticIntensity.STRONG)
        repository.setLanguage(AppLanguage.ENGLISH)

        val updated = SettingsRepository(dataStore).settings.first()
        assertEquals(ScreenMode.ROUND, updated.screenMode)
        assertEquals(ContentSize.LARGE, updated.contentSize)
        assertEquals(false, updated.animationsEnabled)
        assertEquals(false, updated.rotaryScrollingEnabled)
        assertEquals(false, updated.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.STRONG, updated.hapticIntensity)
        assertEquals(AppLanguage.ENGLISH, updated.language)
    }

    @Test
    fun `home feature toggles and reordering work in repository`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = SettingsRepository(dataStore)

        repository.toggleHomeFeatureVisibility(HomeFeature.SIX_YAO)
        var current = repository.settings.first()
        assertTrue(current.hiddenHomeFeatures.contains(HomeFeature.SIX_YAO))

        repository.toggleHomeFeatureVisibility(HomeFeature.SIX_YAO)
        current = repository.settings.first()
        assertFalse(current.hiddenHomeFeatures.contains(HomeFeature.SIX_YAO))

        val firstFeature = current.effectiveHomeOrder().first()
        val secondFeature = current.effectiveHomeOrder()[1]
        repository.moveHomeFeature(secondFeature, moveUp = true)

        current = repository.settings.first()
        assertEquals(secondFeature, current.effectiveHomeOrder().first())
        assertEquals(firstFeature, current.effectiveHomeOrder()[1])
    }

    @Test
    fun `onboarding status is false by default and can be marked as completed`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = SettingsRepository(dataStore)

        assertEquals(false, repository.settings.first().hasCompletedOnboarding)

        repository.setOnboardingCompleted(true)
        assertEquals(true, repository.settings.first().hasCompletedOnboarding)

        val newRepoInstance = SettingsRepository(dataStore)
        assertEquals(true, newRepoInstance.settings.first().hasCompletedOnboarding)
    }
}
