package com.jericx.trainr.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.local.MIGRATION_1_2
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.FitnessGoal
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

// A migration that does not open is every existing client's app crashing on
// update, so this walks the real path: a version 1 file with a profile in it,
// opened by the version 2 schema.
@RunWith(AndroidJUnit4::class)
class UserMigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "migration_test.db"

    @After
    fun deleteTheFile() {
        context.deleteDatabase(name)
    }

    @Test
    fun aProfileSavedBeforeTheStyleAndTimeQuestionsWereDroppedStillOpens() = runTest {
        context.deleteDatabase(name)
        writeVersionOne()

        val database = Room.databaseBuilder(context, TrainrDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            .build()

        val user = UserMapper().mapToDomain(database.userDao.getCurrentUser()!!)

        assertThat(user.id).isEqualTo(1L)
        assertThat(user.firstName).isEqualTo("Jeco")
        assertThat(user.fitnessGoal).isEqualTo(FitnessGoal.MUSCLE_GAIN)
        assertThat(user.availableEquipment).containsExactly(Equipment.DUMBBELL)
        assertThat(user.workoutDaysPerWeek).isEqualTo(3)
        assertThat(user.workoutDuration).isEqualTo(45)

        database.close()
    }

    // Room builds the tables this version shares with the last one, then the
    // users table is put back the shape it had. Hand-writing all five would
    // be five chances to describe them wrongly and call it a passing test.
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
