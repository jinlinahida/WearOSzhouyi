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

    @Test
    fun `user birth settings can be saved and cleared`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = SettingsRepository(dataStore)

        val initial = repository.settings.first()
        assertEquals(null, initial.userBirthDate)
        assertEquals(null, initial.userBirthHour)
        assertEquals(com.boompala.engine.bazi.BaziGender.MALE, initial.userGender)

        repository.setUserBirth(
            birthDate = "1995-10-24",
            birthHour = 9,
            gender = com.boompala.engine.bazi.BaziGender.FEMALE,
        )
        val saved = repository.settings.first()
        assertEquals("1995-10-24", saved.userBirthDate)
        assertEquals(9, saved.userBirthHour)
        assertEquals(com.boompala.engine.bazi.BaziGender.FEMALE, saved.userGender)

        repository.clearUserBirth()
        val cleared = repository.settings.first()
        assertEquals(null, cleared.userBirthDate)
        assertEquals(null, cleared.userBirthHour)
        assertEquals(com.boompala.engine.bazi.BaziGender.MALE, cleared.userGender)
    }

    @Test
    fun `new preferences and resetAllPreferences work as expected`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = SettingsRepository(dataStore)

        val initial = repository.settings.first()
        assertFalse(initial.keepScreenOnEnabled)
        assertFalse(initial.compassTrueNorthEnabled)
        assertEquals(0f, initial.compassDeclination)
        assertTrue(initial.tarotReversedEnabled)
        assertFalse(initial.tarotMajorArcanaOnly)

        repository.setKeepScreenOnEnabled(true)
        repository.setCompassTrueNorthEnabled(true)
        repository.setCompassDeclination(-6.0f)
        repository.setTarotReversedEnabled(false)
        repository.setTarotMajorArcanaOnly(true)
        repository.setScreenMode(ScreenMode.ROUND)
        repository.setUserBirth("1990-01-01", 12, com.boompala.engine.bazi.BaziGender.FEMALE)

        var updated = repository.settings.first()
        assertTrue(updated.keepScreenOnEnabled)
        assertTrue(updated.compassTrueNorthEnabled)
        assertEquals(-6.0f, updated.compassDeclination)
        assertFalse(updated.tarotReversedEnabled)
        assertTrue(updated.tarotMajorArcanaOnly)
        assertEquals(ScreenMode.ROUND, updated.screenMode)
        assertEquals("1990-01-01", updated.userBirthDate)

        repository.resetAllPreferences()
        val reset = repository.settings.first()
        assertFalse(reset.keepScreenOnEnabled)
        assertFalse(reset.compassTrueNorthEnabled)
        assertEquals(0f, reset.compassDeclination)
        assertTrue(reset.tarotReversedEnabled)
        assertFalse(reset.tarotMajorArcanaOnly)
        assertEquals(ScreenMode.AUTO, reset.screenMode)
        // Birth profile should be preserved
        assertEquals("1990-01-01", reset.userBirthDate)
        assertEquals(12, reset.userBirthHour)
        assertEquals(com.boompala.engine.bazi.BaziGender.FEMALE, reset.userGender)
    }
}
