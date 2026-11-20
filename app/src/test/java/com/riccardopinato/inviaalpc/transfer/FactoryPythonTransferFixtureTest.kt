package com.riccardopinato.inviaalpc.transfer

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Independent, deterministic second consumer of App Factory Python fixtures.
 * Exercises real transfer policy and the same SHA-256 algorithm used by the
 * resumable upload store, but is NOT a network/MediaStore device test.
 */
class FactoryPythonTransferFixtureTest {
    @Test fun pythonFixtureValidatesTransferChunkBoundsAndContentHashes() {
        val root = System.getenv("FACTORY_FIXTURE_ROOT")
        // Other app unit-test jobs do not generate the pinned Factory fixture.
        // The dedicated consumer CI provides this env variable and MUST assert
        // that this testcase executed successfully (not skipped).
        assumeTrue("Factory fixture is only available in the consumer workflow", !root.isNullOrBlank())
        val folder = File(requireNotNull(root))
        assertTrue(folder.isDirectory)
        val manifest = File(folder, "manifest.tsv")
        assertTrue(manifest.isFile)
        val rows = manifest.readLines().filter { it.isNotBlank() }
        assertEquals(512, rows.size)
        val hashes = HashMap<String, Int>()
        for ((index, line) in rows.withIndex()) {
            val parts = line.split('\t')
            assertEquals(3, parts.size)
            val (name, sizeValue, expectedHash) = parts
            assertEquals("synthetic_" + index.toString().padStart(6, '0') + ".bin", name)
            assertEquals(name, FileNameUtils.sanitize(name))
            val file = File(folder, name)
            assertTrue(file.isFile)
            val size = sizeValue.toLong()
            assertEquals(size, file.length())
            assertTrue(TransferReliabilityPolicy.isValidFileSize(size))
            assertTrue(TransferReliabilityPolicy.isValidChunk(size, 0L, size))
            assertTrue(!TransferReliabilityPolicy.isValidChunk(size, size, 1L))
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { stream ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val count = stream.read(buffer)
                    if (count == -1) break
                    if (count > 0) md.update(buffer, 0, count)
                }
            }
            val actualHash = md.digest().joinToString("") { "%02x".format(it) }
            assertEquals(expectedHash, actualHash)
            hashes[actualHash] = (hashes[actualHash] ?: 0) + 1
        }
        assertEquals(8, hashes.values.count { it == 2 })
        assertTrue(hashes.values.all { it <= 2 })
    }
}
