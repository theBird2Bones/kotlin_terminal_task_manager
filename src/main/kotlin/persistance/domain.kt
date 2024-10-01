package tira.persistance

import java.io.File
import java.time.LocalDateTime

object domain {
    interface Project {
        fun tasks(): List<Task>

        companion object {
            //todo: newtype for dir and file
            //todo: how to move files from directories within rename?
            //todo: caching Decorator
            class Dir private constructor(val source: File) : Project {
                override fun toString(): String = "Dir(${source.absolutePath})"
                override fun tasks(): List<Task> {
                    return source
                        .listFiles()
                        .orEmpty()
                        .map { f ->
                            Task.Companion.FileTask.from(f)
                        }
                }

                companion object {
                    fun from(dir: File): Project {
                        if (!dir.exists()) {
                            throw Throwable("dir ${dir.absolutePath} doesn't exists") //todo: replace with TiraError
                        }
                        println("make project for ${dir.absolutePath}")
                        return Dir(dir)
                    }
                }
            }

        }
    }

    interface Task {
        fun props(): List<Property>
        fun name(): String

        companion object {
            class FileTask private constructor(val source: File) : Task { // very scala like. todo: #todo1
                override fun name(): String = source.name  //todo: мб свой декоратор для такого

                //todo: держать ли дескриптор открытым?
                override fun props(): List<Property> {
                    TODO(
                        """ Имплементировать с помощью prettier для сбора меты """.trimIndent()
                    )
                }

                companion object {
                    //only for existing files
                    fun from(file: File): Task {
                        if (!file.exists()) {
                            throw Throwable("source doesn't exists") //todo: replace with TiraError
                        }
                        println("make task for ${file.absolutePath}")
                        return FileTask(file)
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
            data class TextProp(val value: String) : Property {
                override fun repr(): String = value
            }

            data class IntProp(val value: Int) : Property {
                override fun repr(): String = value.toString()
            }

            data class DateProp(val value: LocalDateTime) : Property {
                override fun repr(): String = value.toString()
            }
        }
    }

}
