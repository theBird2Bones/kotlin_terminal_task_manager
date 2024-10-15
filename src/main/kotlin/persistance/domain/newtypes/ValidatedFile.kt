package tira.persistance.domain.newtypes

import tira.persistance.domain.Source

//todo: add method proxy in order to escape file becoming a directory
@JvmInline
value class ValidatedFile private constructor(val underlying: Source) {
    companion object {
        fun from(file: Source): ValidatedFile {
            if (!file.isFile()) {
                throw Exception("${file.absolutePath()} is not a file")
            }
            return ValidatedFile(file)
        }
    }
}
