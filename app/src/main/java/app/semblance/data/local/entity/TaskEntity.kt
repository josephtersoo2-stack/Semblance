package app.semblance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String, // UUID
    val targetProfilesJson: String, // JSON array of Int profile ids, e.g. "[1,2,3]"
    val instruction: String,
    val status: String, // "queued" | "running" | "done" | "failed"
    val createdAt: Long,
    val runAt: Long? = null,
    val completedAt: Long? = null,
    val traceLogJson: String = "[]" // JSON array of trace strings
)
