package demo.app.regstrationapp.util

import android.content.SharedPreferences

class PrefsHelper(
    private val sharedPreferences: SharedPreferences
) {
    var mapType: String?
        get() = sharedPreferences.getString(Keys.MAP_TYPES, null)
        set(value) = sharedPreferences.put(Keys.MAP_TYPES, value)
}