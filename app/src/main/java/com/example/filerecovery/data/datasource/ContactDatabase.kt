package com.example.filerecovery.data.datasource

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.filerecovery.data.model.ContactEntity

@Database(entities = [ContactEntity::class], version = 1, exportSchema = false)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}