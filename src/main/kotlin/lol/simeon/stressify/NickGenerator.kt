package lol.simeon.stressify

import java.security.SecureRandom

class NickGenerator {

    val secureRandom = SecureRandom()
    var nicks: List<String>

    init {
        val lines = object {}.javaClass.getResourceAsStream("/nicks.txt")?.bufferedReader()?.readLines()
        if (lines.isNullOrEmpty()) {
            throw IllegalStateException("Failed to load nicks from resource!")
        }
        nicks = lines
    }

    fun nextRealNick(): String {
        return nicks[secureRandom.nextInt(nicks.size)]
    }


}