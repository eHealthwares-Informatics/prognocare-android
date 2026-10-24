package com.ehealthwares.prognocare.designsystem.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

/**
 * Theme preferences manager for persisting theme settings
 */
class ThemePreferences(private val context: Context) {
    
    private object Keys {
        val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val CUSTOM_PRIMARY_COLOR = longPreferencesKey("custom_primary_color")
        val CUSTOM_SECONDARY_COLOR = longPreferencesKey("custom_secondary_color")
        val CUSTOM_ACCENT_COLOR = longPreferencesKey("custom_accent_color")
    }
    
    val themeSettings: Flow<ThemeSettings> = context.themeDataStore.data.map { preferences ->
        ThemeSettings(
            appearanceMode = try {
                AppearanceMode.valueOf(
                    preferences[Keys.APPEARANCE_MODE] ?: AppearanceMode.SYSTEM.name
                )
            } catch (e: Exception) {
                AppearanceMode.SYSTEM
            },
            colorTheme = try {
                ColorTheme.valueOf(
                    preferences[Keys.COLOR_THEME] ?: ColorTheme.EHEALTHWARES.name
                )
            } catch (e: Exception) {
                ColorTheme.EHEALTHWARES
            },
            customPrimaryColor = preferences[Keys.CUSTOM_PRIMARY_COLOR] ?: 0xFF2563EB,
            customSecondaryColor = preferences[Keys.CUSTOM_SECONDARY_COLOR],
            customAccentColor = preferences[Keys.CUSTOM_ACCENT_COLOR]
        )
    }
    
    suspend fun updateAppearanceMode(mode: AppearanceMode) {
        context.themeDataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_MODE] = mode.name
        }
    }
    
    suspend fun updateColorTheme(theme: ColorTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[Keys.COLOR_THEME] = theme.name
        }
    }
    
    suspend fun updateCustomPrimaryColor(color: Long) {
        context.themeDataStore.edit { preferences ->
            preferences[Keys.CUSTOM_PRIMARY_COLOR] = color
            preferences[Keys.COLOR_THEME] = ColorTheme.CUSTOM.name
        }
    }
    
    suspend fun updateCustomSecondaryColor(color: Long) {
        context.themeDataStore.edit { preferences ->
            preferences[Keys.CUSTOM_SECONDARY_COLOR] = color
            preferences[Keys.COLOR_THEME] = ColorTheme.CUSTOM.name
        }
    }
    
    suspend fun updateCustomAccentColor(color: Long) {
        context.themeDataStore.edit { preferences ->
            preferences[Keys.CUSTOM_ACCENT_COLOR] = color
            preferences[Keys.COLOR_THEME] = ColorTheme.CUSTOM.name
        }
    }
    
    suspend fun resetToDefaults() {
        context.themeDataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_MODE] = AppearanceMode.SYSTEM.name
            preferences[Keys.COLOR_THEME] = ColorTheme.EHEALTHWARES.name
            preferences[Keys.CUSTOM_PRIMARY_COLOR] = 0xFF2563EB
            preferences.remove(Keys.CUSTOM_SECONDARY_COLOR)
            preferences.remove(Keys.CUSTOM_ACCENT_COLOR)
        }
    }
}
