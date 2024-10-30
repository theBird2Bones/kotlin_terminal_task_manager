package tira.predef.props

import tira.persistance.domain.Task

interface WithContent<A> {
    fun A?.content(): Iterator<String>
}

val taskWithContent = object : WithContent<Task> {
    override fun Task?.content(): Iterator<String> = this?.content() ?: listOf("<empty>").iterator()
}
