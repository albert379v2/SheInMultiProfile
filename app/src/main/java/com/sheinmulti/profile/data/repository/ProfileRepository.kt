package com.sheinmulti.profile.data.repository

import androidx.lifecycle.LiveData
import com.sheinmulti.profile.data.local.ProfileDao
import com.sheinmulti.profile.data.model.BrowserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(private val profileDao: ProfileDao) {
    val allProfiles: LiveData<List<BrowserProfile>> = profileDao.getAllProfiles()

    suspend fun insertProfile(profile: BrowserProfile): Long = withContext(Dispatchers.IO) {
        profileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: BrowserProfile) = withContext(Dispatchers.IO) {
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: BrowserProfile) = withContext(Dispatchers.IO) {
        profileDao.deleteProfile(profile)
    }

    suspend fun getProfileById(id: Long): BrowserProfile? = withContext(Dispatchers.IO) {
        profileDao.getProfileById(id)
    }

    suspend fun getProfileCount(): Int = withContext(Dispatchers.IO) {
        profileDao.getProfileCount()
    }

    suspend fun activateProfile(profileId: Long) = withContext(Dispatchers.IO) {
        profileDao.deactivateAllProfiles()
        profileDao.activateProfile(profileId)
        profileDao.updateLastUsed(profileId)
    }

    suspend fun updateLastUsed(profileId: Long) = withContext(Dispatchers.IO) {
        profileDao.updateLastUsed(profileId)
    }
}
