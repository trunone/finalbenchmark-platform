package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GPU Frequency Reader for Android
 */
class GpuFrequencyReader {
    companion object {
        private const val TAG = "GpuFrequencyReader"
    }
    
    private val vendorDetector = GpuVendorDetector()
    
    // Stable cached resolution
    @Volatile private var cachedCurrentPath: String? = null
    @Volatile private var cachedMaxPath: String? = null
    @Volatile private var cachedMinPath: String? = null
    @Volatile private var cachedGovPath: String? = null

    @Volatile private var cachedVendor: GpuVendorDetector.GpuVendor? = null
    @Volatile private var cachedMaxFreq: Long? = null
    @Volatile private var cachedMinFreq: Long? = null
    @Volatile private var cachedAvailableFreqs: List<Long>? = null
    @Volatile private var cachedGovernor: String? = null

    /**
     * Data class to hold GPU frequency information
     */
    data class GpuFrequencyData(
        val currentFrequencyMhz: Long,
        val maxFrequencyMhz: Long?,
        val minFrequencyMhz: Long?,
        val availableFrequencies: List<Long>?,
        val governor: String?,
        val utilizationPercent: Int?,
        val temperatureCelsius: Int?,
        val vendor: GpuVendorDetector.GpuVendor,
        val sourcePath: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Sealed class for representing different GPU frequency states
     */
    sealed class GpuFrequencyState {
        data class Available(val data: GpuFrequencyData) : GpuFrequencyState()
        object RequiresRoot : GpuFrequencyState()
        object NotSupported : GpuFrequencyState()
        data class Error(val message: String) : GpuFrequencyState()
    }
    
    /**
     * Reads GPU frequency data
     */
    suspend fun readGpuFrequency(context: Context? = null): GpuFrequencyState = withContext(Dispatchers.IO) {
        val rootExecutor = RootCommandExecutor(context)
        try {
            // Step 1: Check if root or Shizuku elevated access is available and enabled
            if (!RootAccessManager.isElevatedAccessGranted(context)) {
                Log.w(TAG, "Elevated access (Root/Shizuku) not available or disabled, trying fallback methods")
                
                val fallbackReader = GpuFrequencyFallback()
                val fallbackResult = fallbackReader.readGpuFrequencyWithoutRoot()
                if (fallbackResult != null) {
                    return@withContext fallbackResult
                }
                return@withContext GpuFrequencyState.RequiresRoot
            }

            // Fast Path: If we already have a known working path, read it directly
            val activePath = cachedCurrentPath
            if (activePath != null) {
                val content = rootExecutor.readFile(activePath)
                if (content != null) {
                    val freq = parseFrequency(content, cachedVendor ?: GpuVendorDetector.GpuVendor.UNKNOWN)
                    if (freq > 0) {
                        val vendor = cachedVendor ?: GpuVendorDetector.GpuVendor.UNKNOWN
                        val data = GpuFrequencyData(
                            currentFrequencyMhz = freq,
                            maxFrequencyMhz = cachedMaxFreq ?: readMaxFrequency(vendor, rootExecutor),
                            minFrequencyMhz = cachedMinFreq ?: readMinFrequency(vendor, rootExecutor),
                            availableFrequencies = cachedAvailableFreqs ?: readAvailableFrequencies(vendor, rootExecutor),
                            governor = cachedGovernor ?: readGovernor(vendor, rootExecutor),
                            utilizationPercent = null,
                            temperatureCelsius = null,
                            vendor = vendor,
                            sourcePath = activePath
                        )
                        return@withContext GpuFrequencyState.Available(data)
                    }
                }
                // Known path failed, invalidate and rediscover
                cachedCurrentPath = null
            }
            
            // Step 2: Vendor detection
            var vendor = cachedVendor
            if (vendor == null || vendor == GpuVendorDetector.GpuVendor.UNKNOWN) {
                vendor = vendorDetector.detectVendor()
                cachedVendor = vendor
            }
            
            // Step 3: Find current frequency path from vendor paths and common paths
            val candidatePaths = mutableListOf<String>()
            candidatePaths.addAll(GpuPaths.getPathsForVendor(vendor, "current_frequency"))
            candidatePaths.addAll(listOf(
                "/sys/class/kgsl/kgsl-3d0/gpuclk",
                "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
                "/sys/class/devfreq/gpufreq/cur_freq",
                "/sys/kernel/gpu/gpu_clock",
                "/sys/devices/platform/galcore/gpu/gpu0/gpufreq/cur_freq"
            ))
            
            for (path in candidatePaths.distinct()) {
                try {
                    val resolvedPaths = if (path.contains('*')) {
                        GpuPaths.resolveWildcardPath(path, rootExecutor)
                    } else {
                        listOf(path)
                    }
                    
                    for (resolvedPath in resolvedPaths) {
                        if (rootExecutor.fileExists(resolvedPath)) {
                            val content = rootExecutor.readFile(resolvedPath)
                            if (content != null) {
                                val frequency = parseFrequency(content, vendor)
                                if (frequency > 0) {
                                    cachedCurrentPath = resolvedPath

                                    val maxFreq = readMaxFrequency(vendor, rootExecutor)
                                    val minFreq = readMinFrequency(vendor, rootExecutor)
                                    val availableFreqs = readAvailableFrequencies(vendor, rootExecutor)
                                    val governor = readGovernor(vendor, rootExecutor)
                                    
                                    val data = GpuFrequencyData(
                                        currentFrequencyMhz = frequency,
                                        maxFrequencyMhz = maxFreq,
                                        minFrequencyMhz = minFreq,
                                        availableFrequencies = availableFreqs,
                                        governor = governor,
                                        utilizationPercent = null,
                                        temperatureCelsius = null,
                                        vendor = vendor,
                                        sourcePath = resolvedPath
                                    )
                                    
                                    return@withContext GpuFrequencyState.Available(data)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking GPU path: $path", e)
                }
            }

            // Fallback check
            val fallbackReader = GpuFrequencyFallback()
            val fallbackResult = fallbackReader.readGpuFrequencyWithoutRoot()
            if (fallbackResult != null) {
                return@withContext fallbackResult
            }
            
            GpuFrequencyState.Error("Failed to read GPU frequency from any available path")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error reading GPU frequency", e)
            GpuFrequencyState.Error("Unexpected error: ${e.message}")
        }
    }
    
    /**
     * Reads maximum frequency
     */
    private suspend fun readMaxFrequency(vendor: GpuVendorDetector.GpuVendor, rootExecutor: RootCommandExecutor): Long? {
        cachedMaxFreq?.let { return it }
        val maxFreqPaths = GpuPaths.getPathsForVendor(vendor, "max_frequency")
        
        for (path in maxFreqPaths) {
            try {
                val resolvedPaths = if (path.contains('*')) {
                    GpuPaths.resolveWildcardPath(path, rootExecutor)
                } else {
                    listOf(path)
                }
                
                for (resolvedPath in resolvedPaths) {
                    if (rootExecutor.fileExists(resolvedPath)) {
                        val content = rootExecutor.readFile(resolvedPath)
                        if (content != null) {
                            val frequency = parseFrequency(content, vendor)
                            if (frequency > 0) {
                                cachedMaxPath = resolvedPath
                                cachedMaxFreq = frequency
                                return frequency
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    /**
     * Reads minimum frequency
     */
    private suspend fun readMinFrequency(vendor: GpuVendorDetector.GpuVendor, rootExecutor: RootCommandExecutor): Long? {
        cachedMinFreq?.let { return it }
        val minFreqPaths = GpuPaths.getPathsForVendor(vendor, "min_frequency")
        
        for (path in minFreqPaths) {
            try {
                val resolvedPaths = if (path.contains('*')) {
                    GpuPaths.resolveWildcardPath(path, rootExecutor)
                } else {
                    listOf(path)
                }
                
                for (resolvedPath in resolvedPaths) {
                    if (rootExecutor.fileExists(resolvedPath)) {
                        val content = rootExecutor.readFile(resolvedPath)
                        if (content != null) {
                            val frequency = parseFrequency(content, vendor)
                            if (frequency > 0) {
                                cachedMinPath = resolvedPath
                                cachedMinFreq = frequency
                                return frequency
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    /**
     * Reads available frequencies
     */
    private suspend fun readAvailableFrequencies(vendor: GpuVendorDetector.GpuVendor, rootExecutor: RootCommandExecutor): List<Long>? {
        cachedAvailableFreqs?.let { return it }
        val availableFreqPaths = GpuPaths.getPathsForVendor(vendor, "available_frequencies")
        
        for (path in availableFreqPaths) {
            try {
                val resolvedPaths = if (path.contains('*')) {
                    GpuPaths.resolveWildcardPath(path, rootExecutor)
                } else {
                    listOf(path)
                }
                
                for (resolvedPath in resolvedPaths) {
                    if (rootExecutor.fileExists(resolvedPath)) {
                        val content = rootExecutor.readFile(resolvedPath)
                        if (content != null) {
                            val frequencies = parseAvailableFrequencies(content, vendor)
                            if (frequencies.isNotEmpty()) {
                                cachedAvailableFreqs = frequencies
                                return frequencies
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    /**
     * Reads governor
     */
    private suspend fun readGovernor(vendor: GpuVendorDetector.GpuVendor, rootExecutor: RootCommandExecutor): String? {
        cachedGovernor?.let { return it }
        val governorPaths = GpuPaths.getPathsForVendor(vendor, "gpu_info").filter { it.contains("governor") }
        
        for (path in governorPaths) {
            try {
                if (rootExecutor.fileExists(path)) {
                    val content = rootExecutor.readFile(path)
                    if (content != null && content.isNotEmpty()) {
                        val gov = content.trim()
                        cachedGovPath = path
                        cachedGovernor = gov
                        return gov
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    fun parseFrequency(rawValue: String, vendor: GpuVendorDetector.GpuVendor): Long {
        val cleanValue = rawValue.trim()
        
        return when {
            cleanValue.toLongOrNull() != null -> {
                val hz = cleanValue.toLong()
                when {
                    hz > 10_000_000 -> hz / 1_000_000  // Hz → MHz
                    hz > 10_000 -> hz / 1_000           // KHz → MHz
                    else -> hz                          // Already MHz
                }
            }
            
            cleanValue.contains("MHz", ignoreCase = true) -> {
                cleanValue.filter { it.isDigit() }.toLongOrNull() ?: 0
            }
            
            cleanValue.contains("KHz", ignoreCase = true) -> {
                val khz = cleanValue.filter { it.isDigit() }.toLongOrNull() ?: 0
                khz / 1000
            }
            
            else -> 0
        }
    }
    
    fun parseAvailableFrequencies(rawValue: String, vendor: GpuVendorDetector.GpuVendor): List<Long> {
        val cleanValue = rawValue.trim()
        val values = cleanValue.split("\\s+".toRegex()).flatMap { it.split(",") }
        
        val frequencies = mutableListOf<Long>()
        for (value in values) {
            val v = value.trim()
            if (v.isNotEmpty()) {
                val frequency = parseFrequency(v, vendor)
                if (frequency > 0) {
                    frequencies.add(frequency)
                }
            }
        }
        
        return frequencies
    }

    fun resetCache() {
        cachedCurrentPath = null
        cachedMaxPath = null
        cachedMinPath = null
        cachedGovPath = null
        cachedVendor = null
        cachedMaxFreq = null
        cachedMinFreq = null
        cachedAvailableFreqs = null
        cachedGovernor = null
    }
}
