package persistance

import org.junit.jupiter.api.Test
import tira.persistance.domain.*
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists


class Tasks {
    val tmpDir = common.TempDir().tempTestDir()


    @Test
    fun `toggleComplete change tag`() {
        val path = Path.of(tmpDir.absolutePathString(), "tmpFile.md")
        path.deleteIfExists()
        path.createFile()
        val source = PathSource.from(path)

        val task = FileTask.from(source)

        assert(task.props().isEmpty()) { "Not empty" }

        task.toggleComplete()

        var props = task
            .props()
            .map { Pair(it.name(), it.value()) }
            .toMap()

        assert(props.containsKey(PropertyName.Completion.name)) { "No complete" }
        assert(props.get(PropertyName.Completion.name) == "true") { "complete is not true" }
        task.toggleComplete()

        props = task
            .props()
            .map { Pair(it.name(), it.value()) }
            .toMap()

        assert(props.containsKey(PropertyName.Completion.name)) { "No complete" }
        assert(props.get(PropertyName.Completion.name) == "false") { "complete is not false" }

        task.toggleComplete()

        props = task
            .props()
            .map { Pair(it.name(), it.value()) }
            .toMap()

        assert(props.containsKey(PropertyName.Completion.name)) { "No complete" }
        assert(props.get(PropertyName.Completion.name) == "true") { "complete is not true" }
    }
}
