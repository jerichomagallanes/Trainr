package com.jericx.trainr.data.repository

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.local.WeeklyWorkoutPlanEntity
import com.jericx.trainr.data.local.WorkoutDayEntity
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.model.WorkoutTime
import com.jericx.trainr.domain.model.WorkoutType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private lateinit var userDao: UserDao
    private lateinit var repository: UserRepositoryImpl
    private val mapper = UserMapper()

    @Before
    fun setUp() {
        userDao = mockk(relaxed = true)
        repository = UserRepositoryImpl(userDao, mapper)
    }

    private fun sampleProfile() = UserProfile(
        firstName = "Jericho",
        age = 30,
        gender = Gender.MALE,
        height = 175f,
        weight = 72f,
        fitnessGoal = FitnessGoal.MUSCLE_GAIN,
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        workoutLocation = WorkoutLocation.GYM,
        availableEquipment = listOf(Equipment.DUMBBELLS),
        workoutDaysPerWeek = 4,
        workoutDuration = 60,
        preferredWorkoutTime = WorkoutTime.EVENING,
        injuries = emptyList(),
        workoutType = WorkoutType.STRENGTH
    )

    private fun planEntity(id: Long = 1L) = WeeklyWorkoutPlanEntity(
        id = id,
        userId = 1L,
        weekNumber = 1,
        title = "Week 1",
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun dayEntity(
        id: Long,
        dayNumber: Int,
        status: WorkoutStatus
    ) = WorkoutDayEntity(
        id = id,
        weeklyPlanId = 1L,
        dayNumber = dayNumber,
        title = "Day $dayNumber",
        status = status.name,
        duration = 45,
        exerciseCount = 5,
        equipment = emptyList(),
        completedAt = null
    )

    @Test
    fun `saveUser returns dao-generated id`() = runTest {
        // Arrange
        coEvery { userDao.insertUser(any()) } returns 99L

        // Act
        val id = repository.saveUser(sampleProfile())

        // Assert
        assertThat(id).isEqualTo(99L)
        coVerify { userDao.insertUser(any()) }
    }

    @Test
    fun `getUser returns null when dao returns null`() = runTest {
        // Arrange
        coEvery { userDao.getUserById(5L) } returns null

        // Act
        val result = repository.getUser(5L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getUser maps entity to domain when dao returns one`() = runTest {
        // Arrange
        val profile = sampleProfile().copy(id = 5L)
        coEvery { userDao.getUserById(5L) } returns mapper.mapToEntity(profile)

        // Act
        val result = repository.getUser(5L)

        // Assert
        assertThat(result).isEqualTo(profile)
    }

    @Test
    fun `hasUsers delegates to dao`() = runTest {
        // Arrange
        coEvery { userDao.hasUsers() } returns true

        // Act / Assert
        assertThat(repository.hasUsers()).isTrue()
    }

    @Test
    fun `getWeeklyProgress computes 50 percent when half of days are completed`() = runTest {
        // Arrange: 4 days, 2 completed
        coEvery { userDao.getWeeklyWorkoutPlan(1L, 1) } returns planEntity()
        val days = listOf(
            dayEntity(10L, 1, WorkoutStatus.COMPLETED),
            dayEntity(11L, 2, WorkoutStatus.COMPLETED),
            dayEntity(12L, 3, WorkoutStatus.NOT_STARTED),
            dayEntity(13L, 4, WorkoutStatus.IN_PROGRESS)
        )
        coEvery { userDao.getWorkoutDaysForPlan(1L) } returns days
        coEvery { userDao.getExercisesForWorkoutDay(any()) } returns emptyList()
        coEvery { userDao.getTotalExercisesForDay(any()) } returns 0
        coEvery { userDao.getCompletedExercisesForDay(any()) } returns 0

        // Act
        val progress = repository.getWeeklyProgress(userId = 1L, weekNumber = 1)

        // Assert
        assertThat(progress).isNotNull()
        assertThat(progress!!.completedWorkouts).isEqualTo(2)
        assertThat(progress.totalWorkouts).isEqualTo(4)
        assertThat(progress.completionPercentage).isEqualTo(50f)
    }

    @Test
    fun `getWeeklyProgress returns null when plan is missing`() = runTest {
        // Arrange
        coEvery { userDao.getWeeklyWorkoutPlan(1L, 99) } returns null

        // Act
        val progress = repository.getWeeklyProgress(userId = 1L, weekNumber = 99)

        // Assert
        assertThat(progress).isNull()
    }

    @Test
    fun `getWorkoutDayProgress reports zero percent when day has no exercises`() = runTest {
        // Arrange
        coEvery { userDao.getWorkoutDaysForPlan(1L) } returns listOf(
            dayEntity(20L, 1, WorkoutStatus.NOT_STARTED)
        )
        coEvery { userDao.getTotalExercisesForDay(20L) } returns 0
        coEvery { userDao.getCompletedExercisesForDay(20L) } returns 0

        // Act
        val progress = repository.getWorkoutDayProgress(weeklyPlanId = 1L)

        // Assert
        assertThat(progress).hasSize(1)
        assertThat(progress[0].completionPercentage).isEqualTo(0f)
        assertThat(progress[0].totalExercises).isEqualTo(0)
    }

    @Test
    fun `getWorkoutDayProgress computes percent from completed over total`() = runTest {
        // Arrange: 3 of 4 exercises completed -> 75%
        coEvery { userDao.getWorkoutDaysForPlan(1L) } returns listOf(
            dayEntity(30L, 1, WorkoutStatus.IN_PROGRESS)
        )
        coEvery { userDao.getTotalExercisesForDay(30L) } returns 4
        coEvery { userDao.getCompletedExercisesForDay(30L) } returns 3

        // Act
        val progress = repository.getWorkoutDayProgress(weeklyPlanId = 1L)

        // Assert
        assertThat(progress[0].completionPercentage).isEqualTo(75f)
        assertThat(progress[0].completedExercises).isEqualTo(3)
        assertThat(progress[0].totalExercises).isEqualTo(4)
    }
}
