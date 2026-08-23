package app.semblance.ui.pulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.semblance.data.repository.EventRepository
import app.semblance.engine.EngineClient
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

data class InterestDrift(
    val topic: String,
    val weight: Float,
    val deltaPercent: Int // +12, -4, etc.
)

data class FleetStats(
    val totalWatchMin: Int = 184,
    val totalComments: Int = 26,
    val totalSessions: Int = 38,
    val avgWarmth: Int = 62
)

@HiltViewModel
class PulseViewModel @Inject constructor(
    private val engineClient: EngineClient,
    private val eventRepository: EventRepository
) : ViewModel() {

    val profiles: StateFlow<List<ProfileUiState>> = engineClient.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableStateFlow<List<AgentEvent>>(emptyList())
    val events: StateFlow<List<AgentEvent>> = _events.asStateFlow()

    val interestDrifts: List<InterestDrift> = listOf(
        InterestDrift("technology", 0.45f, 12),
        InterestDrift("gaming", 0.35f, -3),
        InterestDrift("ai_research", 0.30f, 18),
        InterestDrift("cybersec", 0.25f, 5),
        InterestDrift("travel", 0.20f, -8),
        InterestDrift("culinary", 0.15f, 2)
    )

    val stats: FleetStats = FleetStats(
        totalWatchMin = 342,
        totalComments = 48,
        totalSessions = 56,
        avgWarmth = 65
    )

    init {
        viewModelScope.launch {
            engineClient.events.collect { event ->
                _events.value = (listOf(event) + _events.value).take(100)
            }
        }

        viewModelScope.launch {
            eventRepository.getRecentEvents(50).collect { list ->
                if (_events.value.isEmpty() && list.isNotEmpty()) {
                    _events.value = list.map { AgentEvent(it.profileId, it.ts, it.kind, it.text) }
                }
            }
        }
    }
}
