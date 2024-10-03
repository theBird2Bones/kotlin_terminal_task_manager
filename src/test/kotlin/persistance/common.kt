package persistance

import java.nio.file.Files
import java.nio.file.Path

object common {
    class TempDir {
        fun tempTestDir(): Path {
            return Files.createTempDirectory("Tests")
        }
    }

}
