package com.jericx.trainr.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Preferred workout time was asked of everyone and read by nothing, and
// training style asked a question the movement vocabulary could not answer.
// Rebuilt rather than dropped: DROP COLUMN needs a SQLite newer than the one
// on the oldest phones this ships to.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users_new (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                firstName TEXT NOT NULL,
                age INTEGER NOT NULL,
                gender TEXT NOT NULL,
                height REAL NOT NULL,
                weight REAL NOT NULL,
                fitnessGoal TEXT NOT NULL,
                experienceLevel TEXT NOT NULL,
                workoutLocation TEXT NOT NULL,
                availableEquipment TEXT NOT NULL,
                workoutDaysPerWeek INTEGER NOT NULL,
                workoutDuration INTEGER NOT NULL,
                injuries TEXT NOT NULL,
                bodyUnitSystem TEXT NOT NULL,
                liftingUnitSystem TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO users_new
            SELECT id, firstName, age, gender, height, weight, fitnessGoal,
                   experienceLevel, workoutLocation, availableEquipment,
                   workoutDaysPerWeek, workoutDuration, injuries,
                   bodyUnitSystem, liftingUnitSystem, createdAt
            FROM users
            """.trimIndent()
        )
        db.execSQL("DROP TABLE users")
        db.execSQL("ALTER TABLE users_new RENAME TO users")
    }
}
