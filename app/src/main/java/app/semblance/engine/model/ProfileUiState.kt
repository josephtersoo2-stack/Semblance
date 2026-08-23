package app.semblance.engine.model

data class ProfileUiState(
    val id: Int,
    val alias: String,
    val deviceLabel: String,
    val status: ProfileStatus,
    val currentHost: String?,
    val proxyOk: Boolean,
    val warmth: Int,
    val nextWakeAt: Long?,
    val isLive: Boolean
)
