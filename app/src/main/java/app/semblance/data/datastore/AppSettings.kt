package app.semblance.data.datastore

data class AppSettings(
    val mitmEndpoint: String = "127.0.0.1:8080",
    val portRangeStart: Int = 8080,
    val portRangeEnd: Int = 8088,
    val caInstalled: Boolean = true,
    val llmProvider: String = "Internal Gemini Router",
    val tacticalModel: String = "gemini-1.5-flash-tactical",
    val strategicModel: String = "gemini-1.5-pro-planner",
    val workerPoolSize: Int = 8,
    val storageBudgetMb: Int = 500,
    val autoQaEnabled: Boolean = true,
    val darkThemeForced: Boolean = true
)
