package persistance

import org.junit.jupiter.api.Assertions.*
import tira.predef.std.VisibleListElements
import kotlin.test.Test
import kotlin.NoSuchElementException

class VisibleElementsTests {
    // without elements
    @Test
    fun `current returns null on emtpy collection`() {
        val emptySource: List<Int> = listOf()

        val visible = VisibleListElements(emptySource)

        assertEquals(true, visible.current() == null)
    }

    @Test
    fun `hasPrevious returns false on emtpy collection`() {
        val emptySource: List<Int> = listOf()

        val visible = VisibleListElements(emptySource)

        assertEquals(true, !visible.hasPrevious())
    }

    @Test
    fun `previous throws exception on emtpy collection`() {
        val emptySource: List<Int> = listOf()

        val visible = VisibleListElements(emptySource)

        var isExceptionThrown = false

        try {
            visible.previous()
        } catch (err: NoSuchElementException) {
            isExceptionThrown = true
        }

        assertEquals(true, isExceptionThrown)
    }

    @Test
    fun `hasNext returns false on emtpy collection`() {
        val emptySource: List<Int> = listOf()

        val visible = VisibleListElements(emptySource)

        assertEquals(true, !visible.hasNext())
    }

    @Test
    fun `next throws exception on empty collection`() {
        val emptySource: List<Int> = listOf()

        val visible = VisibleListElements(emptySource)

        var isExceptionThrown = false

        try {
            visible.next()
        } catch (err: NoSuchElementException) {
            isExceptionThrown = true
        }

        assertEquals(true, isExceptionThrown)
    }

    // with 1st elements
    @Test
    fun `current returns first element`() {
        val source: List<Int> = listOf(1)

        val visible = VisibleListElements(source)

        var isExceptionThrown = false

        var nextElement: Int? = null
        try {
            nextElement = visible.current()
        } catch (err: NoSuchElementException) {
            isExceptionThrown = true
        }

        assertEquals(false, isExceptionThrown)
        assertEquals(true, nextElement != null && nextElement == 1)
    }

    @Test
    fun `next throws exception at first element of collection`() {
        val source: List<Int> = listOf(1)

        val visible = VisibleListElements(source)

        var isExceptionThrown = false

        var counted = 0
        try {
            visible.next()
            counted += 1
        } catch (err: NoSuchElementException) {
            isExceptionThrown = true
        }

        assertEquals(true, isExceptionThrown)
        assertEquals(true, counted == 0)

    }

    @Test
    fun `next throws exception at end of collection`() {
        val source: List<Int> = listOf(1)

        //It takes current as first element of collection or returns null on empty
        val visible = VisibleListElements(source)

        var isExceptionThrown = false

        var counted = 0
        try {
            visible.next()
            counted += 1
        } catch (err: NoSuchElementException) {
            isExceptionThrown = true
        }

        assertEquals(true, isExceptionThrown)
        assertEquals(true, counted == 0)

    }

    @Test
    fun `previous throws exception on non empty collection`() {
        val emptySource: List<Int> = listOf(1)

        val visible = VisibleListElements(emptySource)

        var isExceptionThrown = false

        try {
            visible.previous()
        } catch (err: NoSuchElementException) {
            isExceptionThrown = true
        }

        assertEquals(true, isExceptionThrown)
    }

    @Test
    fun `can navigate on elements`() {
        val emptySource: List<Int> = listOf(1, 2, 3)

        val visible = VisibleListElements(emptySource)

        assertEquals(1, visible.current())

        assertEquals(2, visible.next())
        assertEquals(2, visible.current())

        assertEquals(3, visible.next())
        assertEquals(3, visible.current())

        assertEquals(2, visible.previous())
        assertEquals(2, visible.current())

        assertEquals(1, visible.previous())
        assertEquals(1, visible.current())
    }

    @Test
    fun `insert() add element in empty collection and current changes`() {
        val emptySource: List<Int> = listOf()

        val visible = VisibleListElements(emptySource)

        visible.insert(0)
        assertEquals(0, visible.current())
    }

    @Test
    fun `insert() add element before current with 1 element source`() {
        val emptySource: List<Int> = listOf(1)

        val visible = VisibleListElements(emptySource)

        visible.insert(0)
        assertEquals(listOf(0, 1), visible.elements())

    }

    @Test
    fun `insert() add element before current with`() {
        val emptySource: List<Int> = listOf(1, 2)

        val visible = VisibleListElements(emptySource)

        visible.next()
        visible.insert(0)

        assertEquals(listOf(1, 0, 2), visible.elements())
    }

    @Test
    fun `remove() drops last element`() {
        val emptySource: List<Int> = listOf(1)

        val visible = VisibleListElements(emptySource)

        visible.remove()

        assertEquals(null, visible.current())
    }

    @Test
    fun `remove() drops current element with shifting forward at first element`() {
        val emptySource: List<Int> = listOf(1, 2, 3)

        val visible = VisibleListElements(emptySource)

        visible.remove()

        assertEquals(listOf(2,3), visible.elements())
        assertEquals(2, visible.current())
    }

    @Test
    fun `remove() drops current element with shifting back at last element`() {
        val emptySource: List<Int> = listOf(1, 2, 3)

        val visible = VisibleListElements(emptySource)

        visible.next()
        visible.next()

        visible.remove()

        assertEquals(listOf(1,2), visible.elements())
        assertEquals(2, visible.current())
    }
}
