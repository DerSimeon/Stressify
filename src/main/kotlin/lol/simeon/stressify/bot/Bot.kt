@file:OptIn(ExperimentalAtomicApi::class)

package lol.simeon.stressify.bot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import lol.simeon.stressify.protocol.MinecraftAddress
import org.geysermc.mcprotocollib.auth.SessionService
import org.geysermc.mcprotocollib.network.ClientSession
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory
import org.geysermc.mcprotocollib.protocol.MinecraftConstants
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket
import java.time.Instant
import java.util.BitSet
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class Bot(
    val protocol: MinecraftProtocol,
    val address: MinecraftAddress,
    private val autoRespawnDelayMs: Long = 0L,
    private val onConnected: (bot: Bot) -> Unit,
    private val onDead: (bot: Bot, reason: String?, cause: Throwable?) -> Unit
) {
    val nickname: String = protocol.profile.name

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    internal val session: ClientSession =
        ClientNetworkSessionFactory.factory()
            // adjust these property names if your MinecraftAddress differs
            .setAddress(address.hostname, address.port)
            .setProtocol(protocol)
            .create()
            .also { client ->
                client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, SessionService())
                client.addListener(BotListener(this, autoRespawnDelayMs))
            }

    internal var connected: Boolean = false
    internal var lastX: Double = -1.0
    internal var lastY: Double = -1.0
    internal var lastZ: Double = -1.0

    private val deadNotified = AtomicBoolean(false)

    fun connect() {
        session.connect()
    }

    fun disconnect(reason: String = "Leaving") {
        scope.cancel()
        session.disconnect(reason)
    }

    fun sendChat(text: String) {
        if (text.startsWith("/")) {
            session.send(ServerboundChatCommandPacket(text.removePrefix("/")))
            return
        }

        // same approach as the Java file: unsigned message, salt 0, empty-ish signature
        session.send(
            ServerboundChatPacket(
                text,
                Instant.now().toEpochMilli(),
                0L,
                null,
                0,
                BitSet(),
                0
            )
        )
    }

    fun moveTo(x: Double, y: Double, z: Double) {
        lastX = x
        lastY = y
        lastZ = z
        session.send(ServerboundMovePlayerPosPacket(true, false, x, y, z))
    }

    fun move(dx: Double, dy: Double, dz: Double) {
        moveTo(lastX + dx, lastY + dy, lastZ + dz)
    }

    fun fallDown(step: Double = 0.5) {
        if (connected && lastY > 0) move(0.0, -step, 0.0)
    }

    internal fun onConnected() {
        connected = true
        onConnected(this)
    }

    internal fun onDisconnected(reason: String?, cause: Throwable?) {

        if (deadNotified.compareAndSet(false, true)) {
            connected = false
            onDead(this, reason, cause)
        }
    }
}