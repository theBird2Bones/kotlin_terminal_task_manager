package persistance

import org.junit.jupiter.api.Assertions.*
import tira.persistance.domain
import kotlin.test.Test
import tira.persistance.domain.Source
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively

//todo: omnomnom а как задавать фичи и сценарии для тестов? junit не очень
class SourceTests {

    @Test
    fun renamePathSource() {
        val tmpdir = common.TempDir().tempTestDir() //todo: add cleaner

        val tmp = Files.createTempFile(tmpdir, "name before", ".tmp")

        val testPath = Path(tmp.toString())

        val source = Source.Companion.PathSource.from(testPath)

        val nameBefore = source.name()
        val parentBefore = source.parent()

        source.rename("name after")

        val nameAfter = source.name()
        val parentAfter = source.parent()

        assertNotEquals(nameBefore, nameAfter, "file names the same")
        assertEquals(parentBefore, parentAfter, "parents differ")
    }

    @Test
    fun renameDependentSource() {
        val tmpdir = common.TempDir().tempTestDir()
        val tmpfile = Files.createTempFile(tmpdir, "name before", ".tmp")

        val pathSource = Source.Companion.PathSource(tmpfile.parent)
        val dependentSource = Source.Companion.DependentSource.from(
            domain.ValidatedFile.from(Source.Companion.PathSource(tmpfile)),
            domain.ValidatedDirectory.from(pathSource)
        )

        val newPathSourceName = UUID.randomUUID().toString()
        pathSource.rename(newPathSourceName)

        assertEquals(
            pathSource.absolutePath(),
            dependentSource.parent(),
            "dependent source doesn't keep consistency with source"
        )

    }
}
