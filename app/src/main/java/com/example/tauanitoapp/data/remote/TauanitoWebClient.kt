package com.example.tauanitoapp.data.remote

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class TauanitoWebClient(private val client: OkHttpClient) {

    /**
     * GET /login → estrae il CSRF token (_token) dal form HTML.
     */
    fun getCsrfToken(): String {
        val request = Request.Builder()
            .url("${NetworkModule.BASE_URL}/login")
            .get()
            .build()
        val html = client.newCall(request).execute().use { resp ->
            resp.body?.string() ?: throw Exception("Pagina di login vuota")
        }
        val doc = Jsoup.parse(html)
        return doc.select("input[name=_token]").firstOrNull()?.attr("value")
            ?: doc.select("meta[name=csrf-token]").firstOrNull()?.attr("content")
            ?: throw Exception("CSRF token non trovato nella pagina di login")
    }

    /**
     * POST /loginA con form-encoded. Ritorna true se il redirect va a /home (login riuscito).
     */
    fun submitLogin(email: String, password: String, csrfToken: String): Boolean {
        val body = FormBody.Builder()
            .add("_token", csrfToken)
            .add("email", email)
            .add("password", password)
            .build()
        val noRedirectClient = client.newBuilder().followRedirects(false).build()
        val request = Request.Builder()
            .url("${NetworkModule.BASE_URL}/loginA")
            .post(body)
            .build()
        noRedirectClient.newCall(request).execute().use { resp ->
            val location = resp.header("Location") ?: ""
            return location.contains("home")
        }
    }

    /**
     * GET /home → ritorna l'HTML della dashboard (richiede sessione attiva).
     */
    fun fetchHomeHtml(): String {
        val request = Request.Builder()
            .url("${NetworkModule.BASE_URL}/home")
            .get()
            .build()
        return client.newCall(request).execute().use { resp ->
            // OkHttp segue i redirect: se l'URL finale è /login la sessione è scaduta
            if (resp.request.url.toString().contains("/login")) {
                throw Exception("Sessione scaduta, effettua di nuovo il login")
            }
            if (!resp.isSuccessful)
                throw Exception("Errore server: ${resp.code}")
            val html = resp.body?.string() ?: throw Exception("Risposta vuota dal server")
            // Ulteriore controllo: se l'HTML contiene il form di login siamo stati reindirizzati
            if (html.contains("loginA") && !html.contains("Elenco device", ignoreCase = true)) {
                throw Exception("Sessione scaduta, effettua di nuovo il login")
            }
            html
        }
    }
}
