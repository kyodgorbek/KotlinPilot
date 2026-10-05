package io.kotlinpilot.filesystem

import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import io.kotlinpilot.infrastructure.filesystem.SafeFileSystemService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafeFileSystemTest {

    @Test
    fun `test file creation reading editing and deletion`(@TempDir tempDir: Path) {
        val policy = WorkspaceSecurityPolicy(tempDir)
        val fs = SafeFileSystemService(policy)

        // Write
        val bytes = fs.writeFile("src/Main.kt", "fun main() {\n    println(\"Hello\")\n}\n")
        assertTrue(bytes > 0)

        // Read
        val read = fs.readFile("src/Main.kt")
        assertTrue(read.contains("println(\"Hello\")"))

        // Read slice
        val slice = fs.readFile("src/Main.kt", startLine = 2, endLine = 2)
        assertEquals("2:     println(\"Hello\")", slice)

        // Edit
        val edited = fs.editFile("src/Main.kt", "println(\"Hello\")", "println(\"KotlinPilot\")")
        assertTrue(edited)
        val readAfterEdit = fs.readFile("src/Main.kt")
        assertTrue(readAfterEdit.contains("println(\"KotlinPilot\")"))

        // Search
        val matches = fs.searchCode("KotlinPilot")
        assertEquals(1, matches.size)

        // List
        val files = fs.listFiles()
        assertTrue(files.any { it.contains("src/Main.kt") })

        // Delete
        val deleted = fs.deleteFile("src/Main.kt")
        assertTrue(deleted)
        assertFalse(tempDir.resolve("src/Main.kt").toFile().exists())
    }
}
