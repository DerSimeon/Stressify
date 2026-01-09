package lol.simeon.stressify.protocol

data class MinecraftAddress(
    val hostname: String,
    val port: Int
) {
    companion object {
        fun fromString(address: String): MinecraftAddress {
            val parts = address.split(":")
            val hostname = parts[0]
            val port = if (parts.size > 1) parts[1].toInt() else 25565
            return MinecraftAddress(hostname, port)
        }
    }
}
