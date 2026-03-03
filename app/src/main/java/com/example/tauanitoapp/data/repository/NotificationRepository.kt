package com.example.tauanitoapp.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val timestamp: String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
    val isRead: Boolean = false
)

object NotificationRepository {
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    fun addNotification(title: String, body: String) {
        val newList = _notifications.value.toMutableList()
        newList.add(0, NotificationItem(title = title, body = body))
        _notifications.value = newList
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
