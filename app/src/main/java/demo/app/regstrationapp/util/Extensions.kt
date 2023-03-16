package demo.app.regstrationapp.util

import android.content.SharedPreferences

fun SharedPreferences.put(key: String, value: String?) {
    edit().putString(key, value).apply()
}