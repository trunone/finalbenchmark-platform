package com.ivarna.finalbenchmark2.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GpuMonitor {

    private val POSSIBLE_PATHS = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpuclk",             // Adreno (Standard)
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",   // Adreno (Alternative)
        "/sys/class/devfreq/gpufreq/cur_freq",         // Mali/Generic
        "/sys/kernel/gpu/gpu_clock",                   // Some older Kernels
        "/sys/devices/platform/galcore/gpu/gpu0/gpufreq/cur_freq" // Pixel/Tensor specific
    )

    private var cachedPath: String? = null

    /**
     * Finds the valid path for this specific device.
     * We cache it so we don't search every second.
     */
    suspend fun getValidPath(context: Context? = null): String? {
        if (cachedPath != null) return cachedPath

        val executor = RootCommandExecutor(context)
        return withContext(Dispatchers.IO) {
            for (path in POSSIBLE_PATHS) {
                if (executor.fileExists(path)) {
                    cachedPath = path
                    return@withContext path
                }
            }
            return@withContext null
        }
    }

    /**
     * Reads the current frequency using Root or Shizuku (cat command)
     */
    suspend fun getCurrentFrequency(context: Context? = null): String {
        val path = getValidPath(context) ?: return "GPU Path Not Found"

        val executor = RootCommandExecutor(context)
        return withContext(Dispatchers.IO) {
            val content = executor.readFile(path)
            if (content != null && content.isNotBlank()) {
                val rawFreq = content.lines().firstOrNull()?.trim() ?: ""
                formatFrequency(rawFreq)
            } else {
                "Error reading"
            }
        }
    }

    private fun formatFrequency(raw: String): String {
        return try {
            val hertz = raw.toLong()
            when {
                hertz > 10_000_000 -> "${hertz / 1_000_000} MHz"
                hertz > 10_000 -> "${hertz / 1_000} MHz"
                else -> "$hertz MHz"
            }
        } catch (e: Exception) {
            raw
        }
    }
}
