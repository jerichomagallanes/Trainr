package com.jericx.trainr.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.AssetExerciseCatalog
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UnstuckMapper
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.repository.AdjustmentRepositoryImpl
import com.jericx.trainr.data.repository.UserRepositoryImpl
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.unstuck.ActualOrigin
import com.jericx.trainr.domain.unstuck.AdjustmentConstraint
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AdjustmentSnapshot
import com.jericx.trainr.domain.unstuck.ApplyRejection
import com.jericx.trainr.domain.unstuck.ApplyResult
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ExerciseSnapshot
import com.jericx.trainr.domain.unstuck.PlanRevision
import com.jericx.trainr.domain.unstuck.PolicyDecision
import com.jericx.trainr.domain.unstuck.ProposalChange
import com.jericx.trainr.domain.unstuck.ReasonCode
import com.jericx.trainr.domain.unstuck.SetSnapshot
import com.jericx.trainr.domain.unstuck.TimeScope
import com.jericx.trainr.domain.unstuck.UndoResult
import com.jericx.trainr.domain.unstuck.UnstuckPolicy
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdjustmentApplyTest {

    private lateinit var db: TrainrDatabase
    private lateinit var catalog: ExerciseCatalog
    private lateinit var workouts: UserRepositoryImpl
    private lateinit var adjustments: AdjustmentRepositoryImpl
    private lateinit var policy: UnstuckPolicy

    private val user = UserProfile(
        firstName = "Jericho",
        age = 30,
        gender = Gender.MALE,
        weight = 80f,
        fitnessGoal = FitnessGoal.MUSCLE_GAIN,
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        availableEquipment = listOf(Equipment.DUMBBELL),
        workoutDaysPerWeek = 3,
        workoutDuration = 40
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TrainrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        catalog = AssetExerciseCatalog(context)
        workouts = UserRepositoryImpl(db.userDao, UserMapper())
        adjustments = AdjustmentRepositoryImpl(
            db, db.userDao, db.unstuckDao, UnstuckMapper(), UserMapper(), catalog
        )
        policy = UnstuckPolicy(catalog)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun applyingKeepsEveryPerformedSetsIdAndValues() = runTest {
        val seeded = seedDay()
        val squats = seeded.exercise("jump_squat")
        log(squats.sets[0], squats.id)
        log(squats.sets[1], squats.id)
        val day = reread(seeded.id)
        val proposal = shorten(day)

        val result = adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW)

        val applied = result as ApplyResult.Applied
        val after = reread(day.id)
        val performed = after.exercise("jump_squat").sets.take(2)
        assertThat(performed).isEqualTo(day.exercise("jump_squat").sets.take(2))
        assertThat(performed.map { it.actualReps }).containsExactly(12, 12)
        assertThat(performed.map { it.omittedBy }).containsExactly(null, null)
        assertThat(after.omittedSetIds()).isEqualTo(proposal.shedSetIds())
        assertThat(after.omittedBy()).containsExactly(applied.adjustment.id)
    }

    @Test
    fun applyingTheSameProposalTwiceWritesOnce() = runTest {
        val day = seedDay()
        val proposal = shorten(day)
        val first = adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW)
            as ApplyResult.Applied
        val omitted = reread(day.id).omittedSetIds()

        val second = adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW + MINUTE)

        val again = second as ApplyResult.AlreadyApplied
        assertThat(again.adjustment.id).isEqualTo(first.adjustment.id)
        assertThat(again.adjustment.appliedAt).isEqualTo(NOW)
        assertThat(adjustments.getAdjustments(day.id)).hasSize(1)
        assertThat(reread(day.id).omittedSetIds()).isEqualTo(omitted)
    }

    @Test
    fun aProposalBuiltBeforeASetWasLoggedIsStale() = runTest {
        val day = seedDay()
        val proposal = shorten(day)
        val squats = day.exercise("jump_squat")
        log(squats.sets[0], squats.id)

        val result = adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW)

        val current = reread(day.id)
        assertThat(result).isEqualTo(ApplyResult.Stale(PlanRevision.of(current)))
        assertThat(current.omittedSetIds()).isEmpty()
        assertThat(adjustments.getAdjustments(day.id)).isEmpty()
    }

    @Test
    fun aRejectedChangeLeavesNothingBehind() = runTest {
        val day = seedDay()
        val proposal = shorten(day)
        assertThat(proposal.changes.size).isAtLeast(2)
        val broken = proposal.copy(
            changes = proposal.changes.mapIndexed { index, change ->
                if (index == 0) change else change.copy(before = change.before.withWrongTargets())
            }
        )

        val result = adjustments.apply(broken, day.id, AdjustmentReason.LESS_TIME, NOW)

        assertThat(result).isEqualTo(ApplyResult.Rejected(ApplyRejection.BEFORE_SNAPSHOT_MISMATCH))
        assertThat(reread(day.id).omittedSetIds()).isEmpty()
        assertThat(adjustments.getAdjustments(day.id)).isEmpty()
    }

    @Test
    fun aPerformedSetCanNeverBeOmitted() = runTest {
        val seeded = seedDay()
        val squats = seeded.exercise("jump_squat")
        log(squats.sets[0], squats.id)
        val day = reread(seeded.id)
        val target = day.exercise("jump_squat")
        val proposal = handBuilt(
            day,
            ProposalChange(
                kind = ChangeKind.OMIT_UNPERFORMED,
                before = ExerciseSnapshot(
                    exerciseInstanceId = "exercise:${target.id}",
                    catalogKey = target.exerciseKey,
                    sets = target.sets.map { it.snapshot() }
                ),
                after = null
            )
        )

        val result = adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW)

        assertThat(result).isEqualTo(ApplyResult.Rejected(ApplyRejection.BEFORE_SNAPSHOT_MISMATCH))
        val after = reread(day.id)
        assertThat(after.omittedSetIds()).isEmpty()
        assertThat(after.exercise("jump_squat").sets.first().isCompleted).isTrue()
        assertThat(adjustments.getAdjustments(day.id)).isEmpty()
    }

    @Test
    fun undoBeforeAnyWorkRestoresTheWholeRemainingPlan() = runTest {
        val day = seedDay()
        val proposal = shorten(day)
        val applied = adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW)
            as ApplyResult.Applied
        assertThat(reread(day.id).omittedSetIds()).isNotEmpty()

        val result = adjustments.undo(applied.adjustment.id, NOW + MINUTE)

        val restored = result as UndoResult.Restored
        assertThat(restored.keptPerformedSubstituteSets).isEqualTo(0)
        assertThat(reread(day.id)).isEqualTo(day)
        assertThat(adjustments.getAdjustment(proposal.proposalId)?.undoneAt).isEqualTo(NOW + MINUTE)
    }

    @Test
    fun undoAfterPerformingTheSubstituteKeepsWhatWasLogged() = runTest {
        val day = seedDay()
        val original = day.exercise("dumbbell_step_up")
        val proposal = swap(day, original.id)
        val applied = adjustments
            .apply(proposal, day.id, AdjustmentReason.EQUIPMENT_UNAVAILABLE, NOW)
            as ApplyResult.Applied
        val substituteId = checkNotNull(applied.addedExerciseId)
        val substitute = checkNotNull(workouts.getWorkoutExercise(substituteId))
        log(substitute.sets.first(), substituteId)

        val result = adjustments.undo(applied.adjustment.id, NOW + MINUTE)

        val restored = result as UndoResult.Restored
        assertThat(restored.keptPerformedSubstituteSets).isEqualTo(1)
        val after = reread(day.id)
        assertThat(after.exercise("dumbbell_step_up").sets).isEqualTo(original.sets)
        val kept = after.exercises.single { it.id == substituteId }
        assertThat(kept.sets.map { it.isCompleted }).containsExactly(true)
        assertThat(kept.setCount).isEqualTo(1)
        assertThat(kept.addedBy).isEqualTo(applied.adjustment.id)
    }

    @Test
    fun undoOfAnUntouchedSubstituteRemovesOnlyTheAppsOwnRows() = runTest {
        val day = seedDay()
        val original = day.exercise("dumbbell_step_up")
        val applied = adjustments
            .apply(swap(day, original.id), day.id, AdjustmentReason.EQUIPMENT_UNAVAILABLE, NOW)
            as ApplyResult.Applied

        adjustments.undo(applied.adjustment.id, NOW + MINUTE)

        assertThat(workouts.getWorkoutExercise(checkNotNull(applied.addedExerciseId))).isNull()
        assertThat(reread(day.id)).isEqualTo(day)
    }

    @Test
    fun undoTwiceIsIdempotent() = runTest {
        val day = seedDay()
        val applied = adjustments
            .apply(shorten(day), day.id, AdjustmentReason.LESS_TIME, NOW)
            as ApplyResult.Applied
        adjustments.undo(applied.adjustment.id, NOW + MINUTE)

        val second = adjustments.undo(applied.adjustment.id, NOW + 2 * MINUTE)

        assertThat(second).isEqualTo(UndoResult.AlreadyUndone)
        assertThat(reread(day.id)).isEqualTo(day)
        assertThat(adjustments.getAdjustments(day.id).single().undoneAt).isEqualTo(NOW + MINUTE)
    }

    @Test
    fun reapplyAfterUndoOmitsAgainWithoutASecondRow() = runTest {
        val day = seedDay()
        val applied = adjustments
            .apply(shorten(day), day.id, AdjustmentReason.LESS_TIME, NOW)
            as ApplyResult.Applied
        val omitted = reread(day.id).omittedSetIds()
        adjustments.undo(applied.adjustment.id, NOW + MINUTE)

        val result = adjustments.reapply(applied.adjustment.id, NOW + 2 * MINUTE)

        assertThat(result).isInstanceOf(ApplyResult.Applied::class.java)
        assertThat(reread(day.id).omittedSetIds()).isEqualTo(omitted)
        assertThat(adjustments.getAdjustments(day.id).single().undoneAt).isNull()
    }

    @Test
    fun applyingAgainAfterUndoRestoresTheSameOmissions() = runTest {
        val day = seedDay()
        val proposal = shorten(day)
        val applied = adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW)
            as ApplyResult.Applied
        val omitted = reread(day.id).omittedSetIds()
        adjustments.undo(applied.adjustment.id, NOW + MINUTE)

        val result = adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW + 2 * MINUTE)

        assertThat((result as ApplyResult.Applied).adjustment.id).isEqualTo(applied.adjustment.id)
        assertThat(reread(day.id).omittedSetIds()).isEqualTo(omitted)
        assertThat(adjustments.getAdjustments(day.id)).hasSize(1)
    }

    @Test
    fun reapplyingAfterAPartlyPerformedSubstituteRestoresItsRemainingSets() = runTest {
        val day = seedDay()
        val original = day.exercise("dumbbell_step_up")
        val applied = adjustments
            .apply(swap(day, original.id), day.id, AdjustmentReason.EQUIPMENT_UNAVAILABLE, NOW)
            as ApplyResult.Applied
        val substituteId = checkNotNull(applied.addedExerciseId)
        val planned = checkNotNull(workouts.getWorkoutExercise(substituteId))
        log(planned.sets.first(), substituteId)
        adjustments.undo(applied.adjustment.id, NOW + MINUTE)

        val result = adjustments.reapply(applied.adjustment.id, NOW + 2 * MINUTE)

        assertThat((result as ApplyResult.Applied).addedExerciseId).isEqualTo(substituteId)
        val substitute = checkNotNull(workouts.getWorkoutExercise(substituteId))
        assertThat(substitute.setCount).isEqualTo(planned.sets.size)
        assertThat(substitute.sets.map { it.setNumber }).isEqualTo(planned.sets.map { it.setNumber })
        assertThat(substitute.sets.map { it.isCompleted }).containsExactly(true, false, false).inOrder()
        assertThat(substitute.sets.map { it.targetWeightKg })
            .isEqualTo(planned.sets.map { it.targetWeightKg })
        assertThat(reread(day.id).exercise("dumbbell_step_up").sets.map { it.omittedBy })
            .doesNotContain(null)
    }

    @Test
    fun aProposalIsRejectedAgainstAnyDayButItsOwn() = runTest {
        val day = seedDay()
        val proposal = shorten(day)

        val stray = adjustments.apply(proposal, day.id + 1, AdjustmentReason.LESS_TIME, NOW)

        assertThat(stray).isEqualTo(ApplyResult.Rejected(ApplyRejection.WRONG_SCOPE))
        assertThat(reread(day.id).omittedSetIds()).isEmpty()
        assertThat(adjustments.getAdjustments(day.id)).isEmpty()
        adjustments.apply(proposal, day.id, AdjustmentReason.LESS_TIME, NOW)

        val again = adjustments.apply(proposal, day.id + 1, AdjustmentReason.LESS_TIME, NOW + MINUTE)

        assertThat(again).isEqualTo(ApplyResult.Rejected(ApplyRejection.WRONG_SCOPE))
        assertThat(adjustments.getAdjustments(day.id)).hasSize(1)
    }

    @Test
    fun aReplacementNeverCopiesTheOriginalWeight() = runTest {
        val seeded = seedDay()
        val loaded = seeded.exercise("dumbbell_step_up")
        loaded.sets.forEach { workouts.updateExerciseSet(it.copy(targetWeightKg = ODD_KG), loaded.id) }
        val day = reread(seeded.id)
        val original = day.exercise("dumbbell_step_up")
        val proposal = swap(day, original.id)
        val after = checkNotNull(proposal.changes.single().after)

        val applied = adjustments
            .apply(proposal, day.id, AdjustmentReason.EQUIPMENT_UNAVAILABLE, NOW)
            as ApplyResult.Applied

        val substitute = checkNotNull(workouts.getWorkoutExercise(checkNotNull(applied.addedExerciseId)))
        assertThat(substitute.exerciseKey).isEqualTo(after.catalogKey)
        assertThat(substitute.sets.map { it.targetWeightKg })
            .isEqualTo(after.sets.map { it.targetWeightKg })
        assertThat(substitute.sets.map { it.targetWeightKg }).doesNotContain(ODD_KG)
        assertThat(substitute.sets.map { it.setNumber }).containsExactly(1, 2, 3).inOrder()
        assertThat(substitute.sets.map { it.isCompleted }).containsExactly(false, false, false)
        assertThat(substitute.videoTutorialUrl).isNull()
        assertThat(reread(day.id).exercise("dumbbell_step_up").sets.map { it.targetWeightKg })
            .containsExactly(ODD_KG, ODD_KG, ODD_KG)
    }

    @Test
    fun theRevisionChangesAfterApplyAndChangesBackAfterUndo() = runTest {
        val day = seedDay()
        val before = PlanRevision.of(day)
        val applied = adjustments
            .apply(shorten(day), day.id, AdjustmentReason.LESS_TIME, NOW)
            as ApplyResult.Applied
        assertThat(PlanRevision.of(reread(day.id))).isNotEqualTo(before)

        adjustments.undo(applied.adjustment.id, NOW + MINUTE)

        assertThat(PlanRevision.of(reread(day.id))).isEqualTo(before)
    }

    private suspend fun seedDay(): WorkoutDay {
        val userId = workouts.saveUser(user)
        val week = SampleWorkoutData.weekOne
        workouts.saveWeeklyWorkoutPlan(
            week.copy(
                id = 0,
                userId = userId,
                workoutDays = listOf(week.workoutDays.last().copy(id = 0))
            )
        )
        return checkNotNull(workouts.getWeeklyWorkoutPlan(userId, 1)).workoutDays.single()
    }

    private suspend fun reread(dayId: Long): WorkoutDay = checkNotNull(workouts.getWorkoutDay(dayId))

    private suspend fun log(set: ExerciseSet, exerciseId: Long) = workouts.updateExerciseSet(
        set.copy(
            actualReps = set.targetReps,
            actualWeightKg = set.targetWeightKg,
            actualSeconds = set.targetSeconds,
            isCompleted = true,
            actualOrigin = ActualOrigin.TYPED
        ),
        exerciseId
    )

    private fun shorten(day: WorkoutDay, minutes: Int = BUDGET_MINUTES): AdjustmentProposal =
        proposed(day, AdjustmentConstraint.LessTime(minutes, TimeScope.WHOLE_SESSION), "request-time")

    private fun swap(day: WorkoutDay, exerciseId: Long): AdjustmentProposal =
        proposed(day, AdjustmentConstraint.EquipmentUnavailable(exerciseId, emptySet()), "request-kit")

    private fun proposed(
        day: WorkoutDay,
        constraint: AdjustmentConstraint,
        requestId: String
    ): AdjustmentProposal {
        val decision = policy.decide(AdjustmentSnapshot(day, user), constraint, requestId)
        return (decision as PolicyDecision.Proposed).proposal
    }

    private fun handBuilt(day: WorkoutDay, change: ProposalChange) = AdjustmentProposal(
        proposalId = "hand-built-1",
        requestId = "request-hand",
        sessionId = "day:${day.id}",
        baseRevision = PlanRevision.of(day),
        policyVersion = UnstuckPolicy.VERSION,
        changes = listOf(change),
        preservedPerformedSetIds = day.exercises.flatMap { exercise ->
            exercise.sets.filter { it.isCompleted }.map { "set:${it.id}" }
        },
        reasonCode = ReasonCode.TIME_CONSTRAINT,
        tradeoffCode = "reduced_session",
        factReferences = emptyList()
    )

    private fun WorkoutDay.exercise(key: String) = exercises.single { it.exerciseKey == key }

    private fun WorkoutDay.omittedSetIds(): Set<Long> =
        exercises.flatMap { it.sets }.filter { it.omittedBy != null }.map { it.id }.toSet()

    private fun WorkoutDay.omittedBy(): Set<Long> =
        exercises.flatMap { it.sets }.mapNotNull { it.omittedBy }.toSet()

    private fun AdjustmentProposal.shedSetIds(): Set<Long> = changes.flatMap { change ->
        val kept = change.after?.sets?.map { it.setId }.orEmpty().toSet()
        change.before.sets.map { it.setId }.filterNot { it in kept }
    }.map { it.removePrefix("set:").toLong() }.toSet()

    private fun ExerciseSnapshot.withWrongTargets() =
        copy(sets = sets.map { it.copy(targetReps = (it.targetReps ?: 0) + 1) })

    private fun ExerciseSet.snapshot() =
        SetSnapshot("set:$id", targetReps, targetWeightKg, targetSeconds, null)

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val MINUTE = 60_000L
        const val BUDGET_MINUTES = 15
        const val ODD_KG = 13.7f
    }
}
