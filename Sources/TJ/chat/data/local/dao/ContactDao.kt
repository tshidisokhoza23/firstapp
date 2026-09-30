package com.tpsk.tj.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tpsk.tj.chat.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber OR (LENGTH(:phoneNumber) >= 9 AND LENGTH(phoneNumber) >= 9 AND SUBSTR(REPLACE(:phoneNumber, ' ', ''), -9) = SUBSTR(REPLACE(phoneNumber, ' ', ''), -9)) LIMIT 1")
    suspend fun getContactByPhoneNumber(phoneNumber: String): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY isRegistered DESC, displayName ASC")
    fun getContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Query("SELECT * FROM contacts WHERE (LOWER(displayName) LIKE '%' || LOWER(:query) || '%' OR LOWER(username) LIKE '%' || LOWER(:query) || '%' OR phoneNumber LIKE '%' || :query || '%') ORDER BY isRegistered DESC, displayName ASC")
    fun searchContacts(query: String): Flow<List<ContactEntity>>
}
