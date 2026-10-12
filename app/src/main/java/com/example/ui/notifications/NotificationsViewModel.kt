package com.example.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ClientNotification
import com.example.data.remote.SupabaseClient
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<ClientNotification> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val everHadNotifications: Boolean = false
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

class NotificationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    private var loadedForUserId = ""
    /** IDs dispensados por swipe: filtro local que sobrevive aos refreshes (sem endpoint de delete). */
    private val dismissedIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            UserSessionRepository.userSession.collect { session ->
                if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
                    loadedForUserId = ""
                    _uiState.value = NotificationsUiState(notifications = emptyList(), isLoading = false)
                } else if (loadedForUserId != session.userId) {
                    loadedForUserId = session.userId
                    loadNotifications()
                }
            }
        }
    }

    fun loadNotifications() {
        val session = UserSessionRepository.userSession.value
        if (!session.isLoggedIn || session.accessToken.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val fetched = SupabaseClient.fetchClientNotifications(session.accessToken)
                    .filter { it.id !in dismissedIds }
                _uiState.value = NotificationsUiState(
                    notifications = fetched,
                    isLoading = false,
                    everHadNotifications = _uiState.value.everHadNotifications || fetched.isNotEmpty()
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Não foi possível carregar as notificações."
                )
            }
        }
    }

    fun openNotification(notification: ClientNotification, onOpenOrder: () -> Unit) {
        if (notification.isRead) {
            onOpenOrder()
            return
        }
        val session = UserSessionRepository.userSession.value
        _uiState.value = _uiState.value.copy(
            notifications = _uiState.value.notifications.map {
                if (it.id == notification.id) it.copy(readAt = "pending") else it
            }
        )
        viewModelScope.launch {
            val marked = SupabaseClient.markClientNotificationRead(notification.id, session.accessToken)
            if (!marked) loadNotifications()
        }
        onOpenOrder()
    }

    /** Marca todas as não-lidas como lidas (otimista; recarrega se alguma falhar). */
    fun markAllAsRead() {
        val session = UserSessionRepository.userSession.value
        val unread = _uiState.value.notifications.filter { !it.isRead }
        if (unread.isEmpty() || session.accessToken.isBlank()) return
        _uiState.value = _uiState.value.copy(
            notifications = _uiState.value.notifications.map {
                if (!it.isRead) it.copy(readAt = "pending") else it
            }
        )
        viewModelScope.launch {
            var allOk = true
            for (notification in unread) {
                if (!SupabaseClient.markClientNotificationRead(notification.id, session.accessToken)) {
                    allOk = false
                }
            }
            if (!allOk) loadNotifications()
        }
    }

    /** Dispensa uma notificação localmente (sem endpoint de delete no backend). */
    fun dismissNotification(notificationId: String) {
        if (notificationId.isBlank()) return
        dismissedIds.add(notificationId)
        _uiState.value = _uiState.value.copy(
            notifications = _uiState.value.notifications.filter { it.id != notificationId }
        )
    }
}
