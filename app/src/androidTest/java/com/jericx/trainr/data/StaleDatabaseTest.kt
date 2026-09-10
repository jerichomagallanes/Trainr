package com.jericx.trainr.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.local.TrainrDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

// Before release a schema change resets the local database instead of earning
// a migration. What must not happen is the app refusing to open: a stale file
// is dropped, and the client starts over rather than seeing a crash.
@RunWith(AndroidJUnit4::class)
class StaleDatabaseTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "migration_test.db"

    @After
    fun deleteTheFile() {
        context.deleteDatabase(name)
    }

    @Test
    fun aFileFromTheOldSchemaOpensEmptyRatherThanRefusingToOpen() = runTest {
        context.deleteDatabase(name)
        writeVersionOne()

        val database = Room.databaseBuilder(context, TrainrDatabase::class.java, name)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        assertThat(database.userDao.getCurrentUser()).isNull()

        database.close()
    }

    // Room builds the tables, then the users table is put back the shape it
    // had before the style and preferred-time questions went.
    private fun writeVersionOne() {
        Room.databaseBuilder(context, TrainrDatabase::class.java, name).build().apply {
            openHelper.writableDatabase
            close()
        }

        val file = context.getDatabasePath(name)
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        db.execSQL("DROP TABLE users")
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                firstName TEXT NOT NULL, age INTEGER NOT NULL,
                gender TEXT NOT NULL, height REAL NOT NULL,
                weight REAL NOT NULL, fitnessGoal TEXT NOT NULL,
                experienceLevel TEXT NOT NULL, workoutLocation TEXT NOT NULL,
                availableEquipment TEXT NOT NULL,
                workoutDaysPerWeek INTEGER NOT NULL, workoutDuration INTEGER NOT NULL,
                preferredWorkoutTime TEXT NOT NULL, injuries TEXT NOT NULL,
                workoutType TEXT NOT NULL, bodyUnitSystem TEXT NOT NULL,
                liftingUnitSystem TEXT, createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO users VALUES (
                1, 'Jeco', 26, 'MALE', 175.0, 72.0, 'MUSCLE_GAIN', 'BEGINNER',
                'HOME', '["DUMBBELL"]', 3, 45, 'EVENING', '["LOWER_BACK"]', 'STRENGTH',
                'METRIC', NULL, 1700000000000
            )
            """.trimIndent()
        )
        db.version = 1
        db.close()
    }
}
