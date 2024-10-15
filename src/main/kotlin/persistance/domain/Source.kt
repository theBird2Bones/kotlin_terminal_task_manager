package tira.persistance.domain

import tira.persistance.domain.newtypes.*
import java.nio.file.Files
import java.nio.file.Path

import kotlin.io.path.*

//todo: maybe here is another decorator for in mem changes with flushing in fs
interface Source {
    fun isFile(): Boolean
    fun isDirectory(): Boolean
    fun name(): String
    fun parent(): String
    fun rename(name: String): Unit
    fun absolutePath(): String
}

/**
 * Describes path relative to _source_ position
 */
class DependentSource private constructor(
    private val source: Source,
    private var path: Path
) : Source {
    override fun isFile(): Boolean = unsafePath().isRegularFile()
    override fun isDirectory(): Boolean = unsafePath().isDirectory()
    override fun name(): String = unsafePath().fileName.toString()
    override fun parent(): String = unsafePath().parent.toString()
    override fun rename(name: String) = synchronized(this) {
        val newPath = Path(name) //fixme: consider with no nested projects
        Files.move(
            unsafePath(),
            Path(source.absolutePath(), newPath.toString())
        )
        path = newPath
    }

    override fun absolutePath() = unsafePath().toString()

    private fun unsafePath() = Path(source.absolutePath(), path.toString())

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
