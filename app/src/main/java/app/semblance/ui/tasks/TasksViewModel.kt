package app.semblance.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.semblance.data.local.entity.TaskEntity
import app.semblance.data.repository.TaskRepository
import app.semblance.engine.EngineClient
import app.semblance.engine.model.ProfileUiState
import app.semblance.engine.model.TaskUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val engineClient: EngineClient,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val profiles: StateFlow<List<ProfileUiState>> = engineClient.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskUi>> = taskRepository.allTasks
        .map { list -> list.map { it.toTaskUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTaskTrace(id: String): Flow<TaskUi?> {
        return taskRepository.getTask(id).map { it?.toTaskUi() }
    }

    fun sendInstruction(targets: List<Int>, text: String, runAt: Long?) {
        viewModelScope.launch {
            val targetList = if (targets.isEmpty()) profiles.value.map { it.id } else targets
            engineClient.sendInstruction(targetList, text, runAt)
        }
    }
}

private fun TaskEntity.toTaskUi(): TaskUi {
    val targetIds = try {
        Json.decodeFromString<List<Int>>(this.targetProfilesJson)
    } catch (e: Exception) {
        emptyList()
    }
    val traceList = try {
        Json.decodeFromString<List<String>>(this.traceLogJson)
    } catch (e: Exception) {
        emptyList()
    }
    return TaskUi(
        id = this.id,
        targets = targetIds,
        instruction = this.instruction,
        status = this.status,
        createdAt = this.createdAt,
        runAt = this.runAt,
        completedAt = this.completedAt,
        traces = traceList
    )
}
