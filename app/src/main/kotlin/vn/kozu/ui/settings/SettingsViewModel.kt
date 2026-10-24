package vn.kozu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.ThemeMode
import vn.kozu.ui.containerFactory

sealed interface VerifyState {
    data object Idle : VerifyState
    data object Checking : VerifyState
    data object Success : VerifyState
    data class Failure(val message: String) : VerifyState
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _verify = MutableStateFlow<VerifyState>(VerifyState.Idle)
    val verify: StateFlow<VerifyState> = _verify.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { container.settings.setThemeMode(mode) }
    fun setAmoled(enabled: Boolean) = viewModelScope.launch { container.settings.setAmoled(enabled) }
    fun setArtworkColors(enabled: Boolean) = viewModelScope.launch { container.settings.setArtworkColors(enabled) }
    fun setDisplayName(name: String) = viewModelScope.launch { container.settings.setDisplayName(name) }


    fun saveAndVerify(clientId: String, clientSecret: String) {
        viewModelScope.launch {
            _verify.value = VerifyState.Checking
            container.settings.setOsuCredentials(clientId, clientSecret)
            val result = container.catalog.verifyCredentials()
            _verify.value = result.fold(
                onSuccess = { VerifyState.Success },
                onFailure = { VerifyState.Failure(it.message ?: "Could not connect") }
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            container.settings.setOsuCredentials("", "")
            _verify.value = VerifyState.Idle
        }
    }

    fun clearHistory() = viewModelScope.launch { container.history.clear() }
    fun clearSearches() = viewModelScope.launch { container.settings.clearRecentSearches() }

    companion object {
        val Factory = containerFactory { SettingsViewModel(it) }
    }
}
