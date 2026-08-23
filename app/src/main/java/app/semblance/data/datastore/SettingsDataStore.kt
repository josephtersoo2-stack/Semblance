package app.semblance.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "semblance_settings")

interface SettingsDataStore {
    val settings: Flow<AppSettings>
    suspend fun updateMitmEndpoint(endpoint: String)
    suspend fun updatePortRange(start: Int, end: Int)
    suspend fun updateCaInstalled(installed: Boolean)
    suspend fun updateLlmProvider(provider: String)
    suspend fun updateTacticalModel(model: String)
    suspend fun updateStrategicModel(model: String)
    suspend fun updateWorkerPoolSize(size: Int)
    suspend fun updateStorageBudget(mb: Int)
    suspend fun updateAutoQa(enabled: Boolean)
    suspend fun updateDarkTheme(forced: Boolean)
}

@Singleton
class SettingsDataStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsDataStore {
    companion object {
        private val MITM_ENDPOINT = stringPreferencesKey("mitm_endpoint")
        private val PORT_RANGE_START = intPreferencesKey("port_range_start")
        private val PORT_RANGE_END = intPreferencesKey("port_range_end")
        private val CA_INSTALLED = booleanPreferencesKey("ca_installed")
        private val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        private val TACTICAL_MODEL = stringPreferencesKey("tactical_model")
        private val STRATEGIC_MODEL = stringPreferencesKey("strategic_model")
        private val WORKER_POOL_SIZE = intPreferencesKey("worker_pool_size")
        private val STORAGE_BUDGET_MB = intPreferencesKey("storage_budget_mb")
        private val AUTO_QA_ENABLED = booleanPreferencesKey("auto_qa_enabled")
        private val DARK_THEME_FORCED = booleanPreferencesKey("dark_theme_forced")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            mitmEndpoint = prefs[MITM_ENDPOINT] ?: "127.0.0.1:8080",
            portRangeStart = prefs[PORT_RANGE_START] ?: 8080,
            portRangeEnd = prefs[PORT_RANGE_END] ?: 8088,
            caInstalled = prefs[CA_INSTALLED] ?: true,
            llmProvider = prefs[LLM_PROVIDER] ?: "Internal Gemini Router",
            tacticalModel = prefs[TACTICAL_MODEL] ?: "gemini-1.5-flash-tactical",
            strategicModel = prefs[STRATEGIC_MODEL] ?: "gemini-1.5-pro-planner",
            workerPoolSize = prefs[WORKER_POOL_SIZE] ?: 8,
            storageBudgetMb = prefs[STORAGE_BUDGET_MB] ?: 500,
            autoQaEnabled = prefs[AUTO_QA_ENABLED] ?: true,
            darkThemeForced = prefs[DARK_THEME_FORCED] ?: true
        )
    }

    override suspend fun updateMitmEndpoint(endpoint: String) {
        context.dataStore.edit { it[MITM_ENDPOINT] = endpoint }
    }

    override suspend fun updatePortRange(start: Int, end: Int) {
        context.dataStore.edit {
            it[PORT_RANGE_START] = start
            it[PORT_RANGE_END] = end
        }
    }

    override suspend fun updateCaInstalled(installed: Boolean) {
        context.dataStore.edit { it[CA_INSTALLED] = installed }
    }

    override suspend fun updateLlmProvider(provider: String) {
        context.dataStore.edit { it[LLM_PROVIDER] = provider }
    }

    override suspend fun updateTacticalModel(model: String) {
        context.dataStore.edit { it[TACTICAL_MODEL] = model }
    }

    override suspend fun updateStrategicModel(model: String) {
        context.dataStore.edit { it[STRATEGIC_MODEL] = model }
    }

    override suspend fun updateWorkerPoolSize(size: Int) {
        context.dataStore.edit { it[WORKER_POOL_SIZE] = size }
    }

    override suspend fun updateStorageBudget(mb: Int) {
        context.dataStore.edit { it[STORAGE_BUDGET_MB] = mb }
    }

    override suspend fun updateAutoQa(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_QA_ENABLED] = enabled }
    }

    override suspend fun updateDarkTheme(forced: Boolean) {
        context.dataStore.edit { it[DARK_THEME_FORCED] = forced }
    }
}
