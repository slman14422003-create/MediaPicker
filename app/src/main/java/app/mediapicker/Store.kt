package app.mediapicker

import android.content.Context
import android.content.SharedPreferences

object Store {
    private fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("mp", Context.MODE_PRIVATE)

    fun themeMode(c: Context): String = sp(c).getString("theme_mode", Appearance.AUTO) ?: Appearance.AUTO

    fun setThemeMode(c: Context, v: String) {
        sp(c).edit().putString("theme_mode", v).apply()
    }

    fun bool(c: Context, key: String, def: Boolean): Boolean = sp(c).getBoolean("b_$key", def)

    fun setBool(c: Context, key: String, v: Boolean) {
        sp(c).edit().putBoolean("b_$key", v).apply()
    }

    fun str(c: Context, key: String, def: String): String = sp(c).getString("s_$key", def) ?: def

    fun setStr(c: Context, key: String, v: String) {
        sp(c).edit().putString("s_$key", v).apply()
    }

    fun int(c: Context, key: String, def: Int): Int = sp(c).getInt("i_$key", def)

    fun setInt(c: Context, key: String, v: Int) {
        sp(c).edit().putInt("i_$key", v).apply()
    }

    /** Package of the app linked to a file category, or null to ask every time. */
    fun linked(c: Context, cat: String): String? = sp(c).getString("link_$cat", null)

    fun setLinked(c: Context, cat: String, pkg: String?) {
        val e = sp(c).edit()
        if (pkg == null) e.remove("link_$cat") else e.putString("link_$cat", pkg)
        e.apply()
    }
}
