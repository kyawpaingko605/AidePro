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
class HomeViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val recentProjects: StateFlow<List<ProjectInfo>> = repository.recentProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeProject(path: String) {
        viewModelScope.launch {
            repository.removeRecentProject(path)
        }
    }

    fun addProject(project: ProjectInfo) {
        viewModelScope.launch {
            repository.addRecentProject(project)
        }
    }
}
