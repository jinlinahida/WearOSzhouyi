package com.boompala.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.boompala.engine.bazi.BaziGender
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.appSettingsDataStore)

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AppSettings(
                screenMode = preferences[SCREEN_MODE_KEY].toEnumOrDefault(ScreenMode.AUTO),
                contentSize = preferences[CONTENT_SIZE_KEY].toEnumOrDefault(ContentSize.STANDARD),
                animationsEnabled = preferences[ANIMATIONS_ENABLED_KEY] ?: true,
                rotaryScrollingEnabled = preferences[ROTARY_SCROLLING_ENABLED_KEY] ?: true,
                hapticFeedbackEnabled = preferences[HAPTIC_FEEDBACK_ENABLED_KEY] ?: true,
                hapticIntensity = preferences[HAPTIC_INTENSITY_KEY].toEnumOrDefault(HapticIntensity.STANDARD),
                language = preferences[LANGUAGE_KEY].toEnumOrDefault(AppLanguage.CHINESE),
                homeOrder = parseHomeOrder(preferences[HOME_ORDER_KEY]),
                hiddenHomeFeatures = parseHiddenFeatures(preferences[HIDDEN_HOME_FEATURES_KEY]),
                hasCompletedOnboarding = preferences[ONBOARDING_COMPLETED_KEY] ?: false,
                userBirthDate = preferences[USER_BIRTH_DATE_KEY],
                userBirthHour = preferences[USER_BIRTH_HOUR_KEY]?.takeIf { it in 0..23 },
                userGender = preferences[USER_GENDER_KEY].toEnumOrDefault(BaziGender.MALE),
                keepScreenOnEnabled = preferences[KEEP_SCREEN_ON_ENABLED_KEY] ?: false,
                compassTrueNorthEnabled = preferences[COMPASS_TRUE_NORTH_ENABLED_KEY] ?: false,
                compassDeclination = preferences[COMPASS_DECLINATION_KEY] ?: 0f,
                tarotReversedEnabled = preferences[TAROT_REVERSED_ENABLED_KEY] ?: true,
                tarotMajorArcanaOnly = preferences[TAROT_MAJOR_ARCANA_ONLY_KEY] ?: false,
            )
        }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val current = AppSettings(
                screenMode = preferences[SCREEN_MODE_KEY].toEnumOrDefault(ScreenMode.AUTO),
                contentSize = preferences[CONTENT_SIZE_KEY].toEnumOrDefault(ContentSize.STANDARD),
                animationsEnabled = preferences[ANIMATIONS_ENABLED_KEY] ?: true,
                rotaryScrollingEnabled = preferences[ROTARY_SCROLLING_ENABLED_KEY] ?: true,
                hapticFeedbackEnabled = preferences[HAPTIC_FEEDBACK_ENABLED_KEY] ?: true,
                hapticIntensity = preferences[HAPTIC_INTENSITY_KEY].toEnumOrDefault(HapticIntensity.STANDARD),
                language = preferences[LANGUAGE_KEY].toEnumOrDefault(AppLanguage.CHINESE),
                homeOrder = parseHomeOrder(preferences[HOME_ORDER_KEY]),
                hiddenHomeFeatures = parseHiddenFeatures(preferences[HIDDEN_HOME_FEATURES_KEY]),
                hasCompletedOnboarding = preferences[ONBOARDING_COMPLETED_KEY] ?: false,
                userBirthDate = preferences[USER_BIRTH_DATE_KEY],
                userBirthHour = preferences[USER_BIRTH_HOUR_KEY]?.takeIf { it in 0..23 },
                userGender = preferences[USER_GENDER_KEY].toEnumOrDefault(BaziGender.MALE),
                keepScreenOnEnabled = preferences[KEEP_SCREEN_ON_ENABLED_KEY] ?: false,
                compassTrueNorthEnabled = preferences[COMPASS_TRUE_NORTH_ENABLED_KEY] ?: false,
                compassDeclination = preferences[COMPASS_DECLINATION_KEY] ?: 0f,
                tarotReversedEnabled = preferences[TAROT_REVERSED_ENABLED_KEY] ?: true,
                tarotMajorArcanaOnly = preferences[TAROT_MAJOR_ARCANA_ONLY_KEY] ?: false,
            )
            val next = transform(current)
            preferences[SCREEN_MODE_KEY] = next.screenMode.name
            preferences[CONTENT_SIZE_KEY] = next.contentSize.name
            preferences[ANIMATIONS_ENABLED_KEY] = next.animationsEnabled
            preferences[ROTARY_SCROLLING_ENABLED_KEY] = next.rotaryScrollingEnabled
            preferences[HAPTIC_FEEDBACK_ENABLED_KEY] = next.hapticFeedbackEnabled
            preferences[HAPTIC_INTENSITY_KEY] = next.hapticIntensity.name
            preferences[LANGUAGE_KEY] = next.language.name
            preferences[HOME_ORDER_KEY] = next.homeOrder.joinToString(",") { it.id }
            preferences[HIDDEN_HOME_FEATURES_KEY] = next.hiddenHomeFeatures.joinToString(",") { it.id }
            preferences[ONBOARDING_COMPLETED_KEY] = next.hasCompletedOnboarding
            if (next.userBirthDate != null) {
                preferences[USER_BIRTH_DATE_KEY] = next.userBirthDate
            } else {
                preferences.remove(USER_BIRTH_DATE_KEY)
            }
            if (next.userBirthHour != null) {
                preferences[USER_BIRTH_HOUR_KEY] = next.userBirthHour
            } else {
                preferences.remove(USER_BIRTH_HOUR_KEY)
            }
            preferences[USER_GENDER_KEY] = next.userGender.name
            preferences[KEEP_SCREEN_ON_ENABLED_KEY] = next.keepScreenOnEnabled
            preferences[COMPASS_TRUE_NORTH_ENABLED_KEY] = next.compassTrueNorthEnabled
            preferences[COMPASS_DECLINATION_KEY] = next.compassDeclination
            preferences[TAROT_REVERSED_ENABLED_KEY] = next.tarotReversedEnabled
            preferences[TAROT_MAJOR_ARCANA_ONLY_KEY] = next.tarotMajorArcanaOnly
        }
    }

    suspend fun setUserBirth(birthDate: String?, birthHour: Int?, gender: BaziGender) {
        update {
            it.copy(
                userBirthDate = birthDate,
                userBirthHour = birthHour,
                userGender = gender,
            )
        }
    }

    suspend fun clearUserBirth() {
        update {
            it.copy(
                userBirthDate = null,
                userBirthHour = null,
                userGender = BaziGender.MALE,
            )
        }
    }

    suspend fun setScreenMode(screenMode: ScreenMode) {
        update { it.copy(screenMode = screenMode) }
    }

    suspend fun setContentSize(contentSize: ContentSize) {
        update { it.copy(contentSize = contentSize) }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        update { it.copy(animationsEnabled = enabled) }
    }

    suspend fun setRotaryScrollingEnabled(enabled: Boolean) {
        update { it.copy(rotaryScrollingEnabled = enabled) }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        update { it.copy(hapticFeedbackEnabled = enabled) }
    }

    suspend fun setHapticIntensity(intensity: HapticIntensity) {
        update { it.copy(hapticIntensity = intensity) }
    }

    suspend fun setLanguage(language: AppLanguage) {
        update { it.copy(language = language) }
    }

    suspend fun setHomeOrder(order: List<HomeFeature>) {
        update { it.copy(homeOrder = order) }
    }

    suspend fun setHiddenHomeFeatures(hidden: Set<HomeFeature>) {
        update { it.copy(hiddenHomeFeatures = hidden) }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        update { it.copy(hasCompletedOnboarding = completed) }
    }

    suspend fun toggleHomeFeatureVisibility(feature: HomeFeature) {
        update {
            val hidden = it.hiddenHomeFeatures.toMutableSet()
            if (hidden.contains(feature)) {
                hidden.remove(feature)
            } else {
                hidden.add(feature)
            }
            it.copy(hiddenHomeFeatures = hidden)
        }
    }

    suspend fun moveHomeFeature(feature: HomeFeature, moveUp: Boolean) {
        update { current ->
            val list = current.effectiveHomeOrder().toMutableList()
            val index = list.indexOf(feature)
            if (index == -1) return@update current
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in list.indices) {
                val item = list.removeAt(index)
                list.add(targetIndex, item)
                current.copy(homeOrder = list)
            } else {
                current
            }
        }
    }

    suspend fun setKeepScreenOnEnabled(enabled: Boolean) {
        update { it.copy(keepScreenOnEnabled = enabled) }
    }

    suspend fun setCompassTrueNorthEnabled(enabled: Boolean) {
        update { it.copy(compassTrueNorthEnabled = enabled) }
    }

    suspend fun setCompassDeclination(declination: Float) {
        update { it.copy(compassDeclination = declination) }
    }

    suspend fun setTarotReversedEnabled(enabled: Boolean) {
        update { it.copy(tarotReversedEnabled = enabled) }
    }

    suspend fun setTarotMajorArcanaOnly(enabled: Boolean) {
        update { it.copy(tarotMajorArcanaOnly = enabled) }
    }

    suspend fun resetAllPreferences() {
        update { current ->
            AppSettings(
                userBirthDate = current.userBirthDate,
                userBirthHour = current.userBirthHour,
                userGender = current.userGender,
                hasCompletedOnboarding = current.hasCompletedOnboarding,
            )
        }
    }

    private companion object {
        val SCREEN_MODE_KEY = stringPreferencesKey("screen_mode")
        val CONTENT_SIZE_KEY = stringPreferencesKey("content_size")
        val ANIMATIONS_ENABLED_KEY = booleanPreferencesKey("animations_enabled")
        val ROTARY_SCROLLING_ENABLED_KEY = booleanPreferencesKey("rotary_scrolling_enabled")
        val HAPTIC_FEEDBACK_ENABLED_KEY = booleanPreferencesKey("haptic_feedback_enabled")
        val HAPTIC_INTENSITY_KEY = stringPreferencesKey("haptic_intensity")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val HOME_ORDER_KEY = stringPreferencesKey("home_order")
        val HIDDEN_HOME_FEATURES_KEY = stringPreferencesKey("hidden_home_features")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val USER_BIRTH_DATE_KEY = stringPreferencesKey("user_birth_date")
        val USER_BIRTH_HOUR_KEY = intPreferencesKey("user_birth_hour")
        val USER_GENDER_KEY = stringPreferencesKey("user_gender")
        val KEEP_SCREEN_ON_ENABLED_KEY = booleanPreferencesKey("keep_screen_on_enabled")
        val COMPASS_TRUE_NORTH_ENABLED_KEY = booleanPreferencesKey("compass_true_north_enabled")
        val COMPASS_DECLINATION_KEY = floatPreferencesKey("compass_declination")
        val TAROT_REVERSED_ENABLED_KEY = booleanPreferencesKey("tarot_reversed_enabled")
        val TAROT_MAJOR_ARCANA_ONLY_KEY = booleanPreferencesKey("tarot_major_arcana_only")

        inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
            runCatching { enumValueOf<T>(this.orEmpty()) }.getOrDefault(default)

        fun parseHomeOrder(stored: String?): List<HomeFeature> {
            if (stored.isNullOrBlank()) return HomeFeature.DEFAULT_ORDER
            val parsed = stored.split(",")
                .mapNotNull(String::trim)
                .mapNotNull(HomeFeature::fromId)
            val result = mutableListOf<HomeFeature>()
            val seen = mutableSetOf<HomeFeature>()
            for (f in parsed) {
                if (seen.add(f)) result.add(f)
            }
            for (f in HomeFeature.DEFAULT_ORDER) {
                if (seen.add(f)) result.add(f)
            }
            return result
        }

        fun parseHiddenFeatures(stored: String?): Set<HomeFeature> {
            if (stored.isNullOrBlank()) return emptySet()
            return stored.split(",")
                .mapNotNull(String::trim)
                .mapNotNull(HomeFeature::fromId)
                .toSet()
        }
    }
}
