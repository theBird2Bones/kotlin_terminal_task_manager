package tira.predef.props

import tira.persistance.domain.Task

interface WithContent<A> {
    fun A?.content(): Iterator<String>
}

val taskWithContent = object : WithContent<Task> {
    override fun Task?.content(): Iterator<String> = this?.content() ?: listOf("<empty>").iterator()
}

val taskWithContentSkipProps = object : WithContent<Task> {
    override fun Task?.content(): Iterator<String> =
        FilteredPropsIterator(
            this?.content() ?: listOf("<empty>").iterator()
        )

    inner class FilteredPropsIterator(private val iter: Iterator<String>) : Iterator<String> {
        private var last: String? = null
        private var hasNext: Boolean = iter.hasNext()

        init {
            if (iter.hasNext()) {
                setNextAfterProps(iter)
            }
        }

        private fun setNextAfterProps(iter: Iterator<String>) {
            val next = iter.next()
            if (next == "---") {
                dropProps(iter)
                if (iter.hasNext()) {
                    last = iter.next()
                }
            } else {
                last = next
            }
            hasNext = iter.hasNext() && last != null
        }

        private fun dropProps(iter: Iterator<String>) {
            while (true) {
                if (iter.hasNext()) {
                    val next = iter.next()
                    if (next != "---") continue
                    else break
                }
            }
        }

        override fun hasNext(): Boolean {
            return hasNext
        }

        override fun next(): String {
            if (last == null) {
                throw NoSuchElementException()
            }

            val next = last
            if (iter.hasNext()) {
                last = iter.next()
                hasNext = iter.hasNext() || last != null
            } else {
                hasNext = false
            }
            return next!!
        }
    }
}
