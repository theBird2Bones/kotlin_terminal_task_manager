package tira.predef.std

interface VisibleElements<A> {
    fun hasPrevious(): Boolean
    fun previous(): A

    fun hasNext(): Boolean
    fun next(): A

    fun current(): A?

    fun elements(): List<A>
    fun isEmpty(): Boolean

    fun insert(element: A)
}

class VisibleListElements<A>(elements: List<A>) : VisibleElements<A> {
    private val _elements: MutableList<A> = elements.toMutableList()
    private var idx: Int = 0

    override fun hasPrevious(): Boolean = isInBound(idx - 1)

    override fun previous(): A {
        if (!hasPrevious()) throw NoSuchElementException("Has no previous element")
        idx -= 1
        return current() ?: throw NoSuchElementException("Has no previous element")
    }

    override fun hasNext(): Boolean = isInBound(idx + 1)

    override fun next(): A {
        if (!hasNext()) throw NoSuchElementException("Has no previous element")
        idx += 1
        return current() ?: throw NoSuchElementException("Has no previous element")
    }

    private fun isInBound(nextIndex: Int): Boolean = (nextIndex >= 0 && nextIndex < _elements.size)

    override fun current(): A? {
        if (!isInBound(idx)) return null
        return _elements[idx]
    }

    override fun elements(): List<A> = _elements.toList()
    override fun isEmpty(): Boolean = _elements.isEmpty()

    override fun insert(element: A) {
        _elements.add(idx, element)
    }
}
