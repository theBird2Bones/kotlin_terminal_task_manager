package tira.persistance.domain

import java.time.LocalDateTime

interface Property {
    fun repr(): String

    companion object {
        data class TextProp(
            val value: String
        ) : Property {
            override fun repr(): String = value
        }

        data class IntProp(
            val value: Int
        ) : Property {
            override fun repr(): String = value.toString()
        }

        data class DateProp(
            val value: LocalDateTime
        ) : Property {
            override fun repr(): String = value.toString()
        }
    }
}
