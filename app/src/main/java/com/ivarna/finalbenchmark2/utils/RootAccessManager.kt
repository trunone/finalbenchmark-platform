package com.ivarna.finalbenchmark2.utils

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
     * Returns Shizuku permission status. Performs a fast live check.
     */
    suspend fun isShizukuGranted(): Boolean {
        val result = withContext(Dispatchers.IO) {
            ShizukuUtils.hasShizukuPermission()
        }
        cachedShizukuState = result
        return result
    }

    /**
     * Returns true if either Root or Shizuku access is granted.
     * Checks Shizuku first and runs root check concurrently to prevent UI delays on non-rooted devices.
     */
    suspend fun isElevatedAccessGranted(): Boolean = coroutineScope {
        val shizukuActive = isShizukuGranted()
        if (shizukuActive) return@coroutineScope true

        val rootDeferred = async(Dispatchers.IO) { isRootGranted() }
        rootDeferred.await()
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
        val live = ShizukuUtils.hasShizukuPermission()
        cachedShizukuState = live
        return live
    }

    fun getCachedElevatedAccess(): Boolean? {
        val s = ShizukuUtils.hasShizukuPermission()
        if (s) return true
        val r = cachedRootState
        return r
    }

    /**
     * Returns true if elevated access (Root or Shizuku) is available.
     */
    suspend fun hasRootAccess(): Boolean {
        return isElevatedAccessGranted()
    }

    /**
     * Resets cached states
     */
    fun reset() {
        cachedRootState = null
        cachedShizukuState = null
    }
}
