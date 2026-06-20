package com.sheinmulti.profile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class BrowserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startUrl: String = "https://www.google.com",
    val userAgent: String,
    val proxyHost: String? = null,
    val proxyPort: Int? = null,
    val proxyUsername: String? = null,
    val proxyPassword: String? = null,
    val proxyType: String = "NONE",
    val dataDirectorySuffix: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
)
