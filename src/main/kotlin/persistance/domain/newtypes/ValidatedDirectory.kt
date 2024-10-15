package tira.persistance.domain.newtypes

import tira.persistance.domain.Source

@JvmInline
value class ValidatedDirectory private constructor(val underlying: Source) {
    companion object {
        fun from(file: Source): ValidatedDirectory {
            if (!file.isDirectory()) {
                throw Exception("${file.absolutePath()} is not a directory")
            }
            return ValidatedDirectory(file)
        }
    }

}
