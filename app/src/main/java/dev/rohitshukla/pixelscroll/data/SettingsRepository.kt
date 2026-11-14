package dev.rohitshukla.pixelscroll.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Reads and writes [Settings]. The app screens and the accessibility service share one instance. */
class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<Settings> = store.data.map(::toSettings).distinctUntilChanged()

    suspend fun setEnabled(on: Boolean) {
        store.edit { it[ENABLED] = on }
    }

    suspend fun setFallbackDelay(seconds: Int) {
        store.edit { it[DELAY] = Settings.clampDelay(seconds) }
    }

    private fun toSettings(p: Preferences) = Settings(
        enabled = p[ENABLED] ?: true,
        fallbackDelaySec = p[DELAY] ?: Settings.DEFAULT_DELAY_SEC,
    )

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val DELAY = intPreferencesKey("fallback_delay_sec")
    }
}
