package com.sheinmulti.profile.viewmodel

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.*
import com.sheinmulti.profile.data.local.AppDatabase
import com.sheinmulti.profile.data.model.BrowserProfile
import com.sheinmulti.profile.data.repository.ProfileRepository
import kotlinx.coroutines.launch
import java.io.File

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProfileRepository
    val allProfiles: LiveData<List<BrowserProfile>>
    private val _operationResult = MutableLiveData<String>()
    val operationResult: LiveData<String> = _operationResult

    init {
        val profileDao = AppDatabase.getDatabase(application).profileDao()
        repository = ProfileRepository(profileDao)
        allProfiles = repository.allProfiles
    }

    fun addProfile(
        name: String,
        startUrl: String,
        userAgent: String,
        proxyHost: String?,
        proxyPort: Int?,
        proxyUsername: String?,
        proxyPassword: String?,
        proxyType: String
    ) {
        viewModelScope.launch {
            val suffix = "profile_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val profile = BrowserProfile(
                name = name,
                startUrl = startUrl.ifBlank { "https://www.google.com" },
                userAgent = userAgent,
                proxyHost = proxyHost,
                proxyPort = proxyPort,
                proxyUsername = proxyUsername,
                proxyPassword = proxyPassword,
                proxyType = proxyType,
                dataDirectorySuffix = suffix
            )
            val id = repository.insertProfile(profile)
            _operationResult.value = "Perfil creado"
        }
    }

    fun updateProfile(profile: BrowserProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
            _operationResult.value = "Perfil actualizado"
        }
    }

    fun deleteProfile(profile: BrowserProfile) {
        viewModelScope.launch {
            clearProfileData(profile.dataDirectorySuffix)
            repository.deleteProfile(profile)
            _operationResult.value = "Perfil eliminado"
        }
    }

    fun activateProfile(profileId: Long) {
        viewModelScope.launch {
            repository.activateProfile(profileId)
        }
    }

    fun clearProfileCookies(profile: BrowserProfile) {
        viewModelScope.launch {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            _operationResult.value = "Cookies limpiadas para ${profile.name}"
        }
    }

    private fun clearProfileData(suffix: String) {
        val context = getApplication<Application>().applicationContext
        val webViewDir = File(context.dataDir, "app_webview/$suffix")
        if (webViewDir.exists()) {
            webViewDir.deleteRecursively()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
