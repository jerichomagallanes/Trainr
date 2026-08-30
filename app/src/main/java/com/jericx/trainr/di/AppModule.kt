package com.jericx.trainr.di

import android.content.Context
import androidx.room.Room
import com.jericx.trainr.BuildConfig
import com.jericx.trainr.common.Constants
import com.jericx.trainr.data.generation.GeminiClient
import com.jericx.trainr.data.generation.GeminiPlanGenerator
import com.jericx.trainr.data.generation.GeneratedPlanParser
import com.jericx.trainr.data.generation.PlanPromptBuilder
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.preferences.LanguageCodeProvider
import com.jericx.trainr.data.preferences.LanguagePreferences
import com.jericx.trainr.data.repository.UserRepositoryImpl
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.model.ExerciseVideoCatalog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTrainrDatabase(@ApplicationContext context: Context): TrainrDatabase {
        return Room.databaseBuilder(
            context,
            TrainrDatabase::class.java,
            Constants.DATABASE_NAME
        ).addMigrations(
            TrainrDatabase.MIGRATION_1_2,
            TrainrDatabase.MIGRATION_2_3,
            TrainrDatabase.MIGRATION_3_4
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: TrainrDatabase): UserDao {
        return database.userDao
    }

    @Provides
    @Singleton
    fun provideUserMapper(): UserMapper {
        return UserMapper()
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        mapper: UserMapper
    ): UserRepository {
        return UserRepositoryImpl(userDao, mapper)
    }

    @Provides
    @Singleton
    fun providePlanGenerator(): PlanGenerator {
        return GeminiPlanGenerator(
            client = GeminiClient(apiKey = BuildConfig.GEMINI_API_KEY),
            parser = GeneratedPlanParser(),
            promptBuilder = PlanPromptBuilder(
                canonicalKeys = ExerciseVideoCatalog.videoIds.keys
            )
        )
    }

    @Provides
    @Singleton
    fun provideLanguageCodeProvider(@ApplicationContext context: Context): LanguageCodeProvider {
        return LanguageCodeProvider {
            LanguagePreferences(context).getCurrentLanguageObject(context).code
        }
    }
}
