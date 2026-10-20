package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.HardwarePropertiesManager
import java.io.File

data class TemperatureResult(
    val temperature: Float,
    val isBattery: Boolean
)

open class TemperatureUtils(private val context: Context? = null) {

    fun getCpuOrBatteryTemperature(): TemperatureResult {
        val cpuTemp = getCpuTemperature()
        if (cpuTemp > 0f) {
            return TemperatureResult(cpuTemp, isBattery = false)
        }
        val batteryTemp = getBatteryTemperature()
        if (batteryTemp > 0f) {
            return TemperatureResult(batteryTemp, isBattery = true)
        }
        return TemperatureResult(-1f, isBattery = false)
    }

    open fun getCpuTemperature(): Float {
        // 1. Try reading from sysfs thermal zones
        val sysfsTemp = readCpuTempFromSysfs()
        if (sysfsTemp > 0f) {
            return sysfsTemp
        }

        // 2. Try HardwarePropertiesManager API (Android 7.0+)
        val hpmTemp = readCpuTempFromHardwareProperties()
        if (hpmTemp > 0f) {
            return hpmTemp
        }

        return -1f
    }

    open fun getBatteryTemperature(): Float {
        // 1. Primary method: BatteryManager broadcast intent
        try {
            val intent = context?.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (temperature > 0) {
                val celsius = temperature / 10f
                if (celsius in 5f..100f) {
                    return celsius
                }
            }
        } catch (e: Exception) {
            // Ignore broadcast failure
        }

        // 2. Fallback: Read directly from sysfs power supply files
        val batterySysfsPaths = listOf(
            "/sys/class/power_supply/battery/temp",
            "/sys/class/power_supply/battery/batt_temp",
            "/sys/class/power_supply/bms/temp",
            "/sys/class/power_supply/battery/subsystem/battery/temp"
        )

        for (path in batterySysfsPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val raw = file.readText().trim().toFloatOrNull() ?: continue
                    val celsius = parseTemperature(raw)
                    if (celsius != null && celsius in 5f..100f) {
                        return celsius
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }

        return -1f
    }

    private fun readCpuTempFromSysfs(): Float {
        val cpuKeywords = listOf(
            "cpu", "soc", "tsens", "exynos", "ap", "processor", "cluster", "kpss", "quiet"
        )

        val validCpuTemps = mutableListOf<Float>()
        val fallbackTemps = mutableListOf<Float>()

        // Check thermal zones 0 to 150 dynamically
        val thermalDir = File("/sys/class/thermal")
        val zoneDirs = try {
            if (thermalDir.exists() && thermalDir.isDirectory) {
                thermalDir.listFiles { file -> file.name.startsWith("thermal_zone") }?.toList() ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val dirsToScan = if (zoneDirs.isNotEmpty()) {
            zoneDirs
        } else {
            (0..150).map { File("/sys/class/thermal/thermal_zone$it") }
        }

        for (zoneDir in dirsToScan) {
            val tempFile = File(zoneDir, "temp")
            if (!tempFile.exists()) continue

            try {
                val rawStr = tempFile.readText().trim()
                val raw = rawStr.toFloatOrNull() ?: continue
                val tempC = parseTemperature(raw) ?: continue

                if (tempC !in 10f..120f) continue

                // Check type if present
                val typeFile = File(zoneDir, "type")
                val typeStr = if (typeFile.exists()) {
                    try { typeFile.readText().trim().lowercase() } catch (e: Exception) { "" }
                } else ""

                if (typeStr.isNotEmpty() && cpuKeywords.any { typeStr.contains(it) }) {
                    validCpuTemps.add(tempC)
                } else {
                    fallbackTemps.add(tempC)
                }
            } catch (e: Exception) {
                continue
            }
        }

        if (validCpuTemps.isNotEmpty()) {
            return validCpuTemps.maxOrNull() ?: validCpuTemps.first()
        }

        if (fallbackTemps.isNotEmpty()) {
            return fallbackTemps.maxOrNull() ?: fallbackTemps.first()
        }

        return -1f
    }

    private fun readCpuTempFromHardwareProperties(): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val hpm = context?.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as? HardwarePropertiesManager
                if (hpm != null) {
                    val temps = hpm.getDeviceTemperatures(
                        HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                        HardwarePropertiesManager.TEMPERATURE_CURRENT
                    )
                    if (temps.isNotEmpty()) {
                        val validTemps = temps.filter { it in 10f..120f }
                        if (validTemps.isNotEmpty()) {
                            return validTemps.maxOrNull() ?: validTemps.first()
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore SecurityException or unsupported operation
            }
        }
        return -1f
    }

    companion object {
        fun parseTemperature(raw: Float): Float? {
            if (raw <= 0f) return null
            val celsius = when {
                raw > 100000f -> raw / 1000f
                raw > 1000f -> raw / 1000f
                raw > 100f -> raw / 10f
                else -> raw
            }
            return if (celsius in 5f..120f) celsius else null
        }
    }
}
