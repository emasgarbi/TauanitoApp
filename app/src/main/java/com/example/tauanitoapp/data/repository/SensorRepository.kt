package com.example.tauanitoapp.data.repository

import android.content.Context
import android.util.Log
import com.example.tauanitoapp.data.model.BatteryLevel
import com.example.tauanitoapp.data.model.Device
import com.example.tauanitoapp.data.model.SensorReading
import com.example.tauanitoapp.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class SensorRepository(context: Context) {

    private val webClient = NetworkModule.getWebClient(context)

    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
        val csrfToken = webClient.getCsrfToken()
        val ok = webClient.submitLogin(email, password, csrfToken)
        if (!ok) throw Exception("Email o password errati")
    }

    /** Verifica se la sessione salvata è ancora valida caricando la home */
    suspend fun isSessionValid(): Boolean = withContext(Dispatchers.IO) {
        try {
            val html = webClient.fetchHomeHtml()
            html.contains("Elenco device", ignoreCase = true) || html.contains("Dashboard")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        val html = webClient.fetchHomeHtml()
        if (html.isBlank()) return@withContext emptyList<Device>()
        parseHomeHtml(html)
    }

    suspend fun getDeviceHistory(deviceId: String): com.example.tauanitoapp.data.model.DeviceHistory = withContext(Dispatchers.IO) {
        val html = webClient.fetchDeviceHtml(deviceId)
        parseHistoryHtml(deviceId, html)
    }

    suspend fun downloadDeviceCsv(deviceId: String): ByteArray = withContext(Dispatchers.IO) {
        webClient.downloadCsv(deviceId)
    }

    private fun parseHistoryHtml(deviceId: String, html: String): com.example.tauanitoapp.data.model.DeviceHistory {
        val doc = Jsoup.parse(html)
        val deviceName = doc.select("h1, h2, h3, .h1, .h2, .h3").firstOrNull()?.text()?.trim() ?: "Device $deviceId"
        val records = mutableListOf<com.example.tauanitoapp.data.model.HistoryRecord>()

        val table = doc.select("table").firstOrNull()
        if (table != null) {
            val headers = table.select("thead th").map { it.text().trim() }
            val rows = table.select("tbody tr")
            for (row in rows) {
                val cols = row.select("td")
                if (cols.isNotEmpty()) {
                    val timestamp = cols.firstOrNull()?.text()?.trim() ?: ""
                    val readings = mutableListOf<SensorReading>()
                    for (i in 1 until cols.size) {
                        val header = headers.getOrNull(i) ?: "Sensore $i"
                        val (value, unit) = splitValueAndUnit(cols[i].text().trim())
                        readings.add(SensorReading(header, value, unit))
                    }
                    records.add(com.example.tauanitoapp.data.model.HistoryRecord(timestamp, readings))
                }
            }
        }
        return com.example.tauanitoapp.data.model.DeviceHistory(deviceId, deviceName, records)
    }

    private fun parseHomeHtml(html: String): List<Device> {
        val doc = Jsoup.parse(html)
        val devices = mutableListOf<Device>()

        // Selettore preciso: solo .device-card evita di catturare elementi annidati o generici
        var cards = doc.select(".device-card")
        // Fallback: se il sito non usa .device-card, cerca card con link a /device/
        if (cards.isEmpty()) {
            cards = doc.select(".card, .box").filter { card ->
                card.select("a[href*=/device/]").isNotEmpty()
            }.let { org.jsoup.select.Elements(it) }
        }
        Log.d("TauanitoRepo", "Device cards trovate: ${cards.size}")

        for (card in cards) {
            try {
                // Estrazione link dispositivo con più fallback
                val deviceLink = card.select("a.h4").firstOrNull()
                    ?: card.select(".h4 a").firstOrNull()
                    ?: card.select("h4 a").firstOrNull()
                    ?: card.select("a[href*=/device/]").firstOrNull()
                    ?: continue
                val deviceName = deviceLink.text().trim()
                val deviceUrl  = deviceLink.attr("href")
                val deviceId   = Regex("/device/([^/]+)/").find(deviceUrl)?.groupValues?.getOrNull(1) ?: deviceName

                val timestamp = card.select("strong").firstOrNull()?.text()?.trim()
                val batterySpan = card.select("span.battery").firstOrNull()
                val voltage = batterySpan?.parent()?.select("strong")?.firstOrNull()?.text()?.replace('\u00A0', ' ')?.trim()

                val voltageValue = voltage?.replace(",", ".")?.substringBefore(" ")?.toDoubleOrNull()
                val batteryLevel = when {
                    batterySpan?.hasClass("empty") == true || (voltageValue != null && voltageValue < 3.35) -> BatteryLevel.EMPTY
                    batterySpan?.hasClass("low")   == true || (voltageValue != null && voltageValue < 3.50) -> BatteryLevel.LOW
                    batterySpan?.hasClass("half")  == true -> BatteryLevel.HALF
                    batterySpan?.hasClass("full")  == true -> BatteryLevel.FULL
                    else -> BatteryLevel.UNKNOWN
                }

                val modelCustomerText = card.select("p i[data-toggle=tooltip]").firstOrNull()?.text()?.trim()
                val customer = modelCustomerText?.substringAfterLast(" - ")?.trim()

                val detailsDiv = card.select("[id^=deviceDetails]").firstOrNull()
                val readings = mutableListOf<SensorReading>()
                if (detailsDiv != null) {
                    for (row in detailsDiv.select("div.row")) {
                        val label = row.select(".col").firstOrNull()?.text()?.trim() ?: continue
                        val rawValue = row.select(".col-auto strong").firstOrNull()?.text()?.trim() ?: continue
                        val (value, unit) = splitValueAndUnit(rawValue)
                        readings.add(SensorReading(label, value, unit))
                    }
                }

                devices.add(Device(deviceId, deviceName, customer, timestamp, voltage, batteryLevel, readings, null, null))
            } catch (e: Exception) {
                Log.e("TauanitoRepo", "Errore parsing card: ${e.message}")
            }
        }
        Log.d("TauanitoRepo", "Device parsati: ${devices.size}")
        return devices
    }

    private fun splitValueAndUnit(text: String): Pair<String, String?> {
        val trimmed = text.trim()
        return when {
            trimmed.endsWith("%") -> Pair(trimmed.dropLast(1).trim(), "%")
            ' ' in trimmed -> {
                val idx = trimmed.lastIndexOf(' ')
                Pair(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim().ifBlank { null })
            }
            else -> Pair(trimmed, null)
        }
    }
}
