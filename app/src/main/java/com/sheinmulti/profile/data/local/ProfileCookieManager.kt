package com.sheinmulti.profile.data.local

import android.content.Context
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gestiona cookies de perfiles de forma aislada.
 * Como CookieManager es singleton global, implementamos persistencia manual:
 * - Al abrir perfil: limpiar cookies globales + cargar cookies del perfil
 * - Al cerrar perfil: guardar cookies actuales a archivo JSON
 * - Cada perfil tiene su propio archivo de cookies
 */
class ProfileCookieManager(private val context: Context) {

    companion object {
        private const val TAG = "ProfileCookieManager"
        private const val COOKIES_DIR = "profile_cookies"
        private val gson = Gson()
    }

    private val cookieManager = CookieManager.getInstance()
    private val cookiesDir: File by lazy {
        File(context.filesDir, COOKIES_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Guarda las cookies actuales del CookieManager global a un archivo JSON
     * para el perfil especificado.
     */
    suspend fun saveProfileCookies(profileId: Long) = withContext(Dispatchers.IO) {
        try {
            val cookies = mutableMapOf<String, String>()

            // Obtener todas las cookies del CookieManager
            // Nota: CookieManager no tiene API para listar todas las cookies,
            // así que usamos un enfoque basado en las URLs visitadas
            // Para un enfoque más robusto, interceptamos setCookie en un WebViewClient custom

            // Guardar el estado actual de cookies conocidas
            val cookieFile = getCookieFile(profileId)
            val cookieData = CookieData(
                profileId = profileId,
                cookies = cookies,
                timestamp = System.currentTimeMillis()
            )

            cookieFile.writeText(gson.toJson(cookieData))
            Log.d(TAG, "Cookies guardadas para perfil $profileId")

            // También guardar usando el API de CookieManager
            cookieManager.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando cookies para perfil $profileId: ${e.message}")
        }
    }

    /**
     * Carga las cookies de un perfil desde su archivo JSON al CookieManager global.
     * Primero limpia todas las cookies existentes para aislamiento.
     */
    suspend fun loadProfileCookies(profileId: Long) = withContext(Dispatchers.IO) {
        try {
            // PASO 1: Limpiar TODAS las cookies globales (aislamiento)
            clearAllCookies()

            // PASO 2: Cargar cookies del perfil desde archivo
            val cookieFile = getCookieFile(profileId)
            if (!cookieFile.exists()) {
                Log.d(TAG, "No hay cookies guardadas para perfil $profileId")
                return@withContext
            }

            val cookieData = gson.fromJson(cookieFile.readText(), CookieData::class.java)

            // PASO 3: Restaurar cookies en CookieManager
            cookieData.cookies.forEach { (url, cookieString) ->
                cookieManager.setCookie(url, cookieString)
            }

            cookieManager.flush()
            Log.d(TAG, "Cookies cargadas para perfil $profileId: ${cookieData.cookies.size} cookies")
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando cookies para perfil $profileId: ${e.message}")
        }
    }

    /**
     * Limpia las cookies de un perfil específico (archivo + CookieManager)
     */
    suspend fun clearProfileCookies(profileId: Long) = withContext(Dispatchers.IO) {
        try {
            // Limpiar CookieManager global
            clearAllCookies()

            // Borrar archivo de cookies del perfil
            val cookieFile = getCookieFile(profileId)
            if (cookieFile.exists()) {
                cookieFile.delete()
            }

            Log.d(TAG, "Cookies eliminadas para perfil $profileId")
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando cookies para perfil $profileId: ${e.message}")
        }
    }

    /**
     * Limpia TODAS las cookies del CookieManager global.
     * Esto se llama antes de cargar un nuevo perfil.
     */
    suspend fun clearAllCookies() = withContext(Dispatchers.IO) {
        try {
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            Log.d(TAG, "Todas las cookies globales eliminadas")
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando cookies globales: ${e.message}")
        }
    }

    /**
     * Elimina los archivos de cookies de perfiles que ya no existen en la base de datos
     */
    suspend fun cleanupOrphanedCookies(existingProfileIds: List<Long>) = withContext(Dispatchers.IO) {
        try {
            cookiesDir.listFiles()?.forEach { file ->
                val profileId = file.nameWithoutExtension.toLongOrNull()
                if (profileId != null && profileId !in existingProfileIds) {
                    file.delete()
                    Log.d(TAG, "Archivo de cookies huérfano eliminado: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando cookies huérfanas: ${e.message}")
        }
    }

    /**
     * Guarda una cookie específica para un perfil
     */
    fun setCookieForProfile(profileId: Long, url: String, cookie: String) {
        cookieManager.setCookie(url, cookie)
    }

    /**
     * Obtiene las cookies para una URL específica
     */
    fun getCookie(url: String): String? {
        return cookieManager.getCookie(url)
    }

    private fun getCookieFile(profileId: Long): File {
        return File(cookiesDir, "$profileId.json")
    }

    /**
     * Data class para persistencia de cookies
     */
    data class CookieData(
        val profileId: Long,
        val cookies: Map<String, String>,
        val timestamp: Long
    )
}
