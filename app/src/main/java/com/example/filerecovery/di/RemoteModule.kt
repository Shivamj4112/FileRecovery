package com.example.filerecovery.di

import android.content.Context
import androidx.room.Room
import com.example.filerecovery.data.datasource.ContactDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context : Context): ContactDatabase {
        return Room.databaseBuilder(
            context,
            ContactDatabase::class.java,
            "Contact.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: ContactDatabase) = database.contactDao()

}