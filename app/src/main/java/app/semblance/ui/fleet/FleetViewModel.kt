package app.semblance.ui.fleet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.semblance.engine.EngineClient
import app.semblance.engine.mock.MockEngine
import app.semblance.engine.model.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FleetViewModel @Inject constructor(
    private val engineClient: EngineClient
) : ViewModel() {

    val profiles: StateFlow<List<ProfileUiState>> = engineClient.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _thumbsMap = MutableStateFlow<Map<Int, ByteArray>>(emptyMap())
    val thumbsMap: StateFlow<Map<Int, ByteArray>> = _thumbsMap.asStateFlow()

    init {
        viewModelScope.launch {
            engineClient.thumbs.collect { frame ->
                _thumbsMap.value = _thumbsMap.value + (frame.profileId to frame.jpeg)
            }
        }
    }

    fun wakeNow(id: Int) {
        viewModelScope.launch {
            engineClient.wakeNow(id)
        }
    }

    fun sleepNow(id: Int) {
        viewModelScope.launch {
            engineClient.sleepNow(id)
        }
    }

    fun runQa(id: Int) {
        viewModelScope.launch {
            engineClient.runQa(id)
        }
    }

    fun sendInstruction(id: Int, text: String) {
        viewModelScope.launch {
            engineClient.sendInstruction(listOf(id), text, null)
        }
    }

    fun startDayAll() {
        viewModelScope.launch {
            profiles.value.forEach { profile ->
                engineClient.wakeNow(profile.id)
            }
        }
    }

    fun pauseFleetAll() {
        viewModelScope.launch {
            profiles.value.forEach { profile ->
                engineClient.sleepNow(profile.id)
            }
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch {
            engineClient.deleteProfile(id)
        }
    }

    fun openInteractiveBrowser(id: Int) {
        viewModelScope.launch {
            engineClient.openInteractiveBrowser(id)
        }
    }
}

