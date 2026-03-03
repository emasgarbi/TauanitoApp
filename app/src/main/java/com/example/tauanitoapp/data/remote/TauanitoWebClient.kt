package com.example.tauanitoapp.data.remote

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class TauanitoWebClient(private val client: OkHttpClient) {

    private val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    fun getCsrfToken(): String {
        val request = Request.Builder()
            .url("${NetworkModule.BASE_URL}/login")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
            
        val html = client.newCall(request).execute().use { resp ->
            resp.body?.string() ?: throw Exception("Errore connessione")
        }
        val doc = Jsoup.parse(html)
        return doc.select("input[name=_token]").firstOrNull()?.attr("value")
            ?: doc.select("meta[name=csrf-token]").firstOrNull()?.attr("content")
            ?: throw Exception("Token non trovato")
    }

    fun submitLogin(email: String, password: String, csrfToken: String): Boolean {
        val body = FormBody.Builder()
            .add("_token", csrfToken)
            .add("email", email)
            .add("password", password)
            .build()
        
        val request = Request.Builder()
            .url("${NetworkModule.BASE_URL}/loginA")
            .header("User-Agent", USER_AGENT)
            .header("Referer", "${NetworkModule.BASE_URL}/login")
            .post(body)
            .build()
            
        return client.newCall(request).execute().use { resp ->
            val finalUrl = resp.request.url.toString()
            // Login ok se siamo su home o dashboard
            finalUrl.contains("home") || finalUrl.contains("dashboard") || resp.isSuccessful
        }
    }

    fun fetchHomeHtml(): String {
        return fetchUrl("${NetworkModule.BASE_URL}/home")
    }

    fun fetchDeviceHtml(deviceId: String): String {
        return fetchUrl("${NetworkModule.BASE_URL}/device/$deviceId/dati")
    }

    fun downloadCsv(deviceId: String): ByteArray {
        val request = Request.Builder()
            .url("${NetworkModule.BASE_URL}/esportaUltimi_ajax.php?d=$deviceId&devtemp=1&devum=1&devpress=1&devco2=1&iaq=1&modello=17")
            .header("User-Agent", USER_AGENT)
            .header("Referer", "${NetworkModule.BASE_URL}/device/$deviceId/dati")
            .get()
            .build()
        
        return client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("Download fallito")
            resp.body?.bytes() ?: throw Exception("File vuoto")
        }
    }

    private fun fetchUrl(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
            
        return client.newCall(request).execute().use { resp ->
            val finalUrl = resp.request.url.toString()
            if (finalUrl.contains("/login") && !url.contains("/login")) {
                throw Exception("Sessione scaduta")
            }
            if (!resp.isSuccessful) throw Exception("Errore server (${resp.code})")
            resp.body?.string() ?: ""
        }
    }
}
