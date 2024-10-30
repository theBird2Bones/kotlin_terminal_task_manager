package tira.view.domain

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

import tira.persistance.domain.Project
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
        screen, contentPane
    )

    private val projectPane = ProjectPane.init(projects, taskPane, screen)


    private val activePane =
        mapOf(
            ViewMode.Projects to projectPane,
            ViewMode.Tasks to taskPane
        )

    fun start() {
        screen.startScreen()
        projectPane.draw()
        screen.refresh()

        while (true) {
            val input = screen.pollInput() ?: continue

            //todo: add navigation profile with keys mapping
            //todo: add squashing for near events
            when (input.character) {
                //ask: Работают ли корутины с ванильным synchronized? там же один тред стучится в рекурсивный мьюетекс
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
                'l' -> currentViewMode = ViewMode.Tasks
            }

            //todo: redraw if size changed
        }
    }
}
