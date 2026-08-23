package app.semblance.engine.mock

import app.semblance.data.datastore.AppSettings
import app.semblance.data.datastore.SettingsDataStore
import app.semblance.data.local.dao.EventDao
import app.semblance.data.local.dao.ProfileDao
import app.semblance.data.local.dao.TaskDao
import app.semblance.data.local.entity.EventEntity
import app.semblance.data.local.entity.ProfileEntity
import app.semblance.data.local.entity.TaskEntity
import app.semblance.data.repository.EventRepository
import app.semblance.data.repository.ProfileRepository
import app.semblance.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockEngineTest {

    private val profilesStore = mutableMapOf<Int, ProfileEntity>()
    private val tasksStore = mutableMapOf<String, TaskEntity>()
    private val eventsStore = mutableListOf<EventEntity>()

    private val profilesFlow = MutableStateFlow<List<ProfileEntity>>(emptyList())
    private val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())

    private lateinit var profileRepository: ProfileRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var eventRepository: EventRepository

    private val fakeProfileDao = object : ProfileDao {
        override fun getAllProfiles(): Flow<List<ProfileEntity>> = profilesFlow
        override fun getProfileById(id: Int): Flow<ProfileEntity?> = profilesFlow.map { it.find { p -> p.id == id } }
        override suspend fun getProfileByIdSync(id: Int): ProfileEntity? = profilesStore[id]
        override suspend fun getProfileCount(): Int = profilesStore.size
        override suspend fun insertOrUpdate(profile: ProfileEntity) {
            profilesStore[profile.id] = profile
            profilesFlow.value = profilesStore.values.toList()
        }
        override suspend fun insertAll(profiles: List<ProfileEntity>) {
            profiles.forEach { profilesStore[it.id] = it }
            profilesFlow.value = profilesStore.values.toList()
        }
        override suspend fun update(profile: ProfileEntity) {
            profilesStore[profile.id] = profile
            profilesFlow.value = profilesStore.values.toList()
        }
        override suspend fun delete(profile: ProfileEntity) {
            profilesStore.remove(profile.id)
            profilesFlow.value = profilesStore.values.toList()
        }
        override suspend fun deleteById(id: Int) {
            profilesStore.remove(id)
            profilesFlow.value = profilesStore.values.toList()
        }
    }

    private val fakeTaskDao = object : TaskDao {
        override fun getAllTasks(): Flow<List<TaskEntity>> = tasksFlow
        override fun getTaskById(id: String): Flow<TaskEntity?> = tasksFlow.map { it.find { t -> t.id == id } }
        override suspend fun getTaskByIdSync(id: String): TaskEntity? = tasksStore[id]
        override suspend fun insert(task: TaskEntity) {
            tasksStore[task.id] = task
            tasksFlow.value = tasksStore.values.toList()
        }
        override suspend fun update(task: TaskEntity) {
            tasksStore[task.id] = task
            tasksFlow.value = tasksStore.values.toList()
        }
        override suspend fun updateStatusAndTrace(id: String, status: String, traceLogJson: String, completedAt: Long?) {
            val existing = tasksStore[id]
            if (existing != null) {
                val updated = existing.copy(status = status, traceLogJson = traceLogJson, completedAt = completedAt)
                tasksStore[id] = updated
                tasksFlow.value = tasksStore.values.toList()
            }
        }
    }

    private val fakeEventDao = object : EventDao {
        override fun getRecentEvents(limit: Int): Flow<List<EventEntity>> = flowOf(eventsStore.takeLast(limit).reversed())
        override fun getEventsByProfile(profileId: Int, limit: Int): Flow<List<EventEntity>> =
            flowOf(eventsStore.filter { it.profileId == profileId }.takeLast(limit).reversed())
        override suspend fun insert(event: EventEntity) {
            eventsStore.add(event)
        }
        override suspend fun insertAll(events: List<EventEntity>) {
            eventsStore.addAll(events)
        }
        override suspend fun trimOlderThan(beforeTs: Long) {
            eventsStore.removeAll { it.ts < beforeTs }
        }
    }

    @Before
    fun setup() {
        profilesStore.clear()
        tasksStore.clear()
        eventsStore.clear()
        profileRepository = ProfileRepository(fakeProfileDao)
        taskRepository = TaskRepository(fakeTaskDao)
        eventRepository = EventRepository(fakeEventDao)
    }

    @Test
    fun testSeedProfilesCreatesEightProfiles() = runTest {
        val engine = MockEngine(
            profileRepository = profileRepository,
            taskRepository = taskRepository,
            eventRepository = eventRepository,
            settingsDataStore = FakeSettingsDataStore()
        )

        var count = 0
        for (i in 1..30) {
            count = profileRepository.getCount()
            if (count == 8) break
            Thread.sleep(50)
        }

        assertEquals(8, count)

        var profileList = engine.profiles.first()
        for (i in 1..30) {
            profileList = engine.profiles.first()
            if (profileList.size == 8) break
            Thread.sleep(50)
        }
        assertEquals(8, profileList.size)
        assertTrue(profileList.any { it.alias == "alex_prime" })
        assertTrue(profileList.any { it.alias == "kev_19" })
    }

    @Test
    fun testWizardCreatedProfileHasWarmupPhase() = runTest {
        val engine = MockEngine(
            profileRepository = profileRepository,
            taskRepository = taskRepository,
            eventRepository = eventRepository,
            settingsDataStore = FakeSettingsDataStore()
        )

        for (i in 1..30) {
            if (profileRepository.getCount() == 8) break
            Thread.sleep(50)
        }

        val newProfile = ProfileEntity(
            id = 99,
            suffix = "p99",
            alias = "test_wizard_user",
            age = 28,
            tz = "America/New_York",
            voice = "casual",
            deviceModel = "Pixel 7 Pro",
            phase = "ACTIVE", // Intentionally set to ACTIVE to verify override
            status = "WATCHING",
            warmth = 50
        )

        engine.createProfileFromWizard(newProfile)

        val persisted = profileRepository.getProfileSync(99)
        assertNotNull(persisted)
        assertEquals("WARMUP", persisted!!.phase)
        assertEquals(0, persisted.warmth)
        assertEquals("SLEEPING", persisted.status)
    }

    private class FakeSettingsDataStore : SettingsDataStore {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings

        override suspend fun updateMitmEndpoint(endpoint: String) {}
        override suspend fun updatePortRange(start: Int, end: Int) {}
        override suspend fun updateCaInstalled(installed: Boolean) {}
        override suspend fun updateLlmProvider(provider: String) {}
        override suspend fun updateTacticalModel(model: String) {}
        override suspend fun updateStrategicModel(model: String) {}
        override suspend fun updateWorkerPoolSize(size: Int) {}
        override suspend fun updateStorageBudget(mb: Int) {}
        override suspend fun updateAutoQa(enabled: Boolean) {}
        override suspend fun updateDarkTheme(forced: Boolean) {}
    }
}
