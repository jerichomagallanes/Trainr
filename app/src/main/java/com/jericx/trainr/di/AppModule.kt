package com.jericx.trainr.di

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import androidx.room.Room
import com.jericx.trainr.common.Constants
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.local.UnstuckDao
import com.jericx.trainr.data.local.UnstuckMapper
import com.jericx.trainr.data.preferences.ThemePreferences
import com.jericx.trainr.data.ads.Ads
import com.jericx.trainr.data.purchases.Entitlements
import com.jericx.trainr.data.purchases.StoredAdjustmentAllowance
import com.jericx.trainr.data.purchases.StoredGenerationAllowance
import com.jericx.trainr.data.repository.UserRepositoryImpl
import com.jericx.trainr.data.repository.AdjustmentRepositoryImpl
import com.jericx.trainr.data.generation.WeekPlanGenerator
import com.jericx.trainr.data.model.DeviceEligibility
import com.jericx.trainr.data.model.HttpModelSource
import com.jericx.trainr.data.model.LlamaIntentInterpreter
import com.jericx.trainr.data.model.ModelInstaller
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.data.diagnostics.CrashlyticsBreadcrumbs
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.data.catalog.AssetExerciseCatalog
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.purchases.AdjustmentAllowance
import com.jericx.trainr.domain.purchases.AdjustmentGate
import com.jericx.trainr.domain.purchases.FreeGenerationAllowance
import com.jericx.trainr.domain.purchases.ProGate
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.unstuck.intent.IntentInterpreter
import com.jericx.trainr.domain.unstuck.intent.LocalModelInstaller
import com.jericx.trainr.llama.LlamaEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

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
            .addMigrations(*TrainrDatabase.MIGRATIONS)
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, *TrainrDatabase.LEGACY_VERSIONS)
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
    fun provideAds(breadcrumbs: Breadcrumbs): Ads = Ads(breadcrumbs)

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
    fun provideAdjustmentAllowance(
        @ApplicationContext context: Context
    ): AdjustmentAllowance = StoredAdjustmentAllowance(context)

    @Provides
    @Singleton
    fun provideAdjustmentGate(
        entitlements: Entitlements,
        allowance: AdjustmentAllowance
    ): AdjustmentGate = AdjustmentGate(
        isPro = { entitlements.isPro.value },
        canSell = { entitlements.canSell },
        allowance = allowance
    )

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
    fun provideUnstuckDao(database: TrainrDatabase): UnstuckDao {
        return database.unstuckDao
    }

    @Provides
    @Singleton
    fun provideUnstuckMapper(): UnstuckMapper {
        return UnstuckMapper()
    }

    @Provides
    @Singleton
    fun provideAdjustmentRepository(
        database: TrainrDatabase,
        userDao: UserDao,
        dao: UnstuckDao,
        mapper: UnstuckMapper,
        userMapper: UserMapper,
        catalog: ExerciseCatalog
    ): AdjustmentRepository {
        return AdjustmentRepositoryImpl(database, userDao, dao, mapper, userMapper, catalog)
    }

    @Provides
    @Singleton
    fun providePlanGenerator(catalog: ExerciseCatalog): PlanGenerator =
        WeekPlanGenerator(catalog)

    @Provides
    @Singleton
    fun provideExerciseCatalog(@ApplicationContext context: Context): ExerciseCatalog =
        AssetExerciseCatalog(context)

    @Provides
    @Singleton
    fun provideDeviceEligibility(@ApplicationContext context: Context): DeviceEligibility =
        DeviceEligibility.of(context)

    @Provides
    @Singleton
    fun provideModelInstaller(
        @ApplicationContext context: Context,
        eligibility: DeviceEligibility
    ): ModelInstaller {
        val directory = context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
        directory.mkdirs()
        return ModelInstaller(
            directory = directory,
            eligibility = eligibility,
            source = HttpModelSource(),
            freeSpace = { StatFs(it.path).availableBytes },
            dispatcher = Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun provideLocalModelInstaller(installer: ModelInstaller): LocalModelInstaller = installer

    @Provides
    @Singleton
    fun provideLlamaEngine(
        @ApplicationContext context: Context,
        installer: ModelInstaller
    ): LlamaEngine {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        return LlamaEngine(
            modelFile = installer::readyFile,
            nativeLibraryDir = context.applicationInfo.nativeLibraryDir,
            availableMemory = {
                ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo).availMem
            },
            dispatcher = Dispatchers.IO
        ).also(context::registerComponentCallbacks)
    }

    @Provides
    @Singleton
    fun provideIntentInterpreter(
        installer: ModelInstaller,
        eligibility: DeviceEligibility,
        engine: LlamaEngine
    ): IntentInterpreter = LlamaIntentInterpreter(installer, eligibility, engine)

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }
}
