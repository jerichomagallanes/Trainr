package com.jericx.trainr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        WeeklyWorkoutPlanEntity::class,
        WorkoutDayEntity::class,
        WorkoutExerciseEntity::class,
        ExerciseSetEntity::class,
        SessionOutcomeEntity::class,
        AppliedAdjustmentEntity::class,
        AdjustmentFeedbackEntity::class,
        TrainingPreferenceEntity::class,
        SessionNoteEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TrainrDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val unstuckDao: UnstuckDao

    companion object {
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `exercise_sets` ADD COLUMN `actualOrigin` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE `exercise_sets` ADD COLUMN `omittedBy` INTEGER")
                db.execSQL("ALTER TABLE `workout_exercises` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `workout_exercises` ADD COLUMN `addedBy` INTEGER")
                db.execSQL(
                    """
                    UPDATE exercise_sets SET actualOrigin = 'LEGACY_UNKNOWN'
                    WHERE actualReps IS NOT NULL OR actualWeightKg IS NOT NULL OR actualSeconds IS NOT NULL
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE workout_exercises SET sortOrder = (
                        SELECT COUNT(*) FROM workout_exercises o
                        WHERE o.workoutDayId = workout_exercises.workoutDayId AND o.id < workout_exercises.id
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE TABLE IF NOT EXISTS `session_outcomes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutDayId` INTEGER NOT NULL, `finishKind` TEXT NOT NULL, `finishedAt` INTEGER NOT NULL, `performedSetCount` INTEGER NOT NULL, `plannedSetCount` INTEGER NOT NULL, FOREIGN KEY(`workoutDayId`) REFERENCES `workout_days`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_outcomes_workoutDayId` ON `session_outcomes` (`workoutDayId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `applied_adjustments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutDayId` INTEGER NOT NULL, `proposalId` TEXT NOT NULL, `reason` TEXT NOT NULL, `proposalJson` TEXT NOT NULL, `policyVersion` TEXT NOT NULL, `appliedAt` INTEGER NOT NULL, `undoneAt` INTEGER, FOREIGN KEY(`workoutDayId`) REFERENCES `workout_days`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_applied_adjustments_workoutDayId` ON `applied_adjustments` (`workoutDayId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_applied_adjustments_proposalId` ON `applied_adjustments` (`proposalId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `adjustment_feedback` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `adjustmentId` INTEGER NOT NULL, `answer` TEXT, `answeredAt` INTEGER, `dismissedAt` INTEGER, FOREIGN KEY(`adjustmentId`) REFERENCES `applied_adjustments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_adjustment_feedback_adjustmentId` ON `adjustment_feedback` (`adjustmentId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `training_preferences` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `kind` TEXT NOT NULL, `minutes` INTEGER NOT NULL, `weekday` INTEGER NOT NULL, `sourceAdjustmentId` INTEGER, `confirmedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_training_preferences_userId` ON `training_preferences` (`userId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `session_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `workoutDayId` INTEGER, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`workoutDayId`) REFERENCES `workout_days`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_notes_userId` ON `session_notes` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_notes_workoutDayId` ON `session_notes` (`workoutDayId`)")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_4_5)

        // Predate the 2026-09-13 store upload: the only versions that may be wiped.
        val LEGACY_VERSIONS = intArrayOf(1, 2, 3)
    }
}
