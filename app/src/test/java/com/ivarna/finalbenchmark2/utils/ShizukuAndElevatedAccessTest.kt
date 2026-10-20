package com.ivarna.finalbenchmark2.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ShizukuAndElevatedAccessTest {

    @Test
    fun testShizukuUtilsAvailabilityInUnitTestEnvironment() {
        val isAvailable = ShizukuUtils.isShizukuAvailable()
        assertFalse(isAvailable)
    }

    @Test
    fun testShizukuPermissionInUnitTestEnvironment() {
        val hasPermission = ShizukuUtils.hasShizukuPermission()
        assertFalse(hasPermission)
    }

    @Test
    fun testRootCommandExecutorElevatedCheck() = runBlocking {
        val executor = RootCommandExecutor()
        val hasElevated = executor.hasElevatedAccess()
        assertFalse(hasElevated)
    }

    @Test
    fun testRootAccessManagerReset() = runBlocking {
        RootAccessManager.reset()
        assertNull(RootAccessManager.getCachedRootAccess())
        assertFalse(RootAccessManager.getCachedShizukuAccess())
        assertNull(RootAccessManager.getCachedElevatedAccess())
    }
}
