package com.example.filerecovery.di

import android.content.Context
import com.example.filerecovery.data.repository.local.FileRecoveryRepository
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
    fun provideFileRecoveryRepository(@ApplicationContext context: Context): FileRecoveryRepository {
        return FileRecoveryRepository(context)
    }
}