package app.semblance.engine.model

data class TaskUi(
    val id: String,
    val targets: List<Int>,
    val instruction: String,
    val status: String, // "queued" | "running" | "done" | "failed"
    val createdAt: Long,
    val runAt: Long? = null,
    val completedAt: Long? = null,
    val traces: List<String> = emptyList()
)
