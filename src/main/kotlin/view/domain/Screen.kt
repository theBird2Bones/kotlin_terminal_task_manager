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
    private val tf = DefaultTerminalFactory()
    private val screen = tf.createScreen()

    private var currentViewMode = ViewMode.Projects

    //todo: replace Task on smt with specific property
    //todo: replace nailed TP with virtual space and dynamic sizing
    private val contentPane = ContentPane<Task>(screen, TerminalPosition(30, 0))
    private val taskPane = TaskPane.init(
        screen,  contentPane
    )

    private val projectPane = ProjectPane.init(projects, taskPane, screen)


    private val activePane =
        mapOf(
            ViewMode.Projects to projectPane,
            ViewMode.Tasks to taskPane
        )

    fun start() {
        screen.startScreen()
        screen.refresh()

        while (true) {
            val input = screen.pollInput() ?: continue

            //todo: add navigation profile with keys mapping
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

//todo: refactor it
interface NavigationPane {
    fun next(): Unit
    fun prev(): Unit
}

interface WithName<A> {
    fun A.name(): String
}

val projectWithNameInst = object : WithName<Project> {
    override fun Project.name(): String = name()
}
val taskWithNameInst = object : WithName<Task> {
    override fun Task.name(): String = name()
}

interface WithContent<A> {
    fun A?.content(): Iterator<String>
}

val taskWithContent = object : WithContent<Task> {
    override fun Task?.content(): Iterator<String> = this?.content() ?: listOf("<empty>").iterator()

}

context(WithName<A>)
abstract class AbstractListNavigationPane<A>(
    _items: MutableList<A>,
    private val screen: Screen
) : NavigationPane {
    protected abstract var cursor: TerminalPosition
    protected var current: A? = null

    open var items = _items
        get() = field
        set(newItems) {
            field = newItems
            it = field.listIterator()
//            if(it.hasNext()){
//                // logic because of start from first element in list
//                current = it.next()
//            }
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
) : AbstractListNavigationPane<Project>(projects, screen) {
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
                //todo: better logic for init drawing and total init
                taskPane.items = projects.getOrNull(0)?.tasks() ?: mutableListOf()
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
    private val positionShift: TerminalPosition,
    private val contentPane: ContentPane<Task>
) : AbstractListNavigationPane<Task>(mutableListOf(), screen) {
    companion object {
        fun init(
            screen: Screen,
            contentPane: ContentPane<Task>
        ): TaskPane {
            with(taskWithNameInst) {
                val pane = TaskPane(
                    screen,
                    TerminalPosition(10, 0), // todo: replace after assemble pane size
                    contentPane
                )
                pane.draw()
                return pane

            }

        }
    }

    override var cursor: TerminalPosition = positionShift
        get() = field
        set(value) {
            field = value
        }

    override var items: MutableList<Task>
        get() = super.items
        set(value) {
            super.items = value
            cursor = positionShift

            with(taskWithContent) {
                contentPane.draw()
            }
        }


    override fun next() {
        //in case there is next element, current must be not null
        if (!it.hasNext()) {
            return
        }

        TextCharacter.fromString(current!!.name())
            .forEachIndexed { idx, ch ->
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
                screen.setCharacter(
                    cursor.withRelativeColumn(idx),
                    ch
                )
            }

        contentPane.source = current
        with(taskWithContent) {
            contentPane.draw()
        }

        screen.refresh(Screen.RefreshType.AUTOMATIC) //todo: moveout screen refreshing out of here to call site inside map
        //todo: refresh for tasks and content
    }

    //todo: fix going out of screen when first move is prev()
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

        contentPane.source = current
        with(taskWithContent) {
            contentPane.draw()
        }

        screen.refresh(Screen.RefreshType.AUTOMATIC)
    }
}

class ContentPane<A>(
    private val screen: Screen,
    positionShift: TerminalPosition
) {
    var source: A? = null
        set(value) {
            field = value
        }

    var cursor: TerminalPosition = positionShift
        get() = field
        set(value) {
            field = value
        }

    context(WithContent<A>)
    fun draw(): Unit {
        var rowIdx = 0;
        source.content()
            .forEach {
                TextCharacter.fromString(it)
                    .forEachIndexed { idx, ch ->
                        screen.setCharacter(
                            cursor.withRelative(idx, rowIdx),
                            ch
                        )
                    }
                rowIdx++;
            }
    }
}

enum class ViewMode {
    Projects, Tasks
}

