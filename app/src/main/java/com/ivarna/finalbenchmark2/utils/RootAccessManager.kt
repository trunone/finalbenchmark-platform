package com.ivarna.finalbenchmark2.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Singleton class to manage root and Shizuku access state across the application
 * This prevents repeated checks and improves performance by caching results
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
     * Returns cached Shizuku status if available, otherwise performs check.
     */
    suspend fun isShizukuGranted(): Boolean {
        val cachedValue = cachedShizukuState
        if (cachedValue != null) {
            return cachedValue
        }

        return mutex.withLock {
            if (cachedShizukuState == null) {
                val result = withContext(Dispatchers.IO) {
                    ShizukuUtils.hasShizukuPermission()
                }
                cachedShizukuState = result
            }
            cachedShizukuState!!
        }
    }

    /**
     * Returns true if either Root or Shizuku access is granted.
     */
    suspend fun isElevatedAccessGranted(): Boolean {
        return isRootGranted() || isShizukuGranted()
    }

    /**
     * Forces a re-check by clearing cache and performing new checks.
     */
    suspend fun forceRefresh(): Boolean {
        return mutex.withLock {
            cachedRootState = null
            cachedShizukuState = null

            val rootResult = withContext(Dispatchers.IO) {
                RootUtils.canExecuteRootCommandRobust()
            }
            val shizukuResult = withContext(Dispatchers.IO) {
                ShizukuUtils.hasShizukuPermission()
            }

            cachedRootState = rootResult
            cachedShizukuState = shizukuResult

            rootResult || shizukuResult
        }
    }

    fun getCachedRootAccess(): Boolean? {
        return cachedRootState
    }

    fun getCachedShizukuAccess(): Boolean? {
        return cachedShizukuState
    }

    fun getCachedElevatedAccess(): Boolean? {
        val r = cachedRootState
        val s = cachedShizukuState
        if (r == null && s == null) return null
        return (r == true) || (s == true)
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
