package tira.view.domain

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import tira.persistance.domain.*
import tira.persistance.domain.newtypes.ValidatedDirectory

import tira.predef.props.*
import tira.predef.std.VisibleElements
import tira.predef.std.VisibleListElements
import java.nio.file.Path
import kotlin.io.path.absolutePathString

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
    fun processRename(): RenameProcessing?
}

//todo: add blank pane.class  to clean spaces between panes
interface NavigationPane {
    fun next(): Unit
    fun prev(): Unit
}

context(WithName<A>)
abstract class AbstractListNavigationPane<A>(
    open var items: VisibleElements<A>,
    private val screen: Screen,
    private val size: PaneSize,
    private val shift: PaneShift,
    private val propsStyle: Map<Class<out Property>, (Property) -> SGR?>
) : NavigationPane, WithRenameProcessing
        where A : WithProperties,
              A : WithRename {
    protected abstract var cursor: TerminalPosition

//    private var startOffset: Int = 0 //todo: is neccessary for screen scrolling?

    open fun draw() {
        if (items.isEmpty()) {
            printText("<empty>", cursor)
            for (rowIdx in (cursor.row + 1 until size.height())) {
                printText("", cursor.withRow(rowIdx))
            }
            return
        }

        val drawCursor = shift.offset()

        val elements = items.elements()

        for (rowIdx in 0 until size.height()) {
            if (rowIdx < elements.size) {

                val current = elements[rowIdx]
                if (current == items.current()) {
                    printText(
                        current.name(),
                        drawCursor.withRelativeRow(rowIdx),
                        background = TextColor.Factory.fromString("#add8e6"), //todo: add to config
                        modifiers = gatherStyle(current.props())
                    )
                } else {
                    printText(
                        elements[rowIdx].name(),
                        drawCursor.withRelativeRow(rowIdx),
                        modifiers = gatherStyle(current.props())
                    )
                }
            } else {
                printText("", drawCursor.withRelativeRow(rowIdx))
            }
        }
    }

    private fun gatherStyle(props: List<Property>): List<SGR> {
        val res = mutableListOf<SGR>()
        for (prop in props) {
            propsStyle
                .get(prop.javaClass)
                ?.let {
                    it(prop)
                        ?.let { res.add(it) }
                }
        }
        return res
    }

    fun printText(
        text: String,
        start: TerminalPosition,
        foreground: TextColor = TextColor.ANSI.DEFAULT,
        background: TextColor = TextColor.ANSI.DEFAULT,
        modifiers: List<SGR> = listOf()
    ) {
        val prepText = TextCharacter.fromString(text, foreground, background)

        for (idx in (0 until size.width())) {
            if (idx < size.width() - 1 && idx < prepText.size) {
                screen.setCharacter(
                    start.withRelativeColumn(idx),
                    prepText[idx]
                        .let {
                            if (modifiers.isEmpty()) it
                            else it.withModifiers(modifiers)
                        }
                )
            } else if (idx < prepText.size) {
                screen.setCharacter(
                    start.withRelativeColumn(idx),
                    prepText[idx]
                        .withCharacter('~')
                )
            } else {
                screen.setCharacter(start.withRelativeColumn(idx), TextCharacter.DEFAULT_CHARACTER)
            }
        }
    }

    override fun next() {
        if (!items.hasNext()) {
            return
        }

        items.next()
        cursor = cursor.withRelativeRow(1)
    }

    protected fun removeCurrent() {
        if (items.isEmpty()) return
        if (items.hasPrevious()) {
            cursor = cursor.withRelativeRow(-1)
        }
        items.remove()
    }

    override fun prev() {
        if (!items.hasPrevious()) {
            return
        }

        items.previous()
        cursor = cursor.withRelativeRow(-1)
    }

    override fun processRename(): RenameProcessing? {
        if (items.current() == null) return null

        var interrupted = false

        var newName = StringBuilder(items.current()!!.name())

        while (!interrupted) {
            screen.pollInput()
                ?.let { res ->
                    if (res.keyType == KeyType.Enter) {
                        println("here is Enter inside Pane")
                        items.current()!!.rename(newName.toString())
                        interrupted = true
                    } else if (res.keyType == KeyType.Escape) {
                        draw()
                        screen.refresh()
                        return RenameProcessing.Aborted
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

        return RenameProcessing.Succeed
    }

    abstract fun processDelete()

    abstract fun processElementCreation()

    abstract fun complete()
}

context(WithName<Project>)
class ProjectPane(
    private val root: Path,
    private val projects: VisibleElements<Project>,
    private val taskPane: TaskPane,
    private val screen: Screen,
    private var size: PaneSize,
    private var shift: PaneShift,
    private val propsStyle: Map<Class<out Property>, (Property) -> SGR?>
) : AbstractListNavigationPane<Project>(projects, screen, size, shift, propsStyle) {

    override var cursor: TerminalPosition = TerminalPosition.TOP_LEFT_CORNER
        get() = field

    companion object {
        val propsStyle = mapOf(
            Pair<Class<out Property>, (Property) -> SGR?>(
                CompletedProperty::class.java,
                { if (it.value() == "true") SGR.CROSSED_OUT else null }
            )
        )

        fun init(
            root: Path,
            projects: VisibleElements<Project>,
            taskPane: TaskPane,
            screen: Screen,
            size: PaneSize,
            shift: PaneShift
        ): ProjectPane {
            with(projectWithNameInst) {
                val pane = ProjectPane(root, projects, taskPane, screen, size, shift, propsStyle)
                taskPane.items = VisibleListElements(projects.current()?.tasks() ?: emptyList())
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
        taskPane.items = VisibleListElements(items.current()?.tasks()?.toList() ?: emptyList())

        draw()
    }

    override fun prev() {
        super.prev()
        taskPane.items = VisibleListElements(items.current()?.tasks()?.toList() ?: emptyList())

        draw()
    }

    override fun processDelete() {
        items.current()?.run {
            destruct()
            removeCurrent()
            taskPane.items = VisibleListElements(items.current()?.tasks()?.toList() ?: emptyList())
            draw()
            screen.refresh()
        }
    }

    override fun processElementCreation() {
        val tmp = InMemoryProject("")

        items.insert(tmp)
        next()
        screen.refresh()
        when (processRename()) {
            RenameProcessing.Aborted -> removeCurrent()

            RenameProcessing.Succeed -> {
                items.current()?.name()?.let { name ->
                    removeCurrent()
                    if (name != "") {
                        val projectPath = Path.of(root.absolutePathString(), name)
                        items.insert(
                            Dir.from(
                                ValidatedDirectory.create(projectPath).underlying
                            )
                        )
                        next()
                    }
                }
            }

            null -> return
        }

        draw()
        screen.refresh()
    }

    override fun complete() {
        items.current()?.toggleComplete()
        draw()
        screen.refresh()
    }
}

context(WithName<Task>)
class TaskPane(
    private val screen: Screen,
    private val positionShift: PaneShift,
    private val contentPane: ContentPane<Task>,
    private val size: PaneSize,
    private val propsStyle: Map<Class<out Property>, (Property) -> SGR?>
) : AbstractListNavigationPane<Task>(
    VisibleListElements(emptyList()),
    screen,
    size,
    positionShift,
    propsStyle
) {
    companion object {
        fun init(
            screen: Screen,
            contentPane: ContentPane<Task>,
            size: PaneSize,
            shift: PaneShift
        ): TaskPane {
            val propsStyle = mapOf(
                Pair<Class<out Property>, (Property) -> SGR?>(
                    CompletedProperty::class.java,
                    { if (it.value() == "true") SGR.CROSSED_OUT else null }
                )
            )
            with(taskWithNameInst) {
                val pane = TaskPane(
                    screen,
                    shift,
                    contentPane,
                    size,
                    propsStyle
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

    override var items: VisibleElements<Task>
        get() = super.items
        set(value) {
            super.items = value
            val newOffset = positionShift.offset()
            cursor = newOffset
        }


    override fun next() {
        super.next()

        draw()
        contentPane.source = items.current()
        with(taskWithContent) {
            contentPane.draw()
        }
    }

    override fun prev() {
        super.prev()

        draw()
        contentPane.source = items.current()
        with(taskWithContent) {
            contentPane.draw()
        }
    }

    override fun processDelete() {
        items.current()?.let { project?.delete(it) }

        items.remove()

        draw()
        contentPane.source = items.current()
        with(taskWithContent) {
            contentPane.draw()
        }
        screen.refresh()
    }

    override fun processElementCreation() {
        val tmp = InMemoryTask("")

        items.insert(tmp)
        next()
        screen.refresh()
        when (processRename()) {
            RenameProcessing.Aborted -> removeCurrent()

            RenameProcessing.Succeed -> {
                items.current()?.name()?.let { name ->
                    removeCurrent()
                    if (name != "") {
                        project?.createTask(name)?.let {
                            items.insert(it)
                            next()
                        }
                    }
                }
            }

            null -> return
        }

        draw()
        screen.refresh()
    }

    private var project: Project? = null

    fun setAccountableProject(project: Project) {
        this.project = project
    }

    override fun complete() {
        items.current()?.toggleComplete()
        draw()
        screen.refresh()
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


