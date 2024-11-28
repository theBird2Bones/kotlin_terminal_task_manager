package tira.persistance.domain

import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.Renderer
import org.commonmark.renderer.text.TextContentRenderer
import tira.persistance.domain.newtypes.ValidatedDirectory
import tira.persistance.domain.newtypes.ValidatedFile
import tira.predef.props.WithProperties
import tira.predef.props.WithRename
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createFile

interface Task : WithRename, WithProperties {
    //todo: add content fetching
    override fun props(): List<Property>
    fun name(): String
    override fun rename(newName: String): Unit
    fun delete(): Unit //smt like destroy

    fun content(): Iterator<String>
    fun toggleComplete()
}

class FileTask private constructor(
    private val source: ValidatedFile,
    private val parser: Parser,
    private val renderer: Renderer
) : Task { // very scala like. todo: #todo1
    private val _name = FileName(source)
    private val visitor = YamlFrontMatterVisitor()
    private val _props = FileProperty(source)

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

    override fun toggleComplete() {
        val nextValue = _props.props()
            .find { it.name() == PropertyName.Completion.name }
            ?.value()
            ?.let {
                if (it == "false") "true"
                else "false"
            } ?: "true"

        _props.addProperty(CompletedProperty(nextValue))


    }

    override fun equals(other: Any?): Boolean {
        return (other is Task) && other.name() == this.name()
    }

    override fun props(): List<Property> = _props.props()

    companion object {
        val parser = Parser.builder()
            .extensions(
                listOf(
                    YamlFrontMatterExtension.create()
                )
            )
            .build()
        val renderer = TextContentRenderer.builder()
            .extensions(
                listOf(
                    YamlFrontMatterExtension.create()
                )
            )
            .build()

        //only for existing files
        fun from(file: Source): Task {
            val validatedFile = ValidatedFile.from(file)

            println("make task for ${file.absolutePath()}")

            return FileTask(validatedFile, parser, renderer)
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
                ),
                parser,
                renderer
            )
        }
    }

}

class InMemoryTask(
    private var name: String
) : Task {
    override fun props(): List<Property> {
        return emptyList()
    }

    override fun name(): String {
        return name
    }

    override fun rename(newName: String) {
        name = newName
    }

    override fun delete() {
        TODO("Not yet implemented")
    }

    override fun content(): Iterator<String> {
        return listOf("").iterator()
    }

    override fun toggleComplete() {
        TODO("Not yet implemented")
    }
}
