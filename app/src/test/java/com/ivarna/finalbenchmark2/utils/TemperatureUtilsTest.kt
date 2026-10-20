package com.ivarna.finalbenchmark2.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemperatureUtilsTest {

    @Test
    fun testParseTemperatureNormalCelsius() {
        assertEquals(35.5f, TemperatureUtils.parseTemperature(35.5f)!!, 0.01f)
    }

    @Test
    fun testParseTemperatureMillidegrees() {
        // 45000 millidegrees Celsius = 45.0 C
        assertEquals(45.0f, TemperatureUtils.parseTemperature(45000f)!!, 0.01f)
        // 120000 millidegrees Celsius = 120.0 C
        assertEquals(120.0f, TemperatureUtils.parseTemperature(120000f)!!, 0.01f)
    }

    @Test
    fun testParseTemperatureTenths() {
        // 365 tenths of a degree = 36.5 C
        assertEquals(36.5f, TemperatureUtils.parseTemperature(365f)!!, 0.01f)
    }

    @Test
    fun testParseTemperatureInvalidAndExtreme() {
        assertNull(TemperatureUtils.parseTemperature(-1f))
        assertNull(TemperatureUtils.parseTemperature(0f))
        assertNull(TemperatureUtils.parseTemperature(200000f))
    }

    @Test
    fun testGetCpuOrBatteryTemperatureCpuAvailable() {
        val testUtils = object : TemperatureUtils() {
            override fun getCpuTemperature(): Float = 55.0f
            override fun getBatteryTemperature(): Float = 32.0f
        }
        val result = testUtils.getCpuOrBatteryTemperature()
        assertEquals(55.0f, result.temperature, 0.01f)
        assertEquals(false, result.isBattery)
    }

    @Test
    fun testGetCpuOrBatteryTemperatureCpuUnavailableBatteryAvailable() {
        val testUtils = object : TemperatureUtils() {
            override fun getCpuTemperature(): Float = -1f
            override fun getBatteryTemperature(): Float = 34.0f
        }
        val result = testUtils.getCpuOrBatteryTemperature()
        assertEquals(34.0f, result.temperature, 0.01f)
        assertEquals(true, result.isBattery)
    }

    @Test
    fun testGetCpuOrBatteryTemperatureBothUnavailable() {
        val testUtils = object : TemperatureUtils() {
            override fun getCpuTemperature(): Float = -1f
            override fun getBatteryTemperature(): Float = -1f
        }
        val result = testUtils.getCpuOrBatteryTemperature()
        assertEquals(-1f, result.temperature, 0.01f)
        assertEquals(false, result.isBattery)
    }
}
