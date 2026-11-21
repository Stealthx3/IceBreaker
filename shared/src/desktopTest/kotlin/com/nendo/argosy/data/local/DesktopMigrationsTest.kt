package com.nendo.argosy.data.local

import androidx.room.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopMigrationsTest {

    private val dir: File = createTempDirectory("argosy-migration-test").toFile()

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun migrationsCoverEveryStepFromTheFirstDesktopSchema() {
        val steps = DesktopMigrations.ALL.map { it.startVersion to it.endVersion }
        assertEquals(214, steps.first().first)
        assertEquals(steps.map { it.second }.dropLast(1), steps.map { it.first }.drop(1))
    }

    @Test
    fun aSchema214DatabaseOpensAtTheCurrentVersion() = runBlocking {
        val file = File(dir, "alauncher.db")
        createSchema214(file)

        val db = DesktopDatabaseFactory.create(file)
        try {
            db.platformDao().getAllPlatforms()
            val version = db.useReaderConnection { conn ->
                conn.usePrepared("PRAGMA user_version") { it.step(); it.getLong(0) }
            }
            assertEquals(DesktopMigrations.ALL.last().endVersion.toLong(), version)
            val columns = db.useReaderConnection { conn ->
                conn.usePrepared("PRAGMA table_info(`platforms`)") { stmt ->
                    buildList { while (stmt.step()) add(stmt.getText(1)) }
                }
            }
            assertTrue("skipFullSync" in columns, columns.toString())
        } finally {
            db.close()
        }
    }

    private fun createSchema214(file: File) {
        val schema = listOf(File("../app/schemas"), File("app/schemas"))
            .map { File(it, "com.nendo.argosy.data.local.ALauncherDatabase/214.json") }
            .first { it.isFile }
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val root = Moshi.Builder().build().adapter<Map<String, Any>>(type).fromJson(schema.readText())!!

        @Suppress("UNCHECKED_CAST")
        val database = root["database"] as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val entities = database["entities"] as List<Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val setup = database["setupQueries"] as List<String>
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            for (entity in entities) {
                val table = entity["tableName"] as String
                connection.execSQL((entity["createSql"] as String).replace("\${TABLE_NAME}", table))

                @Suppress("UNCHECKED_CAST")
                val indices = entity["indices"] as? List<Map<String, Any>> ?: emptyList()
                for (index in indices) {
                    connection.execSQL((index["createSql"] as String).replace("\${TABLE_NAME}", table))
                }
            }
            setup.forEach(connection::execSQL)
            connection.execSQL("PRAGMA user_version = 214")
        } finally {
            connection.close()
        }
    }
}
