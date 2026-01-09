package lol.simeon.stressify.console

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object TextComponentConsole {

    val ansi = ANSIComponentSerializer.ansi()
    val plain = PlainTextComponentSerializer.plainText()

    fun ansi(value: Any?): String = when (value) {
        null -> ""
        is Component -> ansi.serialize(value)
        else -> value.toString()
    }

    fun plain(value: Any?): String = when (value) {
        null -> ""
        is Component -> plain.serialize(value)
        else -> value.toString()
    }
}