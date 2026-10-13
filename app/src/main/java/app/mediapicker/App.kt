package app.mediapicker

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Appearance.apply(this)
    }
}

object Appearance {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val AUTO = "auto"

    fun apply(c: Context) {
        val mode = when (Store.themeMode(c)) {
            DARK -> AppCompatDelegate.MODE_NIGHT_YES
            LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun name(mode: String): Int = when (mode) {
        DARK -> R.string.theme_dark
        LIGHT -> R.string.theme_light
        else -> R.string.theme_auto
    }

    fun isNight(c: Context): Boolean =
        (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
