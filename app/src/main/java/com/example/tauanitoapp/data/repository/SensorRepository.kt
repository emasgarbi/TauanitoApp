package com.example.tauanitoapp.data.repository

import com.example.tauanitoapp.data.model.BatteryLevel
import com.example.tauanitoapp.data.model.Device
import com.example.tauanitoapp.data.model.SensorReading
import com.example.tauanitoapp.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class SensorRepository {

    private val webClient = NetworkModule.webClient

    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
        val csrfToken = webClient.getCsrfToken()
        val ok = webClient.submitLogin(email, password, csrfToken)
        if (!ok) throw Exception("Email o password non corretti")
    }

    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        val html = webClient.fetchHomeHtml()
        parseHomeHtml(html)
    }

    private fun parseHomeHtml(html: String): List<Device> {
        val doc = Jsoup.parse(html)
        val devices = mutableListOf<Device>()

        for (card in doc.select(".device-card")) {
            // Nome device: <a class="h4"> + ID estratto dall'href /device/{id}/dati
            val deviceLink = card.select("a.h4").firstOrNull() ?: continue
            val deviceName = deviceLink.text().trim().takeIf { it.isNotBlank() } ?: continue
            val deviceUrl  = deviceLink.attr("href")
            val deviceId   = Regex("/device/([^/]+)/").find(deviceUrl)
                ?.groupValues?.getOrNull(1)?.trim()
                ?: deviceName

            // Timestamp: primo <strong> nel card
            val timestamp = card.select("strong").firstOrNull()?.text()?.trim()

            // Batteria: span con classe "battery" + sottoclasse "full/half/low/empty"
            val batterySpan = card.select("span.battery").firstOrNull()
            val batteryLevel = when {
                batterySpan?.hasClass("full")  == true -> BatteryLevel.FULL
                batterySpan?.hasClass("half")  == true -> BatteryLevel.HALF
                batterySpan?.hasClass("low")   == true -> BatteryLevel.LOW
                batterySpan?.hasClass("empty") == true -> BatteryLevel.EMPTY
                else -> BatteryLevel.UNKNOWN
            }

            // Voltaggio: <strong> nel parent di span.battery (es. "3,56 V")
            val voltage = batterySpan
                ?.parent()
                ?.select("strong")
                ?.firstOrNull()
                ?.text()
                ?.replace('\u00A0', ' ')
                ?.trim()

            // Cliente: testo dentro <i data-toggle="tooltip"> nel formato "Modello  - Cliente"
            val modelCustomerText = card
                .select("p i[data-toggle=tooltip]").firstOrNull()?.text()?.trim()
            val customer = modelCustomerText
                ?.substringAfterLast(" - ")?.trim()?.takeIf { it.isNotBlank() }

            // Letture sensori dal div deviceDetails{id}
            val detailsDiv = card.select("[id^=deviceDetails]").firstOrNull() ?: continue
            val readings = mutableListOf<SensorReading>()

            for (row in detailsDiv.select("div.row")) {
                val nameCol  = row.select(".col").firstOrNull() ?: continue
                val valueCol = row.select(".col-auto").firstOrNull() ?: continue

                val label     = nameCol.text().trim()
                val rawValue  = valueCol.select("strong").firstOrNull()?.text() ?: continue
                val cleanValue = rawValue.replace('\u00A0', ' ').trim()

                if (label.isBlank() || cleanValue.isBlank()) continue

                val (value, unit) = splitValueAndUnit(cleanValue)
                readings.add(SensorReading(name = label, value = value, unit = unit))
            }

            devices.add(
                Device(
                    id           = deviceId,
                    name         = deviceName,
                    customer     = customer,
                    timestamp    = timestamp,
                    voltage      = voltage,
                    batteryLevel = batteryLevel,
                    readings     = readings
                )
            )
        }

        return devices
    }

    private fun splitValueAndUnit(text: String): Pair<String, String?> {
        val trimmed = text.trim()
        return when {
            trimmed.endsWith("%") -> Pair(trimmed.dropLast(1).trim(), "%")
            ' ' in trimmed -> {
                val idx  = trimmed.lastIndexOf(' ')
                val unit = trimmed.substring(idx + 1).trim()
                Pair(trimmed.substring(0, idx).trim(), unit.ifBlank { null })
            }
            else -> Pair(trimmed, null)
        }
    }
}
