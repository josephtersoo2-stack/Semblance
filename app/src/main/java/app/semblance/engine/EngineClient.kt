package app.semblance.engine

import app.semblance.engine.model.ActionJson
import app.semblance.engine.model.AgentEvent
import app.semblance.engine.model.ProfileUiState
import app.semblance.engine.model.ThumbFrame
import kotlinx.coroutines.flow.Flow

interface EngineClient {
    val profiles: Flow<List<ProfileUiState>>
    fun profileFlow(id: Int): Flow<ProfileUiState>
    val thumbs: Flow<ThumbFrame>
    val events: Flow<AgentEvent>
    suspend fun open(id: Int)
    suspend fun close(id: Int, save: Boolean = true)
    suspend fun wakeNow(id: Int)
    suspend fun sleepNow(id: Int)
    suspend fun snapshot(id: Int): String
    suspend fun action(id: Int, action: ActionJson)
    suspend fun sendInstruction(targets: List<Int>, text: String, runAt: Long?)
    suspend fun runQa(id: Int)
}
