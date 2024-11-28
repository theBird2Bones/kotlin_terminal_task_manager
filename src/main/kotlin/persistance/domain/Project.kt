package tira.persistance.domain

import tira.persistance.domain.newtypes.*
import tira.predef.props.WithProperties
import tira.predef.props.WithRename
import java.nio.file.Files
import java.nio.file.Path

interface Project : WithRename, WithProperties {
    fun name(): String
    fun tasks(): MutableList<Task> //add task handler to add another tasks via that class
    override fun rename(newName: String): Unit
    fun createTask(name: String): Unit
    fun delete(task: Task): Unit

    override fun props(): List<Property> = listOf() //todo
}

//todo: caching Decorator
class Dir private constructor(
    private val source: ValidatedDirectory
) : Project {
    private val _name = DirectoryName(source)
    private val _tasks: MutableList<Task> = Files.list(
        Path.of(
            source.underlying.absolutePath()
        )
    )
        .toList()
        .map { p ->
            val file = ValidatedFile.from(PathSource.from(p))
            FileTask
                .from(
                    DependentSource.from(file, source) //todo: what with nested folders for project?
                )
        }
        .toMutableList()

    override fun toString(): String = "Dir(${source.underlying.absolutePath()})"

    override fun tasks(): MutableList<Task> = _tasks

    override fun name(): String = _name.name()

    override fun rename(newName: String) = _name.rename(newName)

    override fun createTask(name: String) {
        val task = FileTask.create(name, source)
        _tasks.add(task)
    }

    override fun delete(task: Task) {
        println("gonna remove task ${task}")
        println("tasks before ${tasks()}")
        _tasks.remove(task)
        task.delete()
        println("tasks after ${tasks()}")

    }

    companion object {
        fun from(dir: Source): Project {
            val directory = ValidatedDirectory.from(dir)

            println("make project for ${dir.absolutePath()}")

            return Dir(directory)
        }
    }
}
