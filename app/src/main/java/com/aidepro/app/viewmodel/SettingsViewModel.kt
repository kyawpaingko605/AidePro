package com.aidepro.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useDynamicColor: StateFlow<Boolean> = repository.useDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val aiApiKey: StateFlow<String> = repository.aiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val aiBaseUrl: StateFlow<String> = repository.aiBaseUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://api.openai.com/v1")

    val aiModel: StateFlow<String> = repository.aiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gpt-4o-mini")

    val editorFontSize: StateFlow<Int> = repository.editorFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14)

    val editorTabSize: StateFlow<Int> = repository.editorTabSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val wordWrap: StateFlow<Boolean> = repository.wordWrap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSave: StateFlow<Boolean> = repository.autoSave
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDarkTheme(value: Boolean) = viewModelScope.launch { repository.setDarkTheme(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { repository.setDynamicColor(value) }
    fun setAiApiKey(value: String) = viewModelScope.launch { repository.setAiApiKey(value) }
    fun setAiBaseUrl(value: String) = viewModelScope.launch { repository.setAiBaseUrl(value) }
    fun setAiModel(value: String) = viewModelScope.launch { repository.setAiModel(value) }
    fun setEditorFontSize(value: Int) = viewModelScope.launch { repository.setEditorFontSize(value) }
    fun setEditorTabSize(value: Int) = viewModelScope.launch { repository.setEditorTabSize(value) }
    fun setWordWrap(value: Boolean) = viewModelScope.launch { repository.setWordWrap(value) }
    fun setAutoSave(value: Boolean) = viewModelScope.launch { repository.setAutoSave(value) }
}
