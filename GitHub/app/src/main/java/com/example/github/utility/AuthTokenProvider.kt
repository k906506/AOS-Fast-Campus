package com.example.github.utility

import android.content.Context
import androidx.preference.PreferenceManager

class AuthTokenProvider(private val context: Context) {
    fun setToken(token: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(Key.KEY_AUTH_TOKEN, token)
            .apply()
    }

    val getToken: String?
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Key.KEY_AUTH_TOKEN, null)
}