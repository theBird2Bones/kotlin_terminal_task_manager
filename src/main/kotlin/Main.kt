package tira

import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.time.LocalDateTime

fun main() {
    val screen = DefaultTerminalFactory().createScreen()
    screen.startScreen()

    val gui = MultiWindowTextGUI(screen)

    createMainWindow(gui)

    gui.updateScreen()
    screen.stopScreen()
    screen.close()
}

fun createMainWindow(gui: MultiWindowTextGUI) {
    val window = BasicWindow()
    window.title = "Projects window"
    window.setHints(listOf(Window.Hint.FULL_SCREEN))

    val gridLayout = GridLayout(2)
    val contentPanel = Panel(gridLayout)

    val projectsPanel = Panel()
    projectsPanel.setLayoutManager(LinearLayout())


    val tasksPanel = Panel()
    tasksPanel.setLayoutManager(LinearLayout())

    projects.forEach { project ->
        projectsPanel.addComponent(createProjectButton(project) {
            tasksPanel.removeAllComponents()
            project.tasks().forEach { task ->
                tasksPanel.addComponent(createTaskPanel(task, gui))
            }
        })
    }


    contentPanel.addComponent(projectsPanel)
    contentPanel.addComponent(tasksPanel)

    window.component = contentPanel

    gui.addWindowAndWait(window)
}

fun createTaskPanel(task: Task, gui: MultiWindowTextGUI): Panel {
    val taskPanel = Panel()
    taskPanel.setLayoutManager(GridLayout(2))
    taskPanel.addComponent(Button(task.name()) { gui.addWindow(newWindowOnSelectedTask(task)) })
    task.props().forEach {
        taskPanel.addComponent(Label(it.repr()))
        taskPanel.addComponent(
            EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData())
        )
    }

    return taskPanel
}

fun newWindowOnSelectedTask(task: Task): BasicWindow {
    val window = BasicWindow()
    window.setHints(listOf(Window.Hint.FULL_SCREEN))

    val containerPanel = Panel()

    containerPanel.setLayoutManager(LinearLayout(Direction.VERTICAL))
    containerPanel.addComponent(Label(task.name()))
    task.props().forEach { containerPanel.addComponent(Label(it.repr())) }
    containerPanel.addComponent(Button("Close") { window.close() })

    window.component = containerPanel

    return window
}

fun createProjectButton(proj: Project, func: () -> Unit): Button = Button(proj.name(), func)

class Project(private val _name: String, private val _tasks: MutableList<Task> = mutableListOf()) {
    fun name() = _name
    fun tasks() = _tasks
}

class Task(private val _name: String, private val _props: MutableList<Property> = mutableListOf()) {
    fun name() = _name
    fun props() = _props
}

class Property(val value: String) {
    fun repr() = value
}

val projects = listOf(
    Project(
        "First project", mutableListOf(
            Task(
                "First task",
                mutableListOf(Property("prop1"), Property("123"), Property(LocalDateTime.now().toString()))
            ), Task(
                "Second task",
                mutableListOf(Property("prop2"), Property("234"), Property(LocalDateTime.now().toString()))
            ), Task(
                "Third task",
                mutableListOf(Property("prop3"), Property("345"), Property(LocalDateTime.now().toString()))
            )
        )
    ),
    Project("Second project", mutableListOf(Task("Second task"))),
    Project("Third project", mutableListOf(Task("Third task"))),
    Project("Fourth project", mutableListOf(Task("Fourth task"))),
    Project("Fifth project", mutableListOf(Task("Fifth task"))),
    Project("Sixth project", mutableListOf(Task("Sixth task"))),
    Project("Seventh project", mutableListOf(Task("Seventh task"))),
    Project("Eighth project", mutableListOf(Task("Eighth task"))),
    Project("Ninth project", mutableListOf(Task("Ninth task"))),
)