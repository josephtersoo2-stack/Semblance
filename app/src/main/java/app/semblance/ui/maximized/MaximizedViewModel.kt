package app.semblance.ui.maximized

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.semblance.data.repository.EventRepository
import app.semblance.engine.EngineClient
import app.semblance.engine.model.ActionJson
import app.semblance.engine.model.AgentEvent
import app.semblance.engine.model.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaximizedViewModel @Inject constructor(
    private val engineClient: EngineClient,
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val profileId: Int = checkNotNull(savedStateHandle["profileId"])

    val profile: StateFlow<ProfileUiState?> = engineClient.profileFlow(profileId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _eventsList = MutableStateFlow<List<AgentEvent>>(emptyList())
    val events: StateFlow<List<AgentEvent>> = _eventsList.asStateFlow()

    private val _latestThumb = MutableStateFlow<ByteArray?>(null)
    val latestThumb: StateFlow<ByteArray?> = _latestThumb.asStateFlow()

    private val _snapshotResult = MutableStateFlow<String?>(null)
    val snapshotResult: StateFlow<String?> = _snapshotResult.asStateFlow()

    private val _isCustomViewShowing = MutableStateFlow(false)
    val isCustomViewShowing: StateFlow<Boolean> = _isCustomViewShowing.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    init {
        viewModelScope.launch {
            // Maximize: open surface and unmute media
            engineClient.maximize(profileId)
        }

        viewModelScope.launch {
            engineClient.thumbs.collect { frame ->
                if (frame.profileId == profileId) {
                    _latestThumb.value = frame.jpeg
                }
            }
        }

        viewModelScope.launch {
            engineClient.events.collect { event ->
                if (event.profileId == profileId) {
                    _eventsList.value = listOf(event) + _eventsList.value.take(50)
                }
            }
        }

        viewModelScope.launch {
            engineClient.customViewEvents.collect { (pid, isShowing) ->
                if (pid == profileId) {
                    _isCustomViewShowing.value = isShowing
                }
            }
        }

        viewModelScope.launch {
            eventRepository.getEventsForProfile(profileId, 30).collect { list ->
                if (_eventsList.value.isEmpty() && list.isNotEmpty()) {
                    _eventsList.value = list.map { AgentEvent(it.profileId, it.ts, it.kind, it.text) }
                }
            }
        }
    }

    fun sendInstruction(text: String, runAt: Long?) {
        viewModelScope.launch {
            engineClient.sendInstruction(listOf(profileId), text, runAt)
        }
    }

    fun executeQuickAction(type: String, param: String = "") {
        viewModelScope.launch {
            when (type) {
                "like" -> engineClient.action(profileId, ActionJson.Tap(intent = "like_video_button"))
                "comment" -> engineClient.action(profileId, ActionJson.TypeText(text = param.ifEmpty { "Awesome setup and detailed analysis!" }))
                "pause" -> engineClient.action(profileId, ActionJson.Tap(intent = "video_player_viewport"))
                "navigate" -> engineClient.action(profileId, ActionJson.Navigate(url = param.ifEmpty { "https://youtube.com/trending" }))
                "back" -> engineClient.action(profileId, ActionJson.Back)
                "app_switch" -> engineClient.simulateAppSwitch(profileId, 3000L)
                "volume", "toggle_mute" -> {
                    if (_isMuted.value) {
                        engineClient.maximize(profileId)
                        _isMuted.value = false
                    } else {
                        engineClient.minimize(profileId)
                        _isMuted.value = true
                    }
                }
                else -> engineClient.action(profileId, ActionJson.Wait(s = 2.0f))
            }
        }
    }

    fun requestSnapshot() {
        viewModelScope.launch {
            val result = engineClient.snapshot(profileId)
            _snapshotResult.value = result
        }
    }

    fun clearSnapshot() {
        _snapshotResult.value = null
    }

    fun minimize() {
        viewModelScope.launch {
            // Mute media when minimizing to fleet view
            engineClient.minimize(profileId)
        }
    }

    fun close() {
        viewModelScope.launch {
            engineClient.close(profileId, save = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Automatically minimize (mute) on ViewModel cleared
        CoroutineScope(Dispatchers.Default).launch {
            try {
                engineClient.minimize(profileId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
