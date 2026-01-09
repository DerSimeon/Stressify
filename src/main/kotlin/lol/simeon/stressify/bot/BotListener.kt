package lol.simeon.stressify.bot

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.geysermc.mcprotocollib.network.Session
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter
import org.geysermc.mcprotocollib.network.packet.Packet
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket
import java.util.regex.Pattern

class BotListener(
    private val bot: Bot,
    private val autoRespawnDelayMs: Long = 0L,
) : SessionAdapter() {

    override fun packetReceived(session: Session, packet: Packet) {
        when (packet) {
            is ClientboundLoginPacket -> {
                bot.onConnected()
            }

            is ClientboundPlayerPositionPacket -> {
                bot.lastX = packet.position.x
                bot.lastY = packet.position.y
                bot.lastZ = packet.position.z
                bot.session.send(ServerboundAcceptTeleportationPacket(packet.id))
            }

            is ClientboundPlayerCombatKillPacket -> {
                if (autoRespawnDelayMs >= 0) {
                    bot.scope.launch {
                        delay(autoRespawnDelayMs)
                        bot.session.send(ServerboundClientCommandPacket(ClientCommand.RESPAWN))
                    }
                }
            }
        }
    }

    override fun disconnected(event: DisconnectedEvent) {
        bot.connected = false

        val raw = event.reason?.toString().orEmpty()

        val pattern = Pattern.compile("content=\"(.*?)\"")
        val matcher = pattern.matcher(raw)
        val extracted = buildString {
            while (matcher.find()) append(matcher.group(1))
        }.trim()

        val reason = extracted.ifBlank { raw.ifBlank { null } }

        bot.onDisconnected(reason, event.cause)
    }
}