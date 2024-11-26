package tira.persistance.domain

import tira.persistance.domain.newtypes.ValidatedFile
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.util.*

interface Property {
    fun name(): String
    fun value(): String
}

class RawProperty(
    val name: String,
    val value: String
) : Property {
    override fun name(): String = name

    override fun value(): String = value
}

enum class PropertyName(name: String) {
    Completed("completed")
}

/**
 * MD's yaml front matter reader with tags replacement
 */
class FileProperty(
    private val file: ValidatedFile
) {
    private val rawProps = Parser().parse(FileInputStream(file.underlying.absolutePath())).toMutableList()

    fun props(): List<Property> {
        val res = mutableListOf<Property>()

        for (raw in rawProps) {
            if (raw is ValueToken) {
                res.add(RawProperty(raw.name, raw.value))
            }
        }
        return res
    }

    fun addProperty(property: Property) {
        if (!hasSameProperty(property)) {
            if (rawProps.isEmpty()) {
                rawProps.add(ValueToken(property.name(), property.value(), 1))
            } else {
                rawProps.add(ValueToken(property.name(), property.value(), rawProps.last().linePos() + 1))
            }
        } else {
            replaceValueWith(property)
        }
        val fr = FileWriter(file.underlying.absolutePath())
        fr.write(renderProps())
        fr.flush()
        fr.close()
    }

    private fun hasSameProperty(newProperty: Property): Boolean {
        for (token in rawProps.withIndex()) {
            if (token.value.name() == newProperty.name()) {
                return true
            }
        }
        return false
    }

    private fun replaceValueWith(newProperty: Property) {
        for (token in rawProps.withIndex()) {
            if (token.value.name() == newProperty.name()) {
                rawProps[token.index] = ValueToken(newProperty.name(), newProperty.value(), token.index + 1)
                break
            }
        }
    }

    private fun renderProps(): String {
        val sb = StringBuilder()
        sb.append("---\n")
        rawProps.fold(sb) { sb, pr ->
            sb.append(pr.repr()).append("\n")
        }
        sb.append("---\n")
        return sb.toString()
    }
}

class Parser() {
    private val items = LinkedList<Token>()
    fun parse(input: InputStream): List<Token> {
        val br = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
        var isBlockOpened = false

        for (line in br.readLines().withIndex()) {
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
