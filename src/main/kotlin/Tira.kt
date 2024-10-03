package tira

import tira.persistance.domain
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.exists

class Tira private constructor(val projects: List<domain.Project>) {
    fun run() {
        val project = projects[0]
        println("want rename project")
        project.rename("proj2")

        println("want rename")
        val task = projects[0].tasks()[0]

        println("name before: ${task.name()}")
        println("rename it")
        task.rename(UUID.randomUUID().toString())
        println("name after: ${task.name()}")

        println("---------------")



    }

    companion object {
        fun init(): Tira {
            val root = Path.of(System.getProperty("user.home"), ".tira")
            if (!root.exists()) {
                Files.createDirectory(root)
                println("created dir")
            }
            return Tira(
                Files
                    .list(root)
                    .map { p ->
                        domain.Project.Companion.Dir.from(
                            domain.Source.Companion.PathSource.from(p)
                        )
                    }
                    .toList()
                    .orEmpty()
            )
        }
    }
}
