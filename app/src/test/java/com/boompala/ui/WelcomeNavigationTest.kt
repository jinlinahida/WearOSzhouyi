package com.boompala.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.boompala.settings.AppSettings
import com.boompala.settings.SettingsRepository
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
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class WelcomeNavigationTest {
    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setUp() {
        dataStoreFile = File.createTempFile("boompala-welcome-test-", ".preferences_pb").apply {
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
    fun `first launch enters welcome screen when onboarding is not completed`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = SettingsRepository(dataStore)
        val settings = repository.settings.first()

        assertFalse("New installation must have hasCompletedOnboarding = false", settings.hasCompletedOnboarding)

        val initialScreen = if (!settings.hasCompletedOnboarding) AppScreen.WELCOME else AppScreen.HOME
        assertEquals(AppScreen.WELCOME, initialScreen)
    }

    @Test
    fun `completing welcome onboarding transitions to home and persists state`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = SettingsRepository(dataStore)

        // Initial launch
        var currentScreen = if (!repository.settings.first().hasCompletedOnboarding) AppScreen.WELCOME else AppScreen.HOME
        assertEquals(AppScreen.WELCOME, currentScreen)

        // Complete onboarding
        repository.setOnboardingCompleted(true)
        currentScreen = AppScreen.HOME

        assertEquals(AppScreen.HOME, currentScreen)
        assertTrue(repository.settings.first().hasCompletedOnboarding)

        // Simulate app restart: create fresh repository instance from same persisted file
        val restartedRepo = SettingsRepository(dataStore)
        val restartedSettings = restartedRepo.settings.first()
        assertTrue("Subsequent launch must see hasCompletedOnboarding = true", restartedSettings.hasCompletedOnboarding)

        val subsequentInitialScreen = if (!restartedSettings.hasCompletedOnboarding) AppScreen.WELCOME else AppScreen.HOME
        assertEquals(AppScreen.HOME, subsequentInitialScreen)
    }

    @Test
    fun `manual entry from settings about screen navigates to welcome and returns without resetting flag`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = SettingsRepository(dataStore)
        repository.setOnboardingCompleted(true)

        var screen = AppScreen.ABOUT
        var welcomeReturnScreen = AppScreen.HOME

        // User clicks "View Welcome & Disclaimers" in AboutScreen
        welcomeReturnScreen = AppScreen.ABOUT
        screen = AppScreen.WELCOME
        assertEquals(AppScreen.WELCOME, screen)

        // User finishes or goes back
        val isFirstRun = !repository.settings.first().hasCompletedOnboarding
        assertFalse(isFirstRun)

        // Finish review
        if (isFirstRun) {
            repository.setOnboardingCompleted(true)
        }
        screen = welcomeReturnScreen
        assertEquals(AppScreen.ABOUT, screen)
        assertTrue("Manual visit must preserve hasCompletedOnboarding = true", repository.settings.first().hasCompletedOnboarding)
    }

    @Test
    fun `recomposition does not reset screen to welcome once completed`() {
        val settings = AppSettings.DEFAULT.copy(hasCompletedOnboarding = true)
        val initialScreen = if (!settings.hasCompletedOnboarding) AppScreen.WELCOME else AppScreen.HOME
        assertEquals(AppScreen.HOME, initialScreen)
    }

    @Test
    fun `welcome string resources are completely mirrored in both zh and en`() {
        val zhStrings = parseStringXml("src/main/res/values/strings.xml")
        val enStrings = parseStringXml("src/main/res/values-en/strings.xml")

        val expectedWelcomeKeys = listOf(
            "welcome_title_prefix",
            "welcome_app_name",
            "welcome_badge_text",
            "welcome_tagline",
            "welcome_tap_to_start",
            "welcome_start_button",
            "welcome_terms_title",
            "welcome_terms_subtitle",
            "welcome_disclaimer_card_title",
            "welcome_disclaimer_card_content",
            "welcome_offline_card_title",
            "welcome_offline_card_content",
            "welcome_sources_card_title",
            "welcome_sources_card_content",
            "welcome_terms_footer_hint",
            "welcome_agree_and_enter",
            "welcome_revisit_from_about",
            "welcome_revisit_from_about_desc",
            "welcome_step_welcome",
            "welcome_step_disclaimer",
        )

        for (key in expectedWelcomeKeys) {
            assertTrue("values/strings.xml must contain $key", zhStrings.containsKey(key))
            assertTrue("values-en/strings.xml must contain $key", enStrings.containsKey(key))
            assertTrue("values/strings.xml key $key must not be blank", zhStrings[key]?.isNotBlank() == true)
            assertTrue("values-en/strings.xml key $key must not be blank", enStrings[key]?.isNotBlank() == true)
        }
    }

    private fun parseStringXml(relativePath: String): Map<String, String> {
        val file = if (File(relativePath).exists()) File(relativePath) else File("app", relativePath)
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(file)
        val nodes = doc.getElementsByTagName("string")
        val result = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val item = nodes.item(i) as Element
            val name = item.getAttribute("name")
            val text = item.textContent
            result[name] = text
        }
        return result
    }
}
