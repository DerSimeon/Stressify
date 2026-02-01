package lol.simeon.stressify.bot

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lol.simeon.stressify.NickGenerator
import lol.simeon.stressify.console.TextComponentConsole
import lol.simeon.stressify.protocol.MinecraftAddress
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BotManager(
    val nickname: NickGenerator,
    val address: MinecraftAddress,
    val count: Int,
    val withGravity: Boolean = false
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bots = mutableListOf<Bot>()

    private val online = AtomicInteger(0)
    private var allDead = CompletableDeferred<Unit>()

    val isShutdown = AtomicBoolean(false)

    data class BotState(
        val name: String,
        val status: Status,
        val lastReason: String? = null
    ) {
        enum class Status { CONNECTING, ONLINE, DEAD }
    }

    private val _botStates = MutableStateFlow<List<BotState>>(emptyList())
    val botStates: StateFlow<List<BotState>> = _botStates.asStateFlow()

    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val logs: SharedFlow<String> = _logs.asSharedFlow()

    fun launchAll(autoRespawnDelayMs: Long = 0L): List<Bot> {
        require(count > 0) { "count must be > 0" }
        if (isShutdown.get()) return emptyList()

        stopAll("Restarting")

        allDead = CompletableDeferred()
        online.set(0)

        val initial = buildList {
            repeat(count) {
                add(BotState(name = nickname.nextRealNick(), status = BotState.Status.CONNECTING))
            }
        }
        _botStates.value = initial
        _logs.tryEmit("[*] launching ${initial.size} bots -> ${address.hostname}:${address.port}")

        initial.forEach { entry ->
            val protocol = MinecraftProtocol(entry.name)

            val bot = Bot(
                protocol = protocol,
                address = address,
                withGravity = withGravity,
                autoRespawnDelayMs = autoRespawnDelayMs,
                onConnected = { b ->
                    val now = online.incrementAndGet()
                    _botStates.update { list ->
                        list.map {
                            if (it.name.equals(b.nickname, ignoreCase = true))
                                it.copy(status = BotState.Status.ONLINE, lastReason = null)
                            else it
                        }
                    }
                    _logs.tryEmit("[+] ${b.nickname} connected ($now online)")
                },
                onDead = { b, reason, cause ->
                    val nowOnline = online.updateAndGet { cur -> (cur - 1).coerceAtLeast(0) }

                    val reasonStr = reason?.let { TextComponentConsole.ansi(it).trim() }?.ifBlank { null }

                    _botStates.update { list ->
                        list.map {
                            if (it.name.equals(b.nickname, ignoreCase = true))
                                it.copy(status = BotState.Status.DEAD, lastReason = reasonStr)
                            else it
                        }
                    }

                    val msg = buildString {
                        append("[-] ${b.nickname} down")
                        if (reasonStr != null) append(": ").append(reasonStr)
                        append(" ($nowOnline online left)")
                    }
                    _logs.tryEmit(msg)
                    cause?.let { _logs.tryEmit("    cause: ${it::class.simpleName}: ${it.message}") }

                    if (nowOnline <= 0 && !allDead.isCompleted) {
                        allDead.complete(Unit)
                    }
                },
                emitLog = { _, message ->
                    _logs.tryEmit(message)
                }
            )

            bots += bot
            scope.launch { bot.connect() }
        }

        return bots.toList()
    }

    suspend fun awaitAllDead() {
        allDead.await()
    }

    fun stopAll(reason: String = "Stopping") {
        if (bots.isEmpty()) return

        bots.forEach { it.disconnect(reason) }
        bots.clear()
        online.set(0)

        _botStates.update { list ->
            list.map { st ->
                if (st.status == BotState.Status.DEAD) st
                else st.copy(status = BotState.Status.DEAD, lastReason = reason)
            }
        }
        _logs.tryEmit("[*] stopped all bots: $reason")

        if (!allDead.isCompleted) allDead.complete(Unit)
    }

    fun shutdown() {
        stopAll("Shutdown")
        isShutdown.set(true)
    }

    fun allBots(): List<Bot> = bots.toList()

    fun botNames(): List<String> = botStates.value.map { it.name }

    fun onlineCount(): Int = online.get()

    fun broadcast(text: String) {
        bots.forEach { it.sendChat(text) }
        _logs.tryEmit("[G] $text")
    }

    fun sendTo(name: String, text: String): Boolean {
        val bot = bots.firstOrNull { it.nickname.equals(name, ignoreCase = true) } ?: return false
        bot.sendChat(text)
        _logs.tryEmit("[@$name] $text")
        return true
    }

    fun connectAll(autoRespawnDelayMs: Long = 0L) = launchAll(autoRespawnDelayMs)

    fun disconnectAll(reason: String = "Disconnect") = stopAll(reason)

    fun sendGlobal(text: String) = broadcast(text)

    fun sendToBot(botName: String, text: String): Boolean = sendTo(botName, text)
}
