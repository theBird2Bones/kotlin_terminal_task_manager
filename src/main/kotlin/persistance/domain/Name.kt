package tira.persistance.domain

import tira.persistance.domain.newtypes.*

abstract class Name protected constructor(
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
        return _name.replace(".md", "")
    }

    fun rename(newName: String): Unit = synchronized(this) {
        _changed = true

        val anotherName = unsafeMkName(newName)

        source.rename(anotherName)
        println("new path ${source.absolutePath()}")
    }

    protected abstract fun unsafeMkName(newName: String): String

}

class FileName(source: ValidatedFile) : Name(source.underlying) {
    override fun unsafeMkName(newName: String): String = "${newName}.md"
}

class DirectoryName(source: ValidatedDirectory) : Name(source.underlying) {
    override fun unsafeMkName(newName: String): String = newName
}
