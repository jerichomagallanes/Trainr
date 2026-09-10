package com.jericx.trainr.di

import android.content.Context
import androidx.room.Room
import com.jericx.trainr.common.Constants
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.preferences.ThemePreferences
import com.jericx.trainr.data.purchases.Entitlements
import com.jericx.trainr.data.purchases.StoredGenerationAllowance
import com.jericx.trainr.data.repository.UserRepositoryImpl
import com.jericx.trainr.data.generation.planGenerator
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.data.diagnostics.CrashlyticsBreadcrumbs
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.data.generation.DailySpentModels
import com.jericx.trainr.data.catalog.AssetExerciseCatalog
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.SpentModels
import com.jericx.trainr.domain.purchases.FreeGenerationAllowance
import com.jericx.trainr.domain.purchases.ProGate
import com.jericx.trainr.domain.repository.UserRepository
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
        )
            // Nothing is in production, so a schema change resets the local
            // database rather than earning a migration. This has to become a
            // real migration before the first release: left here, the first
            // schema change after launch silently wipes every client.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideBreadcrumbs(): Breadcrumbs = CrashlyticsBreadcrumbs()

    @Provides
    @Singleton
    fun provideEntitlements(
        @ApplicationContext context: Context,
        breadcrumbs: Breadcrumbs
    ): Entitlements = Entitlements(context, breadcrumbs)

    @Provides
    @Singleton
    fun provideGenerationAllowance(
        @ApplicationContext context: Context
    ): FreeGenerationAllowance = StoredGenerationAllowance(context)

    @Provides
    @Singleton
    fun provideProGate(
        entitlements: Entitlements,
        allowance: FreeGenerationAllowance
    ): ProGate = ProGate(
        isPro = { entitlements.isPro.value },
        canSell = { entitlements.canSell },
        allowance = allowance
    )

    @Provides
    @Singleton
    fun provideSpentModels(@ApplicationContext context: Context): SpentModels =
        DailySpentModels(context)

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
    // Which generator answers is a property of the build: see planGenerator()
    // in the dev and prod source sets.
    fun providePlanGenerator(
        catalog: ExerciseCatalog,
        spentModels: SpentModels,
        breadcrumbs: Breadcrumbs
    ): PlanGenerator = planGenerator(catalog, spentModels, breadcrumbs)

    @Provides
    @Singleton
    fun provideExerciseCatalog(@ApplicationContext context: Context): ExerciseCatalog =
        AssetExerciseCatalog(context)

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }
}
