package br.f21campo.files

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RawFileStoreTest {
    @Test fun importIsContentAddressedAndDeduplicated() {
        val root = createTempDirectory("f21-raw").toFile()
        val source = File(root, "source.obs").apply { writeText("raw-evidence") }
        val second = File(root, "renamed.obs").apply { writeText("raw-evidence") }
        val store = RawFileStore(File(root, "controlled"))
        val first = store.import(source)
        val duplicate = store.import(second)
        assertEquals(first.sha256, duplicate.sha256)
        assertEquals(first.path, duplicate.path)
        assertTrue(first.immutableOriginal)
    }

    @Test fun importRejectsCorruptedExistingContentAddressedFile() {
        val root = createTempDirectory("f21-raw-corrupt").toFile()
        val source = File(root, "source.obs").apply { writeText("raw-evidence") }
        val store = RawFileStore(File(root, "controlled"))
        val stored = store.import(source)
        stored.path.writeText("corrupted")

        try {
            store.import(source)
            fail("corrupted content-addressed file must not be reused")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("unexpected"))
        }
    }
}
