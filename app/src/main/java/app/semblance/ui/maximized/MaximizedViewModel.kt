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

    private val _events = MutableStateFlow<List<AgentEvent>>(emptyList())
    val events: StateFlow<List<AgentEvent>> = _events.asStateFlow()

    private val _latestThumb = MutableStateFlow<ByteArray?>(null)
    val latestThumb: StateFlow<ByteArray?> = _latestThumb.asStateFlow()

    private val _snapshotResult = MutableStateFlow<String?>(null)
    val snapshotResult: StateFlow<String?> = _snapshotResult.asStateFlow()

    init {
        viewModelScope.launch {
            engineClient.open(profileId)
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
                    _events.value = listOf(event) + _events.value.take(50)
                }
            }
        }

        viewModelScope.launch {
            eventRepository.getEventsForProfile(profileId, 30).collect { list ->
                if (_events.value.isEmpty() && list.isNotEmpty()) {
                    _events.value = list.map { AgentEvent(it.profileId, it.ts, it.kind, it.text) }
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
            val action = when (type) {
                "like" -> ActionJson.Tap(intent = "like_video_button")
                "comment" -> ActionJson.TypeText(text = param.ifEmpty { "Awesome setup and detailed analysis!" })
                "pause" -> ActionJson.Tap(intent = "video_player_viewport")
                "navigate" -> ActionJson.Navigate(url = param.ifEmpty { "https://youtube.com/trending" })
                "back" -> ActionJson.Back
                "volume" -> ActionJson.Volume(dir = "mute")
                else -> ActionJson.Wait(s = 2.0f)
            }
            engineClient.action(profileId, action)
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

    fun close() {
        viewModelScope.launch {
            engineClient.close(profileId, save = true)
        }
    }
}
