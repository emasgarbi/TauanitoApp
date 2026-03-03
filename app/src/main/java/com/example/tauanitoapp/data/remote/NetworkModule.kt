package com.example.tauanitoapp.data.remote

import android.content.Context
import com.example.tauanitoapp.utils.SecurePreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class PersistentCookieJar(private val context: Context) : CookieJar {
    private val inMemoryCache = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (url.host == "www.tauanito.it") {
            inMemoryCache.clear()
            inMemoryCache.addAll(cookies)
            SecurePreferences.saveCookies(context, cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (url.host != "www.tauanito.it") return emptyList()
        
        // Se abbiamo cookie freschi in memoria, usiamo quelli
        if (inMemoryCache.isNotEmpty()) return inMemoryCache
        
        // Altrimenti carichiamo quelli salvati (se biometria attiva)
        return SecurePreferences.getSavedCookies(context)
    }
}

object NetworkModule {
    const val BASE_URL = "https://www.tauanito.it"
    private var client: OkHttpClient? = null

    fun getOkHttpClient(context: Context): OkHttpClient {
        if (client == null) {
            client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .cookieJar(PersistentCookieJar(context))
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true) // Cruciale per stabilità login
                .followSslRedirects(true)
                .build()
        }
        return client!!
    }

    fun getWebClient(context: Context): TauanitoWebClient {
        return TauanitoWebClient(getOkHttpClient(context))
    }
}
