package tira.persistance

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.*

object domain {
    //todo: !!!!!!!!!!!!!!!!!!!11
    //todo: project should propogate his position in tasks
    //      in order to support project rename aka moving of folders
    //      and keeps data consistent
    //todo: !!!!!!!!!!!!!!!!!!!11
    interface Project {
        fun name(): String
        fun tasks(): List<Task> //add task handler to add another tasks via that class
        fun rename(newName: String): Unit
        fun createTask(name: String): Unit
        fun delete(task: Task): Unit

        companion object {
            //todo: caching Decorator
            class Dir private constructor(
                private val source: ValidatedDirectory
            ) : Project {
                private val _name = Name.Companion.DirectoryName(source)
                private val _tasks: MutableList<Task> = Files.list(
                    Path.of(
                        source.underlying.absolutePath()
                    )
                )
                    .toList()
                    .map { p ->
                        val file = ValidatedFile.from(Source.Companion.PathSource.from(p))
                        Task.Companion.FileTask
                            .from(
                                Source.Companion.DependentSource
                                    .from(file, source) //todo: what with nested folders for project?
                            )
                    }
                    .toMutableList()

                override fun toString(): String = "Dir(${source.underlying.absolutePath()})"

                override fun tasks(): List<Task> = _tasks

                override fun name(): String = _name.name()

                override fun rename(newName: String) = _name.rename(newName)

                override fun createTask(name: String) {
                    val task = Task.Companion.FileTask.create(name, source)
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

        }
    }

    abstract class Name private constructor(
        private val source: Source
    ) {
        private var _changed: Boolean
        private var _name: String

        //todo: спросить лучше ли разделять декларацию от инициализации
        init {
            _changed = false
            _name = source.name()
        }

        fun name(): String = synchronized(this) {
            if (_changed) {
                _changed = false
                _name = source.name()
            }
            return _name
        }

        fun rename(newName: String): Unit = synchronized(this) {
            _changed = true

            val anotherName = unsafeMkName(newName)

            source.rename(anotherName)
            println("new path ${source.absolutePath()}")
        }

        protected abstract fun unsafeMkName(newName: String): String

        companion object {
            class FileName(source: ValidatedFile) : Name(source.underlying) {
                override fun unsafeMkName(newName: String): String = "${newName}.md"
            }

            class DirectoryName(source: ValidatedDirectory) : Name(source.underlying) {
                override fun unsafeMkName(newName: String): String = newName
            }
        }
    }


    interface Task {
        //todo: add content fetching
        fun props(): List<Property>
        fun name(): String
        fun rename(newName: String): Unit
        fun delete(): Unit //smt like destroy

        companion object {
            class FileTask private constructor(
                private val source: ValidatedFile
            ) : Task { // very scala like. todo: #todo1
                private val _name = Name.Companion.FileName(source)

                //todo: add class to invalidate changes for names and cache last result
                override fun name(): String = _name.name()

                override fun rename(newName: String) {
                    _name.rename(newName)
                }

                override fun delete(): Unit {
                    Files.delete(Path(source.underlying.absolutePath()))
                }

                override fun equals(other: Any?): Boolean {
                    return other != null ||
                            if (other is Task) {
                                other.name() == this.name()
                            } else {
                                false
                            }
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

                        return Task.Companion.FileTask(
                            ValidatedFile.from(
                                Source.Companion.DependentSource.from(
                                    ValidatedFile.from(Source.Companion.PathSource(file)),
                                    projSource
                                )
                            )
                        )
                    }
                }

            }

        }
    }

    //todo: разные типы how
    //usage via `when`
    interface Property {
        fun repr(): String

        companion object {
            data class TextProp(
                val value: String
            ) : Property {
                override fun repr(): String = value
            }

            data class IntProp(
                val value: Int
            ) : Property {
                override fun repr(): String = value.toString()
            }

            data class DateProp(
                val value: LocalDateTime
            ) : Property {
                override fun repr(): String = value.toString()
            }
        }
    }


    @JvmInline
    value class ValidatedDirectory private constructor(val underlying: Source) {
        companion object {
            fun from(file: Source): ValidatedDirectory {
                if (!file.isDirectory()) {
                    throw Exception("${file.absolutePath()} is not a directory")
                }
                return ValidatedDirectory(file)
            }
        }

    }

    //todo: add method proxy in order to escape file becoming a directory
    @JvmInline
    value class ValidatedFile private constructor(val underlying: Source) {
        companion object {
            fun from(file: Source): ValidatedFile {
                if (!file.isFile()) {
                    throw Exception("${file.absolutePath()} is not a file")
                }
                return ValidatedFile(file)
            }
        }
    }

    //todo: maybe here is another decorator for in mem changes with flushing in fs

    interface Source {
        fun isFile(): Boolean
        fun isDirectory(): Boolean
        fun name(): String
        fun parent(): String
        fun rename(name: String): Unit
        fun absolutePath(): String

        companion object {
            /**
             * Describes path relative to _source_ position
             */
            class DependentSource private constructor(
                private val source: Source,
                private var path: Path
            ) : Source {
                override fun isFile(): Boolean = Path(source.absolutePath(), path.toString()).isRegularFile()
                override fun isDirectory(): Boolean = Path(source.absolutePath(), path.toString()).isDirectory()
                override fun name(): String = Path(source.absolutePath(), path.toString()).fileName.toString()
                override fun parent(): String = Path(source.absolutePath(), path.toString()).parent.toString()
                override fun rename(name: String) = synchronized(this) {
                    val newPath = Path(name) //fixme: consider with no nested projects
                    Files.move(
                        Path(source.absolutePath(), path.toString()),
                        Path(source.absolutePath(), newPath.toString())
                    )
                    path = newPath
                }

                override fun absolutePath() = Path(source.absolutePath(), path.toString()).toString()

                companion object {
                    fun from(file: ValidatedFile, directory: ValidatedDirectory): Source {
                        return DependentSource(
                            directory.underlying,
                            Path(file.underlying.name())
                        )
                    }
                }
            }

            class PathSource(
                private var path: Path
            ) : Source {
                override fun isFile(): Boolean = path.isRegularFile()
                override fun isDirectory(): Boolean = path.isDirectory()
                override fun name(): String = path.fileName.toString()
                override fun parent(): String = path.parent.absolute().toString()
                override fun rename(name: String) = synchronized(this) {
                    val newPath = Path(path.parent.toString(), name)
                    Files.move(path, newPath) //fixme: if exists, suggest to merge
                    path = newPath
                }

                override fun absolutePath() = path.toString()

                companion object {
                    fun from(path: Path): Source {
                        if (!path.exists()) {
                            throw Exception("Non existing source")
                        }
                        return PathSource(path)
                    }
                }
            }
        }
    }

}
