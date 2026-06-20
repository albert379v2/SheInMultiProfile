package com.sheinmulti.profile.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.sheinmulti.profile.data.model.BrowserProfile

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): LiveData<List<BrowserProfile>>

    @Insert
    suspend fun insertProfile(profile: BrowserProfile): Long

    @Update
    suspend fun updateProfile(profile: BrowserProfile)

    @Delete
    suspend fun deleteProfile(profile: BrowserProfile)

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): BrowserProfile?

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun activateProfile(profileId: Long)

    @Query("UPDATE profiles SET lastUsedAt = :timestamp WHERE id = :profileId")
    suspend fun updateLastUsed(profileId: Long, timestamp: Long = System.currentTimeMillis())
}
