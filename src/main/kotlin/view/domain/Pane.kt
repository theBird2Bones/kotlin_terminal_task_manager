package tira.view.domain

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen

import tira.predef.props.*
import tira.persistance.domain.Project
import tira.persistance.domain.Task

interface PaneSize {
    fun width(): Int
    fun height(): Int
}

interface PaneShift {
    fun offset(): TerminalPosition
}

class DynamicPaneShift(
    private val offsetInPercent: Int,
    private val screen: Screen
) : PaneShift {
    override fun offset(): TerminalPosition {
        return TerminalPosition.TOP_LEFT_CORNER.withRelativeColumn(
            screen.terminalSize.columns * offsetInPercent / 100
        )
    }
}

class DynamicPaneSize(
    private val widthInPercent: Int,
    private val screen: Screen
) : PaneSize {
    override fun width(): Int {
        return screen.terminalSize.columns * widthInPercent / 100
    }

    override fun height(): Int {
        return screen.terminalSize.rows
    }
}

interface WithRenameProcessing {
    fun processRename(): Unit
}

//todo: add blank pane.class  to clean spaces between panes
interface NavigationPane {
    fun next(): Unit
    fun prev(): Unit
}

context(WithName<A>)
abstract class AbstractListNavigationPane<A : WithRename>(
    _items: MutableList<A>,
    private val screen: Screen,
    private val size: PaneSize,
    private val shift: PaneShift
) : NavigationPane, WithRenameProcessing {
    protected abstract var cursor: TerminalPosition
    protected var current: A? = null

//    private var startOffset: Int = 0 //todo: is neccessary for screen scrolling?

    open var items: MutableList<A> =
        _items.let { elems ->
            it = elems.listIterator()
            if (it.hasNext()) {
                current = it.next()
            }
            elems
        }
        get() = field
        set(newItems) {
            field = newItems
            it = field.listIterator()
            if (it.hasNext()) {
                current = it.next()
            }
        }

    protected var it: ListIterator<A>

    open fun draw() {
        if (items.isEmpty()) {
            printText("There is empty", cursor)
            return
        }

        val drawCursor = shift.offset()

        for (rowIdx in 0 until size.height()) {
            if (rowIdx < items.size) {
                if (items[rowIdx] == current) {
                    printText(
                        items[rowIdx].name(),
                        drawCursor.withRelativeRow(rowIdx),
                        background = TextColor.Factory.fromString("#add8e6") //todo: add to config
                    )
                } else {
                    printText(items[rowIdx].name(), drawCursor.withRelativeRow(rowIdx))
                }
            } else {
                printText("", drawCursor.withRelativeRow(rowIdx))
            }
        }
    }

    private fun printText(
        text: String,
        start: TerminalPosition,
        foreground: TextColor = TextColor.ANSI.DEFAULT,
        background: TextColor = TextColor.ANSI.DEFAULT
    ) {
        val prepText = TextCharacter.fromString(text, foreground, background)

        for (idx in (0 until size.width())) {
            if (idx < size.width() - 1 && idx < prepText.size) {
                screen.setCharacter(start.withRelativeColumn(idx), prepText[idx])
            } else if (idx < prepText.size) {
                screen.setCharacter(start.withRelativeColumn(idx), prepText[idx].withCharacter('~'))
            } else {
                screen.setCharacter(start.withRelativeColumn(idx), TextCharacter.DEFAULT_CHARACTER)
            }
        }
    }

    override fun next() {
        if (!it.hasNext()) {
            return
        }

        var newCurrent = it.next()
        if (current == newCurrent) { // hint: bidirect iterator specific of calls next + prev results in same element
            if (!it.hasNext()) return
            newCurrent = it.next()
        }
        if (!it.hasPrevious()) {
            it.next()
        }

        cursor = cursor.withRelativeRow(1)
        current = newCurrent
    }

    override fun prev() {
        if (!it.hasPrevious()) {
            return
        }

        var newCurrent = it.previous()
        if (current == newCurrent) {
            if (!it.hasPrevious()) return
            newCurrent = it.previous()
        }
        if (!it.hasNext()) {
            it.previous()
        }
        cursor = cursor.withRelativeRow(-1)
        current = newCurrent
    }

    override fun processRename() {
        if (current == null) return

        var interrupted = false

        var newName = StringBuilder(current!!.name())

        while (!interrupted) {
            screen.pollInput()
                ?.let { res ->
                    if (res.keyType == KeyType.Enter) {
                        println("here is Enter inside Pane")
                        current!!.rename(newName.toString())
                        interrupted = true
                    } else if (res.keyType == KeyType.Escape) {
                        draw()
                        screen.refresh()
                        return
                    } else if (res.keyType == KeyType.Backspace) {
                        if (newName.toString().length > 0) {
                            newName = StringBuilder(newName.dropLast(1))
                            printText(newName.toString(), cursor)
                        }
                    } else {
                        newName.append(res.character.toString())
                        printText(newName.toString(), cursor)
                        println("cursor is ${cursor}")
                    }
                    screen.refresh()
                } ?: continue
        }
    }
}

context(WithName<Project>)
class ProjectPane(
    private val projects: MutableList<Project>,
    private val taskPane: TaskPane,
    private val screen: Screen,
    private var size: PaneSize,
    private var shift: PaneShift
) : AbstractListNavigationPane<Project>(projects, screen, size, shift) {

    override var cursor: TerminalPosition = TerminalPosition.TOP_LEFT_CORNER
        get() = field

    companion object {
        fun init(
            projects: MutableList<Project>,
            taskPane: TaskPane,
            screen: Screen,
            size: PaneSize,
            shift: PaneShift
        ): ProjectPane {
            with(projectWithNameInst) {
                val pane = ProjectPane(projects, taskPane, screen, size, shift)
                taskPane.items = projects.getOrNull(0)?.tasks() ?: mutableListOf()
                return pane
            }
        }
    }

    override fun draw() {
        println("Draw project pane")
        super.draw()
        taskPane.draw()
    }

    override fun next() {
        super.next()
        taskPane.items = current?.tasks().orEmpty().toMutableList()

        draw()
    }

    override fun prev() {
        super.prev()
        taskPane.items = current?.tasks().orEmpty().toMutableList()

        draw()
    }
}

context(WithName<Task>)
class TaskPane(
    private val screen: Screen,
    private val positionShift: PaneShift,
    private val contentPane: ContentPane<Task>,
    private val size: PaneSize
) : AbstractListNavigationPane<Task>(mutableListOf(), screen, size, positionShift) {
    companion object {
        fun init(
            screen: Screen,
            contentPane: ContentPane<Task>,
            size: PaneSize,
            shift: PaneShift
        ): TaskPane {
            with(taskWithNameInst) {
                val pane = TaskPane(
                    screen,
                    shift,
                    contentPane,
                    size
                )
                return pane

            }

        }
    }

    override var cursor: TerminalPosition = positionShift.offset()
        get() = field
        set(value) {
            field = value
        }

    override var items: MutableList<Task>
        get() = super.items
        set(value) {
            super.items = value
            val newOffset = positionShift.offset()
            cursor = newOffset
        }


    override fun next() {
        super.next()

        draw()
        contentPane.source = current
        with(taskWithContent) {
            contentPane.draw()
        }
    }

    override fun prev() {
        super.prev()

        draw()
        contentPane.source = current
        with(taskWithContent) {
            contentPane.draw()
        }
    }
}

class ContentPane<A>(
    private val screen: Screen,
    private val size: PaneSize,
    positionShift: PaneShift
) {
    var source: A? = null
        set(value) {
            field = value
        }

    var cursor: TerminalPosition = positionShift.offset()
        get() = field
        set(value) {
            field = value
        }

    context(WithContent<A>)
    fun draw(): Unit {
        val content = source.content()
        for (idx in 0 until size.height()) {
            if (content.hasNext()) {
                printText(content.next(), cursor.withRelativeRow(idx))
            } else {
                printText("", cursor.withRelativeRow(idx))
            }

        }
    }

    private fun printText(
        text: String,
        start: TerminalPosition,
        foreground: TextColor = TextColor.ANSI.DEFAULT,
        background: TextColor = TextColor.ANSI.DEFAULT
    ) {
        val prepText = TextCharacter.fromString(text, foreground, background)

        for (idx in (0 until size.width())) {
            if (idx < size.width() - 1 && idx < prepText.size) {
                screen.setCharacter(start.withRelativeColumn(idx), prepText[idx])
            } else if (idx < prepText.size) {
                screen.setCharacter(start.withRelativeColumn(idx), prepText[idx].withCharacter('~'))
            } else {
                screen.setCharacter(start.withRelativeColumn(idx), TextCharacter.DEFAULT_CHARACTER)
            }
        }
    }
}


