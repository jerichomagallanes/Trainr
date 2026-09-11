package com.jericx.trainr.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.data.generation.TemplatePlanGenerator
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.repository.UserRepositoryImpl
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutPersistenceTest {

    private lateinit var db: TrainrDatabase
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TrainrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UserRepositoryImpl(db.userDao, UserMapper())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seedSamplePlan(): Long {
        val userId = repository.saveUser(UserProfile(firstName = "Jericho", age = 30))
        val plan = SampleWorkoutData.weekOne.copy(id = 0, userId = userId)
        repository.saveWeeklyWorkoutPlan(plan)
        return userId
    }

    // The week the app builds needs no column Room does not already have:
    // every target it works out comes back exactly as it went in.
    @Test
    fun aWeekTheAppBuiltComesBackExactlyAsItWasSaved() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = ExerciseCatalogReader.read(
            context.assets.open("exercise-catalog.json").bufferedReader().readText()
        )
        val profile = UserProfile(firstName = "Jericho", age = 30, weight = 80f, availableEquipment = Equipment.entries.toList())
        val userId = repository.saveUser(profile)
        val built = (TemplatePlanGenerator(catalog).generate(PlanRequest(profile.copy(id = userId), 1, 0L))
            as PlanGenerationResult.Generated).plan
        repository.saveWeeklyWorkoutPlan(built.copy(userId = userId))

        val stored = repository.getWeeklyWorkoutPlan(userId, weekNumber = 1)!!

        fun shape(plan: WeeklyWorkoutPlan) = plan.workoutDays.map { day ->
            listOf(day.dayNumber, day.title, day.exercises.map { exercise ->
                listOf(
                    exercise.exerciseKey, exercise.measure, exercise.instructions,
                    exercise.sets.map { listOf(it.targetReps, it.targetWeightKg, it.targetSeconds) }
                )
            })
        }
        assertThat(shape(stored)).isEqualTo(shape(built))
    }

    @Test
    fun aPlanSurvivesTheRoundTripWithItsSets() = runTest {
        val userId = seedSamplePlan()

        val stored = repository.getWeeklyWorkoutPlan(userId, weekNumber = 1)

        assertThat(stored).isNotNull()
        assertThat(stored!!.startDateMillis).isEqualTo(SampleWorkoutData.weekOne.startDateMillis)
        assertThat(stored.workoutDays).hasSize(SampleWorkoutData.weekOne.workoutDays.size)

        SampleWorkoutData.weekOne.workoutDays.forEachIndexed { index, expected ->
            val actual = stored.workoutDays[index]
            assertThat(actual.title).isEqualTo(expected.title)
            assertThat(actual.exercises.map { it.name })
                .isEqualTo(expected.exercises.map { it.name })
            assertThat(actual.exercises.map { it.exerciseKey })
                .isEqualTo(expected.exercises.map { it.exerciseKey })
            assertThat(actual.exercises.map { it.sets.size })
                .isEqualTo(expected.exercises.map { it.sets.size })
        }
    }

    @Test
    fun anExercisesMeasureAndPrescriptionSurvive() = runTest {
        val userId = seedSamplePlan()

        val exercises = repository.getWeeklyWorkoutPlan(userId, 1)!!
            .workoutDays.first { it.title == "Full Body Strength" }.exercises

        val plank = exercises.first { it.name == "Plank" }
        assertThat(plank.measure).isEqualTo(ExerciseMeasure.DURATION)
        assertThat(plank.durationMinutes).isEqualTo(6)
        assertThat(plank.prescription).isEqualTo("3 sets of 45 seconds")
        assertThat(plank.sets.map { it.targetSeconds }).containsExactly(45, 45, 45)
    }

    @Test
    fun aLoggedSetIsStoredAndReadBackInOrder() = runTest {
        val userId = seedSamplePlan()
        val exercise = repository.getWeeklyWorkoutPlan(userId, 1)!!
            .workoutDays.first { it.title == "Lower Body Power" }
            .exercises.first { it.name == "Dumbbell Step-Ups" }

        val second = exercise.sets[1]
        repository.updateExerciseSet(
            second.copy(actualReps = 9, actualWeightKg = 14f, isCompleted = true),
            exercise.id
        )

        val reread = repository.getWorkoutExercise(exercise.id)!!
        assertThat(reread.sets.map { it.setNumber }).containsExactly(1, 2, 3).inOrder()
        with(reread.sets[1]) {
            assertThat(actualReps).isEqualTo(9)
            assertThat(actualWeightKg).isEqualTo(14f)
            assertThat(isCompleted).isTrue()
        }
        assertThat(reread.sets[0].actualReps).isNull()
    }

    @Test
    fun anAddedSetComesBackWithItsGeneratedId() = runTest {
        val userId = seedSamplePlan()
        val exercise = repository.getWeeklyWorkoutPlan(userId, 1)!!
            .workoutDays.first().exercises.first()

        val id = repository.addExerciseSet(
            ExerciseSet(setNumber = exercise.sets.size + 1, targetReps = 12),
            exercise.id
        )

        assertThat(id).isGreaterThan(0)
        val reread = repository.getWorkoutExercise(exercise.id)!!
        assertThat(reread.sets.last().id).isEqualTo(id)
        assertThat(reread.sets.last().setNumber).isEqualTo(exercise.sets.size + 1)
    }

    @Test
    fun previousSetsComeFromTheCompletedDayWithTheSameKey() = runTest {
        val userId = seedSamplePlan()
        val squats = repository.getWeeklyWorkoutPlan(userId, 1)!!
            .workoutDays.first { it.title == "Full Body Strength" }
            .exercises.first { it.exerciseKey == "goblet_squat" }

        val previous = repository.getPreviousSets(
            userId, "goblet_squat", excludeDayId = 0, beforeMillis = Long.MAX_VALUE
        )

        assertThat(previous.map { it.setNumber }).containsExactly(1, 2, 3).inOrder()
        assertThat(previous.map { it.actualReps }).isEqualTo(squats.sets.map { it.actualReps })
        assertThat(previous.map { it.actualWeightKg })
            .isEqualTo(squats.sets.map { it.actualWeightKg })
    }

    @Test
    fun aDayNeverSeesItselfAsPrevious() = runTest {
        val userId = seedSamplePlan()
        val monday = repository.getWeeklyWorkoutPlan(userId, 1)!!
            .workoutDays.first { it.title == "Full Body Strength" }

        assertThat(
            repository.getPreviousSets(userId, "goblet_squat", monday.id, Long.MAX_VALUE)
        ).isEmpty()
    }

    // jump_squat exists only on the NOT_STARTED day: prescribed is not history.
    @Test
    fun anUncompletedDayIsNotHistory() = runTest {
        val userId = seedSamplePlan()

        assertThat(
            repository.getPreviousSets(userId, "jump_squat", 0, Long.MAX_VALUE)
        ).isEmpty()
    }

    @Test
    fun historyStopsStrictlyBeforeTheGivenMoment() = runTest {
        val userId = seedSamplePlan()
        val monday = repository.getWeeklyWorkoutPlan(userId, 1)!!
            .workoutDays.first { it.title == "Full Body Strength" }

        assertThat(
            repository.getPreviousSets(userId, "goblet_squat", 0, monday.completedAt!!)
        ).isEmpty()
    }

    @Test
    fun theLatestOfTwoCompletedDaysWins() = runTest {
        val userId = seedSamplePlan()
        val planId = repository.getWeeklyWorkoutPlan(userId, 1)!!.id
        val laterDay = WorkoutDay(
            dayNumber = 6,
            title = "Later Strength",
            status = WorkoutStatus.COMPLETED,
            duration = 8,
            exerciseCount = 1,
            equipment = listOf("Dumbbells"),
            exercises = listOf(
                WorkoutExercise(
                    exerciseKey = "goblet_squat",
                    name = "Goblet Squats",
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    sets = listOf(
                        ExerciseSet(
                            setNumber = 1,
                            targetReps = 12,
                            actualReps = 10,
                            actualWeightKg = 22.5f,
                            isCompleted = true
                        )
                    ),
                    durationMinutes = 8,
                    prescription = "1 set of 12 reps",
                    instructions = "Squat again, heavier.",
                    isCompleted = true
                )
            ),
            completedAt = SampleWorkoutData.dateOf(6)
        )
        repository.saveWorkoutDay(laterDay, planId)

        val previous = repository.getPreviousSets(userId, "goblet_squat", 0, Long.MAX_VALUE)

        assertThat(previous.single().actualWeightKg).isEqualTo(22.5f)
        assertThat(previous.single().actualReps).isEqualTo(10)
    }

    // A day slid complete logs nothing, so it must not shadow older logged numbers.
    @Test
    fun aCompletedDayWithNothingLoggedDoesNotHideOlderLogs() = runTest {
        val userId = seedSamplePlan()
        val planId = repository.getWeeklyWorkoutPlan(userId, 1)!!.id
        val squats = repository.getWeeklyWorkoutPlan(userId, 1)!!
            .workoutDays.first { it.title == "Full Body Strength" }
            .exercises.first { it.exerciseKey == "goblet_squat" }
        val unlogged = WorkoutDay(
            dayNumber = 6,
            title = "Slid Complete",
            status = WorkoutStatus.COMPLETED,
            duration = 8,
            exerciseCount = 1,
            equipment = listOf("Dumbbells"),
            exercises = listOf(
                WorkoutExercise(
                    exerciseKey = "goblet_squat",
                    name = "Goblet Squats",
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    sets = listOf(ExerciseSet(setNumber = 1, targetReps = 12)),
                    durationMinutes = 8,
                    prescription = "1 set of 12 reps",
                    instructions = "Squat.",
                    isCompleted = true
                )
            ),
            completedAt = SampleWorkoutData.dateOf(6)
        )
        repository.saveWorkoutDay(unlogged, planId)

        val previous = repository.getPreviousSets(userId, "goblet_squat", 0, Long.MAX_VALUE)

        assertThat(previous.map { it.actualReps }).isEqualTo(squats.sets.map { it.actualReps })
    }

    @Test
    fun aDeletedSetStaysDeleted() = runTest {
        val userId = seedSamplePlan()
        val exercise = repository.getWeeklyWorkoutPlan(userId, 1)!!
            .workoutDays.first().exercises.first()

        repository.deleteExerciseSet(exercise.sets[1].id)

        val reread = repository.getWorkoutExercise(exercise.id)!!
        assertThat(reread.sets).hasSize(exercise.sets.size - 1)
        assertThat(reread.sets.map { it.id }).doesNotContain(exercise.sets[1].id)
    }

    // Redoing onboarding REPLACEs the user row, which must cascade the old plan away.
    @Test
    fun replacingAUserCascadesAwayTheirOldPlan() = runTest {
        val userId = seedSamplePlan()

        repository.saveUser(repository.getCurrentUser()!!.copy(firstName = "Again"))

        assertThat(repository.getWeeklyWorkoutPlan(userId, 1)).isNull()
    }
    // Two slow generations can both pass an existence check, so the database holds the line.
    @Test
    fun aClientCannotEndUpWithTwoOfTheSameWeek() = runTest {
        val userId = seedSamplePlan()
        val weekTwo = SampleWorkoutData.weekOne.copy(
            id = 0,
            userId = userId,
            weekNumber = 2,
            title = "Second week"
        )

        repository.saveWeeklyWorkoutPlan(weekTwo)
        repository.saveWeeklyWorkoutPlan(weekTwo.copy(title = "Second week again"))

        val stored = repository.getWeeklyWorkoutPlans(userId).first()
        assertThat(stored.count { it.weekNumber == 2 }).isEqualTo(1)
        assertThat(stored.map { it.weekNumber }).containsExactly(1, 2)
    }

}
