package com.argon.ph03n1x.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.argon.ph03n1x.data.ArgonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean
)

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Success(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
    object ApiKeyRequired : ChatUiState()
}

class ChatViewModel(
    private val repository: ArgonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _quotaBadge = MutableStateFlow(repository.getQuotaBadge())
    val quotaBadge: StateFlow<String> = _quotaBadge.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    init {
        if (repository.isApiKeyRequired()) {
            _uiState.value = ChatUiState.ApiKeyRequired
            _showApiKeyDialog.value = true
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        if (repository.isApiKeyRequired()) {
            _uiState.value = ChatUiState.ApiKeyRequired
            _showApiKeyDialog.value = true
            return
        }

        val userMsg = ChatMessage(text = text.trim(), isUser = true)
        _messages.value = _messages.value + userMsg
        _uiState.value = ChatUiState.Loading

        viewModelScope.launch {
            when (val result = repository.sendMessage(text.trim())) {
                is ArgonRepository.Result.Success -> {
                    val botMsg = ChatMessage(text = result.reply, isUser = false)
                    _messages.value = _messages.value + botMsg
                    _quotaBadge.value = repository.getQuotaBadge()
                    _uiState.value = ChatUiState.Success(_messages.value)
                }
                is ArgonRepository.Result.Error -> {
                    _uiState.value = ChatUiState.Error(result.message)
                }
                ArgonRepository.Result.ApiKeyRequired -> {
                    _uiState.value = ChatUiState.ApiKeyRequired
                    _showApiKeyDialog.value = true
                    _quotaBadge.value = repository.getQuotaBadge()
                }
            }
        }
    }

    fun submitApiKey(key: String) {
        if (key.isBlank()) return
        repository.saveApiKey(key)
        _showApiKeyDialog.value = false
        _quotaBadge.value = repository.getQuotaBadge()
        _uiState.value = ChatUiState.Idle
    }

    fun dismissApiKeyDialog() {
        if (repository.hasUserKey()) {
            _showApiKeyDialog.value = false
        }
    }

    fun forceShowApiKeyDialog() {
        _showApiKeyDialog.value = true
    }
}

class ChatViewModelFactory(
    private val repository: ArgonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}