package app.semblance.data.repository

import app.semblance.data.local.dao.TaskDao
import app.semblance.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTask(id: String): Flow<TaskEntity?> = taskDao.getTaskById(id)

    suspend fun getTaskSync(id: String): TaskEntity? = taskDao.getTaskByIdSync(id)

    suspend fun insertTask(task: TaskEntity) = taskDao.insert(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)

    suspend fun updateStatusAndTrace(id: String, status: String, traceJson: String, completedAt: Long?) =
        taskDao.updateStatusAndTrace(id, status, traceJson, completedAt)
}
