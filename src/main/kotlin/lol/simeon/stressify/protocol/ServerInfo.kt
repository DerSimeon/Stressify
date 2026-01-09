package lol.simeon.stressify.protocol

import org.geysermc.mcprotocollib.network.Session
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory
import org.geysermc.mcprotocollib.network.session.ClientNetworkSession
import org.geysermc.mcprotocollib.protocol.MinecraftConstants
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoHandler
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerPingTimeHandler


class ServerInfo(address: MinecraftAddress) {

    val protocol = MinecraftProtocol()

    var serverStatusInfo: ServerStatusInfo? = null
    var ping: Long = 0
    var done: Boolean = false

    val client: ClientNetworkSession =
        ClientNetworkSessionFactory.factory().setAddress(address.hostname, address.port).setProtocol(protocol)
            .create().also {
                it.setFlag(
                    MinecraftConstants.SERVER_INFO_HANDLER_KEY,
                    ServerInfoHandler { _: Session, info: ServerStatusInfo ->
                        this.serverStatusInfo = info
                    })

                it.setFlag(
                    MinecraftConstants.SERVER_PING_TIME_HANDLER_KEY, ServerPingTimeHandler { _: Session, ping: Long ->
                        this.ping = ping
                    })

                it.addListener(object : SessionAdapter() {
                    override fun disconnected(event: DisconnectedEvent?) {
                        done = true
                    }
                })

            }

    fun requestInfo() {
        client.connect()

        while (!done) {
            try {
                Thread.sleep(5)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }


}