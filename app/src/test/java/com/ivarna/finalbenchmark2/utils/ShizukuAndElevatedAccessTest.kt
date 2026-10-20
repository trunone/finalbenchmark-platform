package com.ivarna.finalbenchmark2.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ShizukuAndElevatedAccessTest {

    @Test
    fun testShizukuUtilsAvailabilityInUnitTestEnvironment() {
        // In JVM unit test environment, Shizuku pingBinder should safely return false without throwing an exception
        val isAvailable = ShizukuUtils.isShizukuAvailable()
        assertFalse(isAvailable)
    }

    @Test
    fun testShizukuPermissionInUnitTestEnvironment() {
        // Permission check should safely return false when Shizuku is not running
        val hasPermission = ShizukuUtils.hasShizukuPermission()
        assertFalse(hasPermission)
    }

    @Test
    fun testRootCommandExecutorElevatedCheck() = runBlocking {
        val executor = RootCommandExecutor()
        // Should execute without throwing unexpected exceptions
        val hasElevated = executor.hasElevatedAccess()
        // In non-rooted JVM environment, elevated access is false
        assertFalse(hasElevated)
    }

    @Test
    fun testRootAccessManagerReset() = runBlocking {
        RootAccessManager.reset()
        assertNull(RootAccessManager.getCachedRootAccess())
        assertNull(RootAccessManager.getCachedShizukuAccess())
        assertNull(RootAccessManager.getCachedElevatedAccess())
    }
}
