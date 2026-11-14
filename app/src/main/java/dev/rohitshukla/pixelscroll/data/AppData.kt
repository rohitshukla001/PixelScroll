package dev.rohitshukla.pixelscroll.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** One settings repository per process, shared by the screens and the accessibility service. */
object AppData {
    @Volatile private var settingsRepo: SettingsRepository? = null

    fun settings(context: Context): SettingsRepository =
        settingsRepo ?: synchronized(this) {
            settingsRepo ?: SettingsRepository(context.applicationContext.settingsStore).also { settingsRepo = it }
        }
}
