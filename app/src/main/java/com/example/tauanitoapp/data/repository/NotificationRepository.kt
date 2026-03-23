package com.example.tauanitoapp.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val aiAdvice: String? = null, // Suggerimento intelligente dell'IA
    val timestamp: String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
    val isRead: Boolean = false
)

object NotificationRepository {
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    fun addNotification(title: String, body: String) {
        val aiAdvice = generateAiAdvice(title, body)
        val newList = _notifications.value.toMutableList()
        newList.add(0, NotificationItem(title = title, body = body, aiAdvice = aiAdvice))
        _notifications.value = newList
    }

    private fun generateAiAdvice(title: String, body: String): String? {
        val content = (title + " " + body).lowercase()
        return when {
            content.contains("batteria") || content.contains("scarica") -> 
                "L'IA consiglia di sostituire la batteria entro 24 ore per evitare la perdita di dati storici."
            content.contains("co2") || content.contains("anidride") -> 
                "Livelli di CO2 elevati rilevati. L'IA suggerisce di aprire le finestre per almeno 5-10 minuti."
            content.contains("iaq") || content.contains("qualità") -> 
                "Qualità dell'aria in peggioramento. L'IA consiglia di attivare il purificatore o ventilare il locale."
            content.contains("temp") || content.contains("caldo") || content.contains("freddo") -> 
                "Variazione termica anomala. L'IA suggerisce di controllare l'impianto di climatizzazione."
            content.contains("umidità") || content.contains("umido") -> 
                "Umidità fuori range. L'IA consiglia di deumidificare per prevenire la formazione di muffe."
            else -> "L'IA sta monitorando la situazione. Non sono richiesti interventi immediati."
        }
    }

    fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    val unreadCount: Int
        get() = _notifications.value.count { !it.isRead }
}
