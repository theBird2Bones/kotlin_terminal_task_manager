package tira

import tira.persistance.domain
import java.io.File

class Tira private constructor(val projects: List<domain.Project>) {
    fun run() {
        println(
            "here is projects: ${projects}"
        )
    }

    companion object {
        // find app root dir and init instance
        fun init(): Tira {
            val root = File(System.getProperty("user.home") + "/.tira")
            if (!root.exists()) {
                root.mkdir()
                println("created dir")
            }
            return Tira(
                root
                    .listFiles()
                    .orEmpty()
                    .map { f ->
                        domain.Project.Companion.Dir.from(f)
                    }
            )
        }
    }
}
