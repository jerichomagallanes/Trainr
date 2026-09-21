package com.jericx.trainr.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_outcomes",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workoutDayId"], unique = true)]
)
data class SessionOutcomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutDayId: Long,
    val finishKind: String,
    val finishedAt: Long,
    val performedSetCount: Int,
    val plannedSetCount: Int
)

@Entity(
    tableName = "applied_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workoutDayId"),
        Index(value = ["proposalId"], unique = true)
    ]
)
data class AppliedAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutDayId: Long,
    val proposalId: String,
    val reason: String,
    val proposalJson: String,
    val policyVersion: String,
    val appliedAt: Long,
    val undoneAt: Long?
)

@Entity(
    tableName = "adjustment_feedback",
    foreignKeys = [
        ForeignKey(
            entity = AppliedAdjustmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["adjustmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["adjustmentId"], unique = true)]
)
data class AdjustmentFeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val adjustmentId: Long,
    val answer: String?,
    val answeredAt: Long?,
    val dismissedAt: Long?
)

@Entity(
    tableName = "training_preferences",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class TrainingPreferenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val kind: String,
    val minutes: Int,
    val weekday: Int,
    val sourceAdjustmentId: Long?,
    val confirmedAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "session_notes",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("workoutDayId")]
)
data class SessionNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val workoutDayId: Long?,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)
