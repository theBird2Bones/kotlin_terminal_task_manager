package tira.persistance.domain.newtypes

import tira.persistance.domain.PathSource
import tira.persistance.domain.Source
import java.nio.file.Files
import java.nio.file.Path

@JvmInline
value class ValidatedDirectory private constructor(val underlying: Source) {
    companion object {
        fun from(file: Source): ValidatedDirectory {
            if (!file.isDirectory()) {
                throw Exception("${file.absolutePath()} is not a directory")
            }
            return ValidatedDirectory(file)
        }

        fun create(path: Path): ValidatedDirectory {
            val path = Files.createDirectory(path)
            return from(
                PathSource.from(path)
            )
        }
    }

}
