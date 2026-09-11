package com.jericx.trainr.data.repository

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
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
        availableEquipment = listOf(Equipment.DUMBBELL),
        workoutDaysPerWeek = 4,
        workoutDuration = 60,
        injuries = emptyList(),
    )

    @Test
    fun `saveUser returns dao-generated id`() = runTest {
        coEvery { userDao.insertUser(any()) } returns 99L

        val id = repository.saveUser(sampleProfile())

        assertThat(id).isEqualTo(99L)
        coVerify { userDao.insertUser(any()) }
    }

    @Test
    fun `hasUsers delegates to dao`() = runTest {
        coEvery { userDao.hasUsers() } returns true

        assertThat(repository.hasUsers()).isTrue()
    }
}
