package com.jericx.trainr.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UnstuckMapper
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.repository.AdjustmentRepositoryImpl
import com.jericx.trainr.data.repository.UserRepositoryImpl
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.unstuck.ActualOrigin
import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ExerciseSnapshot
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.domain.unstuck.FinishKind
import com.jericx.trainr.domain.unstuck.PreferenceKind
import com.jericx.trainr.domain.unstuck.ProposalChange
import com.jericx.trainr.domain.unstuck.ReasonCode
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.domain.unstuck.SetSnapshot
import com.jericx.trainr.domain.unstuck.TrainingPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnstuckPersistenceTest {

    private lateinit var db: TrainrDatabase
    private lateinit var workouts: UserRepositoryImpl
    private lateinit var adjustments: AdjustmentRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TrainrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workouts = UserRepositoryImpl(db.userDao, UserMapper())
        adjustments = AdjustmentRepositoryImpl(db.unstuckDao, UnstuckMapper())
    }

    @After
    fun tearDown() = db.close()

    private fun exercise(key: String, sets: Int = 2) = WorkoutExercise(
        exerciseKey = key,
        name = key,
        sets = (1..sets).map { ExerciseSet(setNumber = it, targetReps = 10) }
    )

    private suspend fun seedDay(vararg keys: String): Pair<Long, WorkoutDay> {
        val userId = workouts.saveUser(UserProfile(firstName = "Jericho", age = 30))
        val plan = WeeklyWorkoutPlan(
            userId = userId,
            weekNumber = 1,
            title = "Foundation",
            workoutDays = listOf(
                WorkoutDay(
                    dayNumber = 1,
                    title = "Full body",
                    duration = 30,
                    exerciseCount = keys.size,
                    equipment = emptyList(),
                    exercises = keys.map { exercise(it) }
                )
            )
        )
        workouts.saveWeeklyWorkoutPlan(plan)
        val day = workouts.getWeeklyWorkoutPlan(userId, 1)!!.workoutDays.single()
        return userId to day
    }

    private fun proposal(id: String) = AdjustmentProposal(
        proposalId = id,
        requestId = "request-$id",
        sessionId = "session-1",
        baseRevision = "revision-1",
        policyVersion = "policy-test",
        changes = listOf(
            ProposalChange(
                kind = ChangeKind.OMIT_UNPERFORMED,
                before = ExerciseSnapshot(
                    exerciseInstanceId = "exercise-1",
                    catalogKey = "goblet_squat",
                    sets = listOf(SetSnapshot("set-2", 10, 12.5f, null, 60))
                ),
                after = null
            )
        ),
        preservedPerformedSetIds = listOf("set-1"),
        reasonCode = ReasonCode.TIME_CONSTRAINT,
        tradeoffCode = "less_lower_priority_work",
        factReferences = listOf("confirmed-time-budget")
    )

    private fun applied(dayId: Long, proposalId: String = "proposal-1") = AppliedAdjustment(
        workoutDayId = dayId,
        proposal = proposal(proposalId),
        reason = AdjustmentReason.LESS_TIME,
        appliedAt = 1_700_000_000_000
    )

    @Test
    fun aSavedWeekReadsItsExercisesBackInTheOrderTheyWereWritten() = runTest {
        val (_, day) = seedDay("goblet_squat", "push_up", "bent_over_row")

        assertThat(day.exercises.map { it.exerciseKey })
            .containsExactly("goblet_squat", "push_up", "bent_over_row").inOrder()
        assertThat(day.exercises.map { it.sortOrder }).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun exercisesComeBackBySortOrderNotByInsertion() = runTest {
        val (_, day) = seedDay()
        workouts.saveWorkoutExercise(exercise("last").copy(sortOrder = 2), day.id)
        workouts.saveWorkoutExercise(exercise("first").copy(sortOrder = 0), day.id)
        workouts.saveWorkoutExercise(exercise("middle").copy(sortOrder = 1), day.id)

        val reread = workouts.getExercisesForWorkoutDay(day.id)

        assertThat(reread.map { it.exerciseKey }).containsExactly("first", "middle", "last").inOrder()
    }

    @Test
    fun omittingSetsLeavesACompletedOneInTheDay() = runTest {
        val (_, day) = seedDay("goblet_squat")
        val exercise = day.exercises.single()
        val done = exercise.sets[0].copy(actualReps = 10, isCompleted = true, actualOrigin = ActualOrigin.TYPED)
        workouts.updateExerciseSet(done, exercise.id)
        val adjustmentId = adjustments.recordAdjustment(applied(day.id))

        val changed = db.userDao.omitSets(exercise.sets.map { it.id }, adjustmentId)

        assertThat(changed).isEqualTo(1)
        val reread = workouts.getWorkoutExercise(exercise.id)!!
        assertThat(reread.sets[0].omittedBy).isNull()
        assertThat(reread.sets[0].actualReps).isEqualTo(10)
        assertThat(reread.sets[0].actualOrigin).isEqualTo(ActualOrigin.TYPED)
        assertThat(reread.sets[1].omittedBy).isEqualTo(adjustmentId)
        assertThat(reread.isOmittedToday).isFalse()

        assertThat(db.userDao.restoreOmittedSets(adjustmentId)).isEqualTo(1)
        assertThat(workouts.getWorkoutExercise(exercise.id)!!.sets.map { it.omittedBy })
            .containsExactly(null, null)
    }

    @Test
    fun anOutcomeRoundTripsAndASecondSaveReplacesTheFirst() = runTest {
        val (_, day) = seedDay("goblet_squat")
        val outcome = SessionOutcome(
            workoutDayId = day.id,
            finishKind = FinishKind.PARTIAL,
            finishedAt = 1_700_000_000_000,
            performedSetCount = 3,
            plannedSetCount = 6
        )

        val id = adjustments.saveOutcome(outcome)
        adjustments.saveOutcome(outcome.copy(finishKind = FinishKind.FULL, performedSetCount = 6))

        val stored = adjustments.getOutcome(day.id)!!
        assertThat(id).isGreaterThan(0)
        assertThat(stored.finishKind).isEqualTo(FinishKind.FULL)
        assertThat(stored.performedSetCount).isEqualTo(6)
        assertThat(stored.plannedSetCount).isEqualTo(6)
        assertThat(adjustments.getOutcomes(listOf(day.id))).hasSize(1)
    }

    @Test
    fun anAdjustmentRoundTripsWithItsProposalAndUndoState() = runTest {
        val (_, day) = seedDay("goblet_squat")

        val id = adjustments.recordAdjustment(applied(day.id))

        val stored = adjustments.getAdjustment("proposal-1")!!
        assertThat(stored.id).isEqualTo(id)
        assertThat(stored.proposal).isEqualTo(proposal("proposal-1"))
        assertThat(stored.reason).isEqualTo(AdjustmentReason.LESS_TIME)
        assertThat(stored.isActive).isTrue()
        assertThat(adjustments.getActiveAdjustment(day.id)?.id).isEqualTo(id)

        adjustments.markUndone(id, 1_700_000_100_000)
        assertThat(adjustments.getActiveAdjustment(day.id)).isNull()
        assertThat(adjustments.getAdjustment("proposal-1")!!.undoneAt).isEqualTo(1_700_000_100_000)

        adjustments.markReapplied(id)
        assertThat(adjustments.getActiveAdjustment(day.id)?.id).isEqualTo(id)
        assertThat(adjustments.getAdjustments(day.id)).hasSize(1)
    }

    @Test
    fun theSameProposalCannotBeRecordedTwice() = runTest {
        val (_, day) = seedDay("goblet_squat")
        adjustments.recordAdjustment(applied(day.id))

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { adjustments.recordAdjustment(applied(day.id)) }
        }

        assertThat(adjustments.getAdjustments(day.id)).hasSize(1)
    }

    @Test
    fun feedbackRoundTripsAndAnAnswerReplacesADismissal() = runTest {
        val (_, day) = seedDay("goblet_squat")
        val adjustmentId = adjustments.recordAdjustment(applied(day.id))
        val dismissed = AdjustmentFeedback(
            adjustmentId = adjustmentId,
            answer = null,
            answeredAt = null,
            dismissedAt = 1_700_000_200_000
        )

        adjustments.saveFeedback(dismissed)
        adjustments.saveFeedback(
            dismissed.copy(answer = FeedbackAnswer.HELPED, answeredAt = 1_700_000_300_000, dismissedAt = null)
        )

        val stored = adjustments.getFeedback(adjustmentId)!!
        assertThat(stored.answer).isEqualTo(FeedbackAnswer.HELPED)
        assertThat(stored.answeredAt).isEqualTo(1_700_000_300_000)
        assertThat(stored.dismissedAt).isNull()
    }

    @Test
    fun aPreferenceRoundTripsThroughSaveUpdateAndDelete() = runTest {
        val (userId, _) = seedDay("goblet_squat")
        val preference = TrainingPreference(
            userId = userId,
            kind = PreferenceKind.TIME_LIMIT,
            minutes = 25,
            weekday = 3,
            sourceAdjustmentId = null,
            confirmedAt = 1_700_000_000_000,
            updatedAt = 1_700_000_000_000
        )

        val id = adjustments.savePreference(preference)
        adjustments.updatePreference(preference.copy(id = id, minutes = 30, updatedAt = 1_700_000_400_000))

        val stored = adjustments.getPreferences(userId).single()
        assertThat(stored.id).isEqualTo(id)
        assertThat(stored.minutes).isEqualTo(30)
        assertThat(stored.weekday).isEqualTo(3)
        assertThat(adjustments.observePreferences(userId).first()).containsExactly(stored)

        adjustments.deletePreference(id)
        assertThat(adjustments.getPreferences(userId)).isEmpty()
    }

    @Test
    fun aNoteRoundTripsThroughSaveUpdateAndDelete() = runTest {
        val (userId, day) = seedDay("goblet_squat")
        val note = SessionNote(
            userId = userId,
            workoutDayId = day.id,
            text = "Knee felt fine today",
            createdAt = 1_700_000_000_000,
            updatedAt = 1_700_000_000_000
        )

        val id = adjustments.saveNote(note)
        adjustments.updateNote(note.copy(id = id, text = "Knee felt fine", updatedAt = 1_700_000_500_000))

        val stored = adjustments.getNote(day.id)!!
        assertThat(stored.id).isEqualTo(id)
        assertThat(stored.text).isEqualTo("Knee felt fine")
        assertThat(adjustments.observeNotes(userId).first()).containsExactly(stored)

        adjustments.deleteNote(id)
        assertThat(adjustments.getNote(day.id)).isNull()
        assertThat(adjustments.observeNotes(userId).first()).isEmpty()
    }

    @Test
    fun deletingADayTakesItsOutcomeAndAdjustmentButLeavesTheNoteText() = runTest {
        val (userId, day) = seedDay("goblet_squat")
        val planId = workouts.getWeeklyWorkoutPlan(userId, 1)!!.id
        adjustments.saveOutcome(
            SessionOutcome(workoutDayId = day.id, finishKind = FinishKind.FULL, finishedAt = 1L, performedSetCount = 2, plannedSetCount = 2)
        )
        val adjustmentId = adjustments.recordAdjustment(applied(day.id))
        adjustments.saveFeedback(AdjustmentFeedback(adjustmentId = adjustmentId, answer = FeedbackAnswer.HELPED, answeredAt = 2L, dismissedAt = null))
        val noteId = adjustments.saveNote(
            SessionNote(userId = userId, workoutDayId = day.id, text = "Kept", createdAt = 1L, updatedAt = 1L)
        )

        workouts.deleteWeeklyWorkoutPlan(planId)

        assertThat(adjustments.getOutcome(day.id)).isNull()
        assertThat(adjustments.getAdjustments(day.id)).isEmpty()
        assertThat(adjustments.getFeedback(adjustmentId)).isNull()
        val note = adjustments.observeNotes(userId).first().single()
        assertThat(note.id).isEqualTo(noteId)
        assertThat(note.text).isEqualTo("Kept")
        assertThat(note.workoutDayId).isNull()
    }
}
