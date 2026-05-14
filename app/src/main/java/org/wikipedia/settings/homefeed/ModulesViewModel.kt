package org.wikipedia.settings.homefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wikipedia.settings.SettingsRepository

class ModulesViewModel : ViewModel() {
    val hiddenModules: StateFlow<Set<String>?> = SettingsRepository.hiddenModules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun toggleModuleVisibility(moduleKey: String, isVisible: Boolean) {
        viewModelScope.launch {
            if (isVisible) {
                SettingsRepository.removeHiddenModule(moduleKey)
            } else {
                SettingsRepository.addHiddenModule(moduleKey)
            }
        }
    }
}
