package com.example.firstapp.common.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.common.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts")
    fun getContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Query("SELECT * FROM contacts WHERE LOWER(displayName) LIKE '%' || LOWER(:query) || '%' OR LOWER(username) LIKE '%' || LOWER(:query) || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun searchContacts(query: String): Flow<List<ContactEntity>>
}
