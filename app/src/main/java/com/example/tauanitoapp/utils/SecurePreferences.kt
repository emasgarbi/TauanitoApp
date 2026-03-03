package com.example.tauanitoapp.utils

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

object SecurePreferences {
    private const val PREFS_NAME = "tauanito_prefs_v9"
    private const val KEY_EMAIL = "saved_email"
    private const val KEY_PASSWORD = "saved_password"
    private const val KEY_USE_BIOMETRICS = "use_biometrics"
    private const val KEY_HAS_LOGGED_IN_ONCE = "has_logged_in_once"
    private const val KEY_COOKIES = "saved_cookies"

    /** 
     * Usiamo SharedPreferences standard per massima compatibilità con Pixel 9.
     * I dati rimangono comunque privati all'interno dell'app.
     */
    private fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveCredentials(context: Context, email: String, password: String) {
        getSharedPrefs(context).edit().apply {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
            putBoolean(KEY_HAS_LOGGED_IN_ONCE, true)
            apply()
        }
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean(KEY_USE_BIOMETRICS, enabled).apply()
    }

    fun getSavedEmail(context: Context): String? = getSharedPrefs(context).getString(KEY_EMAIL, null)
    fun getSavedPassword(context: Context): String? = getSharedPrefs(context).getString(KEY_PASSWORD, null)
    
    fun isBiometricEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_USE_BIOMETRICS, false)
    }

    fun hasLoggedInOnce(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_HAS_LOGGED_IN_ONCE, false)
    }

    fun saveCookies(context: Context, cookies: List<Cookie>) {
        if (isBiometricEnabled(context)) {
            val cookieStrings = cookies.map { it.toString() }.toSet()
            getSharedPrefs(context).edit().putStringSet(KEY_COOKIES, cookieStrings).apply()
        }
    }

    fun getSavedCookies(context: Context): List<Cookie> {
        if (!isBiometricEnabled(context)) return emptyList()
        val cookieStrings = getSharedPrefs(context).getStringSet(KEY_COOKIES, emptySet()) ?: emptySet()
        val baseUrl = "https://www.tauanito.it".toHttpUrl()
        return cookieStrings.mapNotNull { Cookie.parse(baseUrl, it) }
    }

    fun clearCredentials(context: Context) {
        getSharedPrefs(context).edit().clear().apply()
    }
}
