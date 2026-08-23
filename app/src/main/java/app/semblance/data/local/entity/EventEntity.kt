package app.semblance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Int,
    val ts: Long,
    val kind: String, // "llm" | "motor" | "nav" | "mitm" | "sys"
    val text: String
)
