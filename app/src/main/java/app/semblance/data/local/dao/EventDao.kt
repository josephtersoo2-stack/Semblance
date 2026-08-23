package app.semblance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.semblance.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY ts DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE profileId = :profileId ORDER BY ts DESC LIMIT :limit")
    fun getEventsByProfile(profileId: Int, limit: Int = 100): Flow<List<EventEntity>>

    @Insert
    suspend fun insert(event: EventEntity)

    @Insert
    suspend fun insertAll(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE ts < :beforeTs")
    suspend fun trimOlderThan(beforeTs: Long)
}
