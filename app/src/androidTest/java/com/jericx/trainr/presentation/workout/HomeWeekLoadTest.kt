package com.jericx.trainr.presentation.workout

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.data.generation.WeekPlanGenerator
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UnstuckMapper
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.repository.AdjustmentRepositoryImpl
import com.jericx.trainr.data.repository.UserRepositoryImpl
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// Home draws one week, so it must read one week. Reading the whole history
// made opening the app cost a fiftieth-week client fifty times what it costs a
// new one, which is why the assertions here are on counted reads rather than
// on milliseconds: a duration passes on a quiet machine whatever the shape.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeWeekLoadTest {

    private lateinit var db: TrainrDatabase
    private lateinit var users: UserRepositoryImpl
    private lateinit var adjustments: AdjustmentRepositoryImpl
    private lateinit var catalog: ExerciseCatalog
    private val queries = AtomicInteger()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        catalog = ExerciseCatalogReader.read(
            context.assets.open("exercise-catalog.json").bufferedReader().readText()
        )
        db = Room.inMemoryDatabaseBuilder(context, TrainrDatabase::class.java)
            // Room's own invalidation bookkeeping runs on its own schedule and
            // would make the count depend on when it last fired.
            .setQueryCallback(
                { sql, _ ->
                    if (sql.startsWith("SELECT") && !sql.contains("room_table_modification_log")) {
                        queries.incrementAndGet()
                    }
                },
                Runnable::run
            )
            .build()
        users = UserRepositoryImpl(db, db.userDao, UserMapper())
        adjustments = AdjustmentRepositoryImpl(
            db, db.userDao, db.unstuckDao, UnstuckMapper(), UserMapper(), catalog
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private val profile = UserProfile(
        firstName = "Jericho",
        age = 30,
        weight = 80f,
        availableEquipment = Equipment.entries.toList()
    )

    private val thisMonday: Long
        get() = WorkoutWeek.startOfDay().let {
            WorkoutWeek.dateOfDay(it, 2 - WorkoutWeek.isoWeekdayOf(it))
        }

    // Generated once from a fixed profile: the week a client is given depends on
    // who they are, and two histories can only be compared week for week.
    private var generated: WeeklyWorkoutPlan? = null

    private suspend fun week(): WeeklyWorkoutPlan = generated ?: run {
        val built = WeekPlanGenerator(catalog).generate(PlanRequest(profile.copy(id = 1), 1, 0L))
        (built as PlanGenerationResult.Generated).plan.also { generated = it }
    }

    // Every week holds the same session, so a week's own content cannot move
    // the count and only the length of the history can.
    private suspend fun seed(weeks: Int): Long {
        val userId = users.saveUser(profile)
        val week = week()
        (1..weeks).forEach { number ->
            users.saveWeeklyWorkoutPlan(
                week.copy(
                    id = 0,
                    userId = userId,
                    weekNumber = number,
                    startDateMillis = thisMonday - TimeUnit.DAYS.toMillis(7L * (weeks - number))
                )
            )
        }
        return userId
    }

    private class Load<T>(val queries: Int, val millis: Long, val state: T)

    private suspend fun <T> measure(load: suspend () -> T): Load<T> {
        queries.set(0)
        val startedAt = System.nanoTime()
        val state = load()
        return Load(queries.get(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt), state)
    }

    private suspend fun loadHome(weekNumber: Int? = null) = measure {
        val handle = SavedStateHandle(
            weekNumber?.let { mapOf<String, Any?>(Screen.WeekPlan.ARG_WEEK_NUMBER to it) }
                ?: emptyMap()
        )
        WeeklyPlanViewModel(handle, users, adjustments, catalog).uiState.first { it.hasLoaded }
    }

    private suspend fun loadProgress() = measure {
        WeeklyProgressViewModel(users).uiState.first { it.hasLoaded }
    }

    private fun shapeOf(state: WeeklyPlanUiState) = state.days.map {
        listOf(it.day.title, it.minutes, it.exerciseCount, it.equipment, it.dateMillis, it.isToday)
    }

    @Test
    fun openingHomeCostsTheSameAtFiftyWeeksAsAtOne() = runBlocking {
        seed(1)
        val one = loadHome()
        db.clearAllTables()
        seed(10)
        val ten = loadHome()
        db.clearAllTables()
        seed(50)
        val fifty = loadHome()

        Log.i(
            "HomeWeekLoadTest",
            "home: 1 week ${one.queries} queries ${one.millis}ms, " +
                "10 weeks ${ten.queries} queries ${ten.millis}ms, " +
                "50 weeks ${fifty.queries} queries ${fifty.millis}ms"
        )

        assertThat(ten.queries).isEqualTo(one.queries)
        assertThat(fifty.queries).isEqualTo(one.queries)
        assertThat(fifty.state.plan.weekNumber).isEqualTo(50)
        assertThat(shapeOf(fifty.state)).isEqualTo(shapeOf(one.state))
    }

    // The one place a second week is read: the week asked for is not the newest,
    // and whether another may be added is a fact about the newest.
    @Test
    fun browsingAnOldWeekReadsThatWeekAndTheNewest() = runBlocking {
        seed(50)
        val browsed = loadHome(weekNumber = 1)
        db.clearAllTables()
        seed(2)
        val shortHistory = loadHome(weekNumber = 1)

        Log.i(
            "HomeWeekLoadTest",
            "browsed week 1: of 50 ${browsed.queries} queries ${browsed.millis}ms, " +
                "of 2 ${shortHistory.queries} queries ${shortHistory.millis}ms"
        )

        assertThat(browsed.queries).isEqualTo(shortHistory.queries)
        assertThat(browsed.state.plan.weekNumber).isEqualTo(1)
        assertThat(browsed.state.isCurrentWeek).isFalse()
    }

    // The screen the history belongs to: every week must still reach it. How
    // many reads that takes is the screen's own business and may yet drop.
    @Test
    fun weeklyProgressStillReadsEveryWeek() = runBlocking {
        seed(1)
        val one = loadProgress()
        db.clearAllTables()
        seed(10)
        val ten = loadProgress()
        db.clearAllTables()
        seed(50)
        val fifty = loadProgress()

        Log.i(
            "HomeWeekLoadTest",
            "progress: 1 week ${one.queries} queries ${one.millis}ms, " +
                "10 weeks ${ten.queries} queries ${ten.millis}ms, " +
                "50 weeks ${fifty.queries} queries ${fifty.millis}ms"
        )

        assertThat(ten.state.weeks).hasSize(10)
        assertThat(fifty.state.weeks).hasSize(50)
        assertThat(fifty.state.weeks.map { it.totalDays }.toSet())
            .containsExactlyElementsIn(one.state.weeks.map { it.totalDays }.toSet())
        assertThat(fifty.state.weeks.map { it.completedDays }.toSet())
            .containsExactlyElementsIn(one.state.weeks.map { it.completedDays }.toSet())
        assertThat(fifty.state.weeks.map { it.weekNumber }).isInStrictOrder()
    }

    // A past day's chip reads the work logged on it, which lives two tables
    // below the day's own row: the one week home reads is still read whole.
    @Test
    fun aPastDayStillKnowsWhetherWorkWasLoggedOnIt() = runBlocking {
        val userId = users.saveUser(profile)
        val lastMonday = thisMonday - TimeUnit.DAYS.toMillis(7)
        users.saveWeeklyWorkoutPlan(
            week().copy(id = 0, userId = userId, weekNumber = 1, startDateMillis = lastMonday)
        )
        val stored: WeeklyWorkoutPlan = checkNotNull(users.getLatestWeeklyWorkoutPlan(userId))
        val worked = stored.workoutDays.first()
        val exercise = worked.exercises.first()
        users.updateExerciseSet(exercise.sets.first().copy(isCompleted = true), exercise.id)

        val state = loadHome().state
        val days = state.days.associateBy { it.day.id }

        assertThat(state.days.all { it.isPast }).isTrue()
        assertThat(days.getValue(worked.id).isPartlyDone).isTrue()
        assertThat(days.getValue(worked.id).isMissed).isFalse()
        assertThat(state.days.filter { it.day.id != worked.id }.all { it.isMissed }).isTrue()
    }
}
