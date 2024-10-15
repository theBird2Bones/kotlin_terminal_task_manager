package persistance

import org.junit.jupiter.api.Assertions.*
import tira.persistance.domain.*
import tira.persistance.domain.newtypes.*
import kotlin.test.Test
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path

//todo: omnomnom а как задавать фичи и сценарии для тестов? junit не очень
class SourceTests {

    @Test
    fun renamePathSource() {
        val tmpdir = common.TempDir().tempTestDir() //todo: add cleaner

        val tmp = Files.createTempFile(tmpdir, "name before", ".tmp")

        val testPath = Path(tmp.toString())

        val source = PathSource.from(testPath)

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

        val pathSource = PathSource(tmpfile.parent)
        val dependentSource = DependentSource.from(
            ValidatedFile.from(PathSource(tmpfile)),
            ValidatedDirectory.from(pathSource)
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
