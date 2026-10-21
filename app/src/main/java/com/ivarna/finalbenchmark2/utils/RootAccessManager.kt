package com.ivarna.finalbenchmark2.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Singleton class to manage root and Shizuku access state across the application
 */
object RootAccessManager {
    private var cachedRootState: Boolean? = null
    private var cachedShizukuState: Boolean? = null
    private val mutex = Mutex()

    /**
     * Returns cached root status if available, otherwise performs check.
     */
    suspend fun isRootGranted(): Boolean {
        val cachedValue = cachedRootState
        if (cachedValue != null) {
            return cachedValue
        }

        return mutex.withLock {
            if (cachedRootState == null) {
                val result = withContext(Dispatchers.IO) {
                    RootUtils.canExecuteRootCommandRobust()
                }
                cachedRootState = result
            }
            cachedRootState!!
        }
    }

    /**
     * Returns Shizuku permission status. Performs a fast live check safely.
     */
    suspend fun isShizukuGranted(): Boolean {
        return try {
            val result = withContext(Dispatchers.IO) {
                ShizukuUtils.hasShizukuPermission()
            }
            cachedShizukuState = result
            result
        } catch (e: Throwable) {
            cachedShizukuState = false
            false
        }
    }

    /**
     * Returns true if either Root or Shizuku access is granted and enabled in preferences.
     */
    suspend fun isElevatedAccessGranted(context: Context? = null): Boolean = coroutineScope {
        val prefs = context?.let { RootAccessPreferences(it) }
        val rootEnabled = prefs?.getUseRootAccess() ?: true
        val shizukuEnabled = prefs?.getUseShizukuAccess() ?: true

        val shizukuActive = if (shizukuEnabled) isShizukuGranted() else false
        if (shizukuActive) return@coroutineScope true

        if (rootEnabled) {
            val rootDeferred = async(Dispatchers.IO) { isRootGranted() }
            rootDeferred.await()
        } else {
            false
        }
    }

    /**
     * Forces a re-check by clearing cache and performing new checks.
     */
    suspend fun forceRefresh(): Boolean = coroutineScope {
        mutex.withLock {
            cachedRootState = null
            cachedShizukuState = null
        }

        val shizukuActive = isShizukuGranted()
        val rootDeferred = async(Dispatchers.IO) {
            mutex.withLock {
                val result = withContext(Dispatchers.IO) {
                    RootUtils.canExecuteRootCommandRobust()
                }
                cachedRootState = result
                result
            }
        }

        shizukuActive || rootDeferred.await()
    }

    fun getCachedRootAccess(): Boolean? {
        return cachedRootState
    }

    fun getCachedShizukuAccess(): Boolean {
        return try {
            val live = ShizukuUtils.hasShizukuPermission()
            cachedShizukuState = live
            live
        } catch (e: Throwable) {
            cachedShizukuState = false
            false
        }
    }

    fun getCachedElevatedAccess(): Boolean? {
        return try {
            val s = ShizukuUtils.hasShizukuPermission()
            if (s) return true
            val r = cachedRootState
            r
        } catch (e: Throwable) {
            cachedRootState
        }
    }

    suspend fun hasRootAccess(context: Context? = null): Boolean {
        return isElevatedAccessGranted(context)
    }

    fun reset() {
        cachedRootState = null
        cachedShizukuState = null
    }
}
