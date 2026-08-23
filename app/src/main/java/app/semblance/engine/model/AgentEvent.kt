package app.semblance.engine.model

data class AgentEvent(
    val profileId: Int,
    val ts: Long,
    val kind: String, // "llm" | "motor" | "nav" | "mitm" | "sys"
    val text: String
)
