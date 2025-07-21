package com.jericx.trainr.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.jericx.trainr.common.Constants
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.repository.UserRepositoryImpl
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
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.KEY_SHARED_PREF, Context.MODE_PRIVATE)
    }
}
