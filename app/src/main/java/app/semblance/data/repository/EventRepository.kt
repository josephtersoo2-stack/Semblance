package app.semblance.data.repository

import app.semblance.data.local.dao.EventDao
import app.semblance.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao
) {
    fun getRecentEvents(limit: Int = 100): Flow<List<EventEntity>> = eventDao.getRecentEvents(limit)

    fun getEventsForProfile(profileId: Int, limit: Int = 100): Flow<List<EventEntity>> =
        eventDao.getEventsByProfile(profileId, limit)

    suspend fun recordEvent(event: EventEntity) = eventDao.insert(event)

    suspend fun recordEvents(events: List<EventEntity>) = eventDao.insertAll(events)

    suspend fun trimOlderThan(beforeTs: Long) = eventDao.trimOlderThan(beforeTs)
}
