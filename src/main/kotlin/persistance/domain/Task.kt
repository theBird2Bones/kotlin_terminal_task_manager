package tira.persistance.domain

import tira.persistance.domain.newtypes.*
import tira.predef.props.WithRename
import java.nio.file.Files

import kotlin.io.path.*

interface Task: WithRename {
    //todo: add content fetching
    fun props(): List<Property>
    fun name(): String
    override fun rename(newName: String): Unit
    fun delete(): Unit //smt like destroy

    fun content(): Iterator<String>
}

class FileTask private constructor(
    private val source: ValidatedFile
) : Task { // very scala like. todo: #todo1
    private val _name = FileName(source)

    //todo: add class to invalidate changes for names and cache last result
    override fun name(): String = _name.name()

    override fun rename(newName: String) {
        _name.rename(newName)
    }

    override fun delete(): Unit {
        Files.delete(Path(source.underlying.absolutePath()))
    }

    override fun content(): Iterator<String> {
        //todo: replace getting abs path from source to Path class
        return Files.readAllLines(Path(source.underlying.absolutePath())).iterator()
    }

    override fun equals(other: Any?): Boolean {
        return (other is Task) && other.name() == this.name()
    }

    //todo: держать ли дескриптор открытым?
    override fun props(): List<Property> {
        TODO(
            """ Имплементировать с помощью prettier для сбора меты """.trimIndent()
        )
    }

    companion object {
        //only for existing files
        fun from(file: Source): Task {
            val validatedFile = ValidatedFile.from(file)

            println("make task for ${file.absolutePath()}")

            return FileTask(validatedFile)
        }

        fun create(name: String, projSource: ValidatedDirectory): Task {
            val file = Path(projSource.underlying.absolutePath(), "${name}.md")
            file.createFile() //fixme: report if exists

            return FileTask(
                ValidatedFile.from(
                    DependentSource.from(
                        ValidatedFile.from(PathSource(file)),
                        projSource
                    )
                )
            )
        }
    }

}
