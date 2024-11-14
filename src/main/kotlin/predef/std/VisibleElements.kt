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
    fun remove()
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

    private fun isInBound(idx: Int): Boolean = (idx >= 0 && idx < _elements.size)

    override fun current(): A? {
        if (!isInBound(idx)) return null
        return _elements[idx]
    }

    override fun elements(): List<A> = _elements.toList()
    override fun isEmpty(): Boolean = _elements.isEmpty()

    /*
     * Trying to move back after removing current element.
     * In position 1 : <<2>> : 3 new current will be 1 and collection is 1 : 3.
     * In position <<1>> : 2 : 3 new current will be 2 and collection is 2 : 3.
     * In position 1 : 2 : <<3>> new current will be 3 and collection is 1 : 2.
     */
    override fun remove() {
        if (_elements.isEmpty()) throw IllegalStateException("Collection is empty")
        _elements.removeAt(idx)
        if (hasPrevious()) {
            idx -= 1
        }

    }

    override fun insert(element: A) {
        _elements.add(idx, element)
    }
}
