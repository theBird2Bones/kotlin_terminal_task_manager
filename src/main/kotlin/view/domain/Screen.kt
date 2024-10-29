package tira.view.domain

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.screen.Screen
import tira.persistance.domain.Project
import tira.persistance.domain.SourceInfo


import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import tira.persistance.domain.Task

class Screen(
    projects: MutableList<Project>
) {
    //todo: there is listIterator with insert and bidi moving
    private val tf = DefaultTerminalFactory()
    private val screen = tf.createScreen()

    private var currentViewMode = ViewMode.Projects

    private val activePane =
        TaskPane
            .init(
                screen,
                projects.getOrNull(0)?.tasks() ?: mutableListOf()
            ).let {
                mapOf(
                    ViewMode.Projects to ProjectPane.init(projects, it, screen),
                    ViewMode.Tasks to it
                )
            }


//    private var mode: ScreenModeState

    fun start() {
        println("Start screen")
        screen.startScreen()
        screen.refresh()

        while (true) {
            val input = screen.pollInput() ?: continue

            when (input.character) {
                'j' -> activePane.get(currentViewMode)?.next() ?: println("nothing to show")
                'k' -> activePane.get(currentViewMode)?.prev() ?: println("nothing to show")
                'h' -> currentViewMode = ViewMode.Projects
                'l' -> currentViewMode = ViewMode.Tasks
            }

            //todo: redraw if size changed
        }
    }
}

interface Pane {
    fun next(): Unit
    fun prev(): Unit
}

interface WithName<A> {
    fun A.name(): String
}

val projectWithNameInst = object : WithName<Project> {
    override fun Project.name(): String = this.name()
}
val taskWithNameInst = object : WithName<Task> {
    override fun Task.name(): String = this.name()
}

context(WithName<A>)
abstract class AbstractListPane<A>(
    _items: MutableList<A>,
    private val screen: Screen
) : Pane {
    protected abstract var cursor: TerminalPosition
    protected var current: A? = null

    open var items = _items
        get() = field
        set(newItems) {
            field = newItems
            it = field.listIterator()
        }

    protected var it = items.listIterator()

    fun draw() {
        if (items.isEmpty()) {
            TextCharacter.fromString("There is empty")
                .forEachIndexed { idx, ch ->
                    screen.setCharacter(cursor.withRelativeColumn(idx), ch)
                }
            return
        }

        items.forEachIndexed { rowIdx, item ->
            val prepString = TextCharacter.fromString(item.name())
            if (rowIdx == 0) {
                current = it.next() //todo: find better place
                prepString
                    .forEachIndexed { idx, ch ->
                        screen.setCharacter(
                            cursor.withRelative(idx, rowIdx),
                            ch
                                .withBackgroundColor(TextColor.Factory.fromString("#add8e6")) //light blue
                                .withForegroundColor(TextColor.ANSI.DEFAULT)
                        )
                    }
            } else {
                prepString
                    .forEachIndexed { idx, ch ->
                        screen.setCharacter(
                            cursor.withRelative(idx, rowIdx),
                            ch
                        )
                    }
            }
        }
    }
}

context(WithName<Project>)
class ProjectPane(
    private val projects: MutableList<Project>,
    private val taskPane: TaskPane,
    private val screen: Screen,
//    private var size: TerminalSize
) : AbstractListPane<Project>(projects, screen) {
    override var cursor: TerminalPosition = TerminalPosition.TOP_LEFT_CORNER
        get() = field
//    protected var cursor: TerminalPosition = TerminalPosition.TOP_LEFT_CORNER

    companion object {
        fun init(
            projects: MutableList<Project>,
            taskPane: TaskPane,
            screen: Screen,
//            size: TerminalSize
        ): ProjectPane {
            with(projectWithNameInst) {
                val pane = ProjectPane(
                    projects,
                    taskPane,
                    screen
//                , size
                )
                pane.draw()
                return pane
            }
        }
    }

    //todo: invalidate first position when add first project after nothing. Next come from 1
    override fun next() {
        println("ProjectPane.next")

        //in case there is next element, current must be not null
        if (!it.hasNext()) {
            return
        }

        TextCharacter.fromString(current!!.name())
            .forEachIndexed { idx, ch ->
                screen.setCharacter(cursor.withRelativeColumn(idx), ch)
            }

        cursor = cursor.withRelativeRow(1)
        if (!it.hasPrevious()) {
            it.next() // todo: will break on 1 element
            //todo: implement bidirect linked list
        }
        current = it.next()

        TextCharacter.fromString(
            current!!.name(),
            TextColor.ANSI.DEFAULT,
            TextColor.Factory.fromString("#add8e6") //light blue
        )
            .forEachIndexed { idx, ch ->
                screen.setCharacter(
                    cursor.withRelativeColumn(idx),
                    ch
                )
            }

        taskPane.items = current?.tasks().orEmpty().toMutableList()
        taskPane.draw()
        screen.refresh(Screen.RefreshType.AUTOMATIC)
        //todo: refresh for tasks and content
    }

    override fun prev() {
        println("ProjectPane.prev")

        if (!it.hasPrevious()) {
            return
        }

        // restore old current to default state
        TextCharacter.fromString(current!!.name())
            .forEachIndexed { idx, ch ->
                screen.setCharacter(
                    cursor.withRelativeColumn(idx),
                    ch
                )
            }
        cursor = cursor.withRelativeRow(-1)
        if (!it.hasNext()) {
            it.previous() // todo: will break on 1 element
        }
        current = it.previous()

        TextCharacter.fromString(
            current!!.name(),
            TextColor.ANSI.DEFAULT,
            TextColor.Factory.fromString("#add8e6") //light blue
        )
            .forEachIndexed { idx, ch ->
                screen.setCharacter(
                    cursor.withRelativeColumn(idx),
                    ch
                )
            }

        taskPane.items = current?.tasks().orEmpty().toMutableList()
        taskPane.draw()
        screen.refresh(Screen.RefreshType.AUTOMATIC)
    }
}

context(WithName<Task>)
class TaskPane(
    private val screen: Screen,
    initTasks: MutableList<Task>,
    private val positionShift: TerminalPosition
) : AbstractListPane<Task>(initTasks, screen) {
    companion object {
        fun init(
            screen: Screen,
            initTasks: MutableList<Task>
        ): TaskPane {
            with(taskWithNameInst) {
                val pane = TaskPane(
                    screen,
                    initTasks,
                    TerminalPosition(10, 0) // todo: replace after assemble pane size
                )
                pane.draw()
                return pane

            }

        }
    }

    override var cursor: TerminalPosition = positionShift
        get() = field
        set(value) {
            //todo: подумать почему разъезжается указатель
            field = value
        }

    override var items: MutableList<Task>
        get() = super.items
        set(value) {
            super.items = value
            cursor = positionShift
        }


    override fun next() {
        //in case there is next element, current must be not null
        println("cursor before: ${cursor}")
        if (!it.hasNext()) {
            return
        }

        TextCharacter.fromString(current!!.name())
            .forEachIndexed { idx, ch ->
                println("traversed: ${cursor.withRelativeColumn(idx)}")
                screen.setCharacter(cursor.withRelativeColumn(idx), ch)
            }

        cursor = cursor.withRelativeRow(1)
        println("cursor after: ${cursor}")
        if (!it.hasPrevious()) {
            it.next() // todo: will break on 1 element
            //todo: implement bidirect linked list
        }
        current = it.next()

        TextCharacter.fromString(
            current!!.name(),
            TextColor.ANSI.DEFAULT,
            TextColor.Factory.fromString("#add8e6") //light blue
        )
            .forEachIndexed { idx, ch ->
                println("traversed: ${cursor.withRelativeColumn(idx)}")
                screen.setCharacter(
                    cursor.withRelativeColumn(idx),
                    ch
                )
            }

        screen.refresh(Screen.RefreshType.AUTOMATIC) //todo: moveout screen refreshing out of here to call site inside map
        //todo: refresh for tasks and content
    }

    override fun prev() {
        if (!it.hasPrevious()) {
            return
        }

        // restore old current to default state
        TextCharacter.fromString(current!!.name())
            .forEachIndexed { idx, ch ->
                screen.setCharacter(
                    cursor.withRelativeColumn(idx),
                    ch
                )
            }
        cursor = cursor.withRelativeRow(-1)
        if (!it.hasNext()) {
            it.previous() // todo: will break on 1 element
        }
        current = it.previous()

        TextCharacter.fromString(
            current!!.name(),
            TextColor.ANSI.DEFAULT,
            TextColor.Factory.fromString("#add8e6") //light blue
        )
            .forEachIndexed { idx, ch ->
                screen.setCharacter(
                    cursor.withRelativeColumn(idx),
                    ch
                )
            }

        screen.refresh(Screen.RefreshType.AUTOMATIC)
    }

}

enum class ViewMode {
    Projects, Tasks
}

interface ScreenMode
interface Navigation : ScreenMode {
    fun next(): Unit // should be Pane
    fun prev(): Unit

    //    fun edit():
    fun info(): SourceInfo
}

interface Edit : ScreenMode

class ProjectsNavigation() : Navigation {
    override fun next() {
        TODO("Not yet implemented")
    }

    override fun prev() {
        TODO("Not yet implemented")
    }

    override fun info(): SourceInfo {
        TODO("Not yet implemented")
    }
}

class TasksNavigation() : Navigation {
    override fun next() {
        TODO("Not yet implemented")
    }

    override fun prev() {
        TODO("Not yet implemented")
    }

    override fun info(): SourceInfo {
        TODO("Not yet implemented")
    }
}

class ProjectEdit : Edit
class TaskEdit : Edit
