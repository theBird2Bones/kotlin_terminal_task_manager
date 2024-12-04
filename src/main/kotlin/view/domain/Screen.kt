package tira.view.domain

import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

import tira.persistance.domain.Project
import tira.persistance.domain.Task
import tira.predef.std.VisibleListElements

class Screen(
    projects: List<Project>
) {
    private val tf = DefaultTerminalFactory()
    private val screen = tf.createScreen()

    private var currentViewMode = ViewMode.Projects

    //todo: replace Task on smt with specific property
    //todo: replace nailed TP with virtual space and dynamic sizing
    private val contentPane = ContentPane<Task>(screen, DynamicPaneSize(60, screen), DynamicPaneShift(40, screen))
    private val taskPane = TaskPane.init(
        screen, contentPane, DynamicPaneSize(20, screen),
        DynamicPaneShift(20, screen)
    )

    private val projectPane = ProjectPane.init(
        VisibleListElements(projects),
        taskPane,
        screen,
        DynamicPaneSize(20, screen),
        DynamicPaneShift(0, screen)
    )


    private val activePane =
        mapOf(
            ViewMode.Projects to projectPane,
            ViewMode.Tasks to taskPane
        )

    fun start() {
        screen.terminal.setCursorVisible(false)
        screen.startScreen()
        projectPane.draw()
        screen.refresh()

        while (true) {
            if (screen.doResizeIfNecessary() != null) {
                screen.clear()
                projectPane.draw()
                screen.refresh()
            }

            val input = screen.pollInput() ?: continue

            //todo: add navigation profile with keys mapping
            //todo: add squashing for near events
            when (input.character) {
                'j' -> activePane.get(currentViewMode)
                    ?.next()
                    ?.let {
                        screen.refresh(Screen.RefreshType.AUTOMATIC)
                    }
                    ?: println("nothing to show")

                'k' -> activePane.get(currentViewMode)
                    ?.prev()
                    ?.let {
                        screen.refresh(Screen.RefreshType.AUTOMATIC)
                    }
                    ?: println("nothing to show")

                'h' -> currentViewMode = ViewMode.Projects
                'l' -> {
                    taskPane.setAccountableProject(
                        projectPane.items.current()!!
                    )
                    currentViewMode = ViewMode.Tasks
                }

                'r' -> activePane.get(currentViewMode)?.processRename()

                'N' -> if (input.isCtrlDown) {
                    activePane.get(currentViewMode)?.processElementCreation()
                } else continue

                'd' -> if (input.isCtrlDown) {
                    activePane.get(currentViewMode)?.processDelete()
                } else continue

                ' ' -> if(input.isCtrlDown) {
                    activePane.get(currentViewMode)?.complete()
                }

            }

            //todo: redraw if size changed
        }
    }
}
