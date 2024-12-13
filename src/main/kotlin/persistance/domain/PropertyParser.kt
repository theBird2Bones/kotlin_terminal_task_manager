package tira.persistance.domain

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class Parser() {
    private val items = LinkedList<Token>()
    fun parse(input: Path): List<Token> {
        var isBlockOpened = false

        for (line in Files.readAllLines(input).withIndex()) {
            if (line.value.isEmpty() && line.value != "---") throw IllegalStateException("Must start with ---")
            if (line.value == "---" && items.isEmpty()) {
                isBlockOpened = true
                continue
            }
            if (line.value == "---" && isBlockOpened) {
                break
            }

            val tokens = line.value.split(":", ignoreCase = false, limit = 2).map { it.trim() }
            if (tokens.size != 2) continue
            items.add(ValueToken(tokens[0], tokens[1], line.index))

        }
        return items
    }

    private fun readBlock(r: Reader) {
        val res = LinkedList<String>()
    }
}

interface Token {
    fun repr(): String
    fun name(): String
    fun linePos(): Int
}

class ValueToken(val name: String, val value: String, private val linePos: Int) : Token {
    override fun repr(): String = "${name}: ${value}"
    override fun name(): String = name

    override fun linePos(): Int = linePos
}
