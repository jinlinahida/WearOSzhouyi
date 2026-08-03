package com.boompala.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
            )
        }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val current = AppSettings(
                screenMode = preferences[SCREEN_MODE_KEY].toEnumOrDefault(ScreenMode.AUTO),
                contentSize = preferences[CONTENT_SIZE_KEY].toEnumOrDefault(ContentSize.STANDARD),
                animationsEnabled = preferences[ANIMATIONS_ENABLED_KEY] ?: true,
                rotaryScrollingEnabled = preferences[ROTARY_SCROLLING_ENABLED_KEY] ?: true,
            )
            val next = transform(current)
            preferences[SCREEN_MODE_KEY] = next.screenMode.name
            preferences[CONTENT_SIZE_KEY] = next.contentSize.name
            preferences[ANIMATIONS_ENABLED_KEY] = next.animationsEnabled
            preferences[ROTARY_SCROLLING_ENABLED_KEY] = next.rotaryScrollingEnabled
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

    private companion object {
        val SCREEN_MODE_KEY = stringPreferencesKey("screen_mode")
        val CONTENT_SIZE_KEY = stringPreferencesKey("content_size")
        val ANIMATIONS_ENABLED_KEY = booleanPreferencesKey("animations_enabled")
        val ROTARY_SCROLLING_ENABLED_KEY = booleanPreferencesKey("rotary_scrolling_enabled")

        inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
            runCatching { enumValueOf<T>(this.orEmpty()) }.getOrDefault(default)
    }
}
