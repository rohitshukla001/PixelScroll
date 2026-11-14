package dev.rohitshukla.pixelscroll.data

import android.content.Context

/** Remembers whether first-run setup (Welcome, then Accessibility access) is finished. */
class SetupState(context: Context) {
    private val prefs = context.getSharedPreferences("setup", Context.MODE_PRIVATE)

    var isComplete: Boolean
        get() = prefs.getBoolean(KEY_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLETE, value).apply()

    private companion object {
        const val KEY_COMPLETE = "complete"
    }
}
