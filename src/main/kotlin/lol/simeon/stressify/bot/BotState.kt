package lol.simeon.stressify.bot

data class BotState(
    val name: String,
    val status: BotStatus,
    val lastReason: String? = null
)
