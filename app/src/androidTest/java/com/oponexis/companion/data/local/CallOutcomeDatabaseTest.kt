package com.oponexis.companion.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallOutcomeDatabaseTest {
    private lateinit var context: Context
    private var database: SkeletonDatabase? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun pendingDraftCanBeSelected() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, SkeletonDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = requireNotNull(database).callOutcomeDao()
        val callRef = UUID.randomUUID().toString()
        dao.insert(
            CallOutcomeEntity(
                callRef = callRef,
                observedAtEpochMillis = 100,
                disconnectCategory = "remote",
                durationBucket = "short",
            ),
        )

        assertEquals(callRef, dao.observePending().first().single().callRef)
        assertEquals(1, dao.selectOutcome(callRef, "interested", 200))
        assertTrue(dao.observePending().first().isEmpty())
    }

    @Test
    fun migrationFromVersionOnePreservesFoundationData() {
        context.deleteDatabase(TEST_DATABASE)
        val path = context.getDatabasePath(TEST_DATABASE)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { sqlite ->
            sqlite.execSQL(
                "CREATE TABLE foundation_metadata (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            )
            sqlite.execSQL("INSERT INTO foundation_metadata (`key`, `value`) VALUES ('existing', 'kept')")
            sqlite.version = 1
        }

        database = Room.databaseBuilder(context, SkeletonDatabase::class.java, TEST_DATABASE)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()

        requireNotNull(database).openHelper.writableDatabase.query(
            "SELECT value FROM foundation_metadata WHERE `key` = 'existing'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("kept", cursor.getString(0))
        }
    }

    @Test
    fun migrationFromVersionTwoAddsTemporaryIdentityFields() {
        context.deleteDatabase(TEST_DATABASE)
        val path = context.getDatabasePath(TEST_DATABASE)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { sqlite ->
            sqlite.execSQL(
                """
                CREATE TABLE call_outcomes (
                    call_ref TEXT NOT NULL,
                    observed_at_epoch_millis INTEGER NOT NULL,
                    disconnect_category TEXT NOT NULL,
                    duration_bucket TEXT NOT NULL,
                    outcome_code TEXT,
                    resolved_at_epoch_millis INTEGER,
                    dismissed_at_epoch_millis INTEGER,
                    PRIMARY KEY(call_ref)
                )
                """.trimIndent(),
            )
            sqlite.execSQL(
                "CREATE TABLE foundation_metadata (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            )
            sqlite.version = 2
        }

        database = Room.databaseBuilder(context, SkeletonDatabase::class.java, TEST_DATABASE)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()

        val columns = mutableSetOf<String>()
        requireNotNull(database).openHelper.writableDatabase.query(
            "PRAGMA table_info(call_outcomes)",
        ).use { cursor ->
            while (cursor.moveToNext()) columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
        }
        assertTrue("phone_number" in columns)
        assertTrue("display_name" in columns)
        assertTrue("history_phone_number" in columns)
        assertTrue("customer_ref" in columns)
        assertTrue("sms_delivery_status" in columns)
    }

    private companion object {
        const val TEST_DATABASE = "m6-migration-test.db"
    }
}
