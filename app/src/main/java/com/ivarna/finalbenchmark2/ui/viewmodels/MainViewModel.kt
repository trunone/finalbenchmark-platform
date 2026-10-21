package com.ivarna.finalbenchmark2.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.utils.RootAccessManager
import com.ivarna.finalbenchmark2.utils.RootAccessPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Enum to represent different root and elevated access states
enum class RootStatus {
    NO_ROOT,           // Device is not rooted and Shizuku not active
    ROOT_AVAILABLE,    // Device is rooted but commands don't work
    ROOT_WORKING,      // Device is rooted and commands work
    SHIZUKU_WORKING    // Shizuku permission granted and working
}

// Enum to represent performance optimization states
enum class PerformanceOptimizationStatus {
    NOT_SUPPORTED,     // Device doesn't support the optimization
    DISABLED,          // Optimization is available but disabled
    ENABLED,           // Optimization is active
    READY              // Optimization is initialized but not yet acquired (for wake lock)
}

data class PerformanceOptimizations(
    val sustainedPerformanceMode: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val wakeLockStatus: PerformanceOptimizationStatus = PerformanceOptimizationStatus.ENABLED, // Ready state
    val screenAlwaysOnStatus: PerformanceOptimizationStatus = PerformanceOptimizationStatus.ENABLED,
    val highPriorityThreading: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val performanceHintApi: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val cpuAffinityControl: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val foregroundService: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED,
    val cpuGovernorHints: PerformanceOptimizationStatus = PerformanceOptimizationStatus.DISABLED
)

class MainViewModel : ViewModel() {
    
    private val _rootState = MutableStateFlow(RootStatus.NO_ROOT)
    val rootState: StateFlow<RootStatus> = _rootState.asStateFlow()
    
    private val _performanceOptimizations = MutableStateFlow(PerformanceOptimizations())
    val performanceOptimizations: StateFlow<PerformanceOptimizations> = _performanceOptimizations.asStateFlow()

    init {
        checkRootAccess()
    }

    fun checkRootAccess(context: Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootGranted = RootAccessManager.isRootGranted()
            val shizukuGranted = RootAccessManager.isShizukuGranted()

            val prefs = context?.let { RootAccessPreferences(it) }
            val useRoot = prefs?.getUseRootAccess() ?: true
            val useShizuku = prefs?.getUseShizukuAccess() ?: true

            val result = when {
                useRoot && rootGranted -> RootStatus.ROOT_WORKING
                useShizuku && shizukuGranted -> RootStatus.SHIZUKU_WORKING
                else -> RootStatus.NO_ROOT
            }

            Log.d("MainViewModel", "Elevated access result via RootAccessManager: $result")
            _rootState.value = result
        }
    }
    
    fun updateSustainedPerformanceModeStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            sustainedPerformanceMode = status
        )
    }
    
    fun updateWakeLockStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            wakeLockStatus = status
        )
    }
    
    fun updateScreenAlwaysOnStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            screenAlwaysOnStatus = status
        )
    }
    
    fun updateHighPriorityThreadingStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            highPriorityThreading = status
        )
    }
    
    fun updatePerformanceHintApiStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            performanceHintApi = status
        )
    }
    
    fun updateCpuAffinityControlStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            cpuAffinityControl = status
        )
    }
    
    fun updateForegroundServiceStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            foregroundService = status
        )
    }
    
    fun updateCpuGovernorHintsStatus(status: PerformanceOptimizationStatus) {
        _performanceOptimizations.value = _performanceOptimizations.value.copy(
            cpuGovernorHints = status
        )
    }
    
    fun acquireWakeLock() {
        // This will be handled by the MainActivity
    }
    
    fun releaseWakeLock() {
        // This will be handled by the MainActivity
    }
}
