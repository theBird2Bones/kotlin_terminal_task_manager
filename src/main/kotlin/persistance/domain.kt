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

        companion object {
            //todo: newtype for dir and file. can do it via inline classes
            //todo: how to move files from directories within rename?
            //todo: caching Decorator
            class Dir private constructor(
                private val source: ValidatedDirectory
            ) : Project {
                private val _name = Name.Companion.DirectoryName(source)
                private val _tasks: List<Task> = Files.list(
                    Path.of(
                        source.underlying.absolutePath()
                    )
                )
                    .toList()
                    .orEmpty()
                    .map { p ->
                        Task.Companion.FileTask.from(
                            Source.from(p)
                        )
                    }

                override fun toString(): String = "Dir(${source.underlying.absolutePath()})"

                override fun tasks(): List<Task> = _tasks

                override fun name(): String = _name.name()

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

    //todo: check if works with projects
    //todo: фабричный метод на файлы и проекты

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

            val anotherName = unsafeMkName("${source.parent()}/${newName}")

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
        fun props(): List<Property>
        fun name(): String
        fun rename(newName: String): Unit

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
    //todo: must be interface
    //todo: add another class kinda DependentSource to propogate path update and match them with actual file placement
    class Source private constructor(
        private var path: Path
    ) {
        fun isFile(): Boolean = path.isRegularFile()
        fun isDirectory(): Boolean = path.isDirectory()
        fun name(): String = path.fileName.toString()
        fun parent(): String = path.parent.absolute().toString()
        fun rename(name: String) = synchronized(this) {
            val newPath = Path(name)
            Files.move(path, newPath)
            path = newPath
        }

        fun absolutePath() = path.toString()

        companion object {
            fun from(path: Path): Source {
                if (!path.exists()) {
                    throw Exception("Non existing source")
                }
                return Source(path)
            }
        }
    }
}
