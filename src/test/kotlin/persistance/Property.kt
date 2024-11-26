package persistance

import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.junit.jupiter.api.Test
import tira.persistance.domain.*
import tira.persistance.domain.newtypes.ValidatedFile
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists


class Property {
    val tmpDir = common.TempDir().tempTestDir()

    @Test
    fun `receive props`() {
        val path = Path.of(tmpDir.absolutePathString(), "tmpFile.md")
        path.deleteIfExists()
        path.createFile()
        val source = PathSource.from(path)
        val fw = FileWriter(source.absolutePath())
        fw.write(
            """
                ---
                date: 2024-02-02T04:14:54-08:00
                draft: false
                title: Example
                weight: 10
                ---
                """.trimIndent()
        )
        fw.flush()

        val fp = FileProperty(ValidatedFile.from(source))
        val props = fp
            .props()
            .map { Pair(it.name(), it.value()) }
            .toMap()

        assert(props.containsKey("date")) { "No date" }
        assert(props.containsKey("draft")) { "No draft" }
        assert(props.containsKey("title")) { "No title" }
        assert(props.containsKey("weight")) { "No weight" }
    }

    @Test
    fun `set props in empty file`() {
        val path = Path.of(tmpDir.absolutePathString(), "tmpFile.md")
        path.deleteIfExists()
        path.createFile()
        val source = PathSource.from(path)

        val fp = FileProperty(ValidatedFile.from(source))
        fp.addProperty(RawProperty("draft", "false"))

        val props = fp
            .props()
            .map { Pair(it.name(), it.value()) }
            .toMap()

        assert(props.containsKey("draft")) { "No draft" }

        val expectedFileContent =
            """
                ---
                draft: false
                ---
            """.trimIndent()

        val actualFileContent = FileReader(File(path.absolutePathString())).readLines().reduce { acc, x -> "${acc}\n${x}"
        }

        assert(expectedFileContent == actualFileContent) { "file content not the same" }
    }

    @Test
    fun `add props in file with another tags`() {
        val path = Path.of(tmpDir.absolutePathString(), "tmpFile.md")
        path.deleteIfExists()
        path.createFile()
        val source = PathSource.from(path)
        val fw = FileWriter(source.absolutePath())
        fw.write(
            """
                ---
                draft: false
                ---
                """.trimIndent()
        )
        fw.flush()

        val fp = FileProperty(ValidatedFile.from(source))
        fp.addProperty(RawProperty("weight", "too much"))

        val props = fp
            .props()
            .map { Pair(it.name(), it.value()) }
            .toMap()

        assert(props.containsKey("draft")) { "No draft" }
        assert(props.containsKey("weight")) { "No weight" }

        val expectedFileContent =
            """
                ---
                draft: false
                weight: too much
                ---
            """.trimIndent()

        val actualFileContent = FileReader(File(path.absolutePathString())).readLines().reduce { acc, x -> "${acc}\n${x}"
        }

        assert(expectedFileContent == actualFileContent) { "file content not the same" }
    }
}
