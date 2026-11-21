package com.nendo.argosy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SuExecutorIsolationTest {

    private val name = SuExecutor::class.simpleName!!
    private val reference = Regex("\\b$name\\b")

    private fun mainSources(): List<File> {
        val roots = listOf(
            "src/main/kotlin", "app/src/main/kotlin",
            "../shared/src/androidMain/kotlin", "shared/src/androidMain/kotlin",
            "../shared/src/commonMain/kotlin", "shared/src/commonMain/kotlin"
        ).map(::File).filter { it.isDirectory }
        assertTrue("Could not locate the main source tree", roots.isNotEmpty())
        return roots
    }

    @Test
    fun `root shell never consults the su route`() {
        val rootShell = mainSources().map { File(it, "com/nendo/argosy/util/RootShell.kt") }.first { it.isFile }.readText()

        assertFalse(reference.containsMatchIn(rootShell))
    }

    @Test
    fun `only the performance resolver and the user-approved privileged access route reach su`() {
        val users = mainSources().asSequence().flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "kt" && it.name != "SuExecutor.kt" }
            .filter { reference.containsMatchIn(it.readText()) }
            .map { it.name }
            .toSet()

        assertEquals(setOf("DevicePerformance.kt", "PrivilegedAccess.kt"), users)
    }
}
