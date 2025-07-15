package com.example.filerecovery.data.datasource

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.filerecovery.data.model.ContactEntity

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE is_deleted = 1")
    suspend fun getDeletedContacts(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("UPDATE contacts SET is_deleted = 1 WHERE id = :contactId")
    suspend fun markContactAsDeleted(contactId: String)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE name = :name AND phone_number = :phoneNumber LIMIT 1")
    fun getContactByNameAndPhone(name: String, phoneNumber: String?): ContactEntity?

    @Query("UPDATE contacts SET is_deleted = 0 WHERE id = :contactId")
    fun markContactAsRestored(contactId: String)

    @Query("UPDATE contacts SET id = :newId WHERE id = :oldId")
    fun updateContactId(oldId: String, newId: String)


    @Query("DELETE FROM contacts")
    suspend fun clearAllContacts()
}