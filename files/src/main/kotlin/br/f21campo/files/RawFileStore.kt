package br.f21campo.files

import br.f21campo.domain.Sha256
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class RawFileImport(
    val path: File,
    val sizeBytes: Long,
    val sha256: String,
    val immutableOriginal: Boolean = true,
)

class RawFileStore(private val controlledDirectory: File) {
    fun import(source: File): RawFileImport {
        require(source.isFile) { "source file does not exist" }
        controlledDirectory.mkdirs()
        FileInputStream(source).use { input ->
            val hash = Sha256.of(input)
            val destination = File(controlledDirectory, "$hash.raw")
            if (!destination.exists()) {
                val temporary = File(controlledDirectory, "$hash.part")
                FileInputStream(source).use { sourceInput ->
                    FileOutputStream(temporary).use { output -> sourceInput.copyTo(output) }
                }
                check(temporary.length() == source.length()) { "copy size validation failed" }
                if (temporary.renameTo(destination).not()) error("could not finalize raw file")
            }
            return RawFileImport(destination, destination.length(), hash)
        }
    }
}
