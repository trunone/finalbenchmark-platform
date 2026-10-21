package com.ivarna.finalbenchmark2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.finalbenchmark2.ui.components.GpuDataPoint
import com.ivarna.finalbenchmark2.utils.GpuFrequencyReader
import com.ivarna.finalbenchmark2.utils.GpuFrequencyFallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.min

class GpuInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val gpuFrequencyReader = GpuFrequencyReader()
    private val gpuFrequencyFallback = GpuFrequencyFallback()
    
    private val _gpuFrequencyState = MutableStateFlow<GpuFrequencyReader.GpuFrequencyState>(
        GpuFrequencyReader.GpuFrequencyState.NotSupported
    )
    val gpuFrequencyState: StateFlow<GpuFrequencyReader.GpuFrequencyState> = _gpuFrequencyState
    
    // GPU utilization data points for the graph
    private val _gpuHistory = MutableStateFlow<List<GpuDataPoint>>(emptyList())
    val gpuHistory: StateFlow<List<GpuDataPoint>> = _gpuHistory
    
    init {
        refreshGpuFrequency()
        startGpuMonitoring()
    }
    
    fun refreshGpuFrequency() {
        viewModelScope.launch {
            val primaryResult = gpuFrequencyReader.readGpuFrequency(getApplication())
            
            val result = when (primaryResult) {
                is GpuFrequencyReader.GpuFrequencyState.RequiresRoot -> {
                    gpuFrequencyFallback.readGpuFrequencyWithoutRoot() ?: primaryResult
                }
                is GpuFrequencyReader.GpuFrequencyState.Error -> {
                    gpuFrequencyFallback.readGpuFrequencyWithoutRoot() ?: primaryResult
                }
                else -> {
                    primaryResult
                }
            }
            
            _gpuFrequencyState.value = result
        }
    }
    
    private fun startGpuMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    val result = gpuFrequencyReader.readGpuFrequency(getApplication())
                    if (result is GpuFrequencyReader.GpuFrequencyState.Available) {
                        val gpuData = result.data
                        val currentFreq = gpuData.currentFrequencyMhz
                        val maxFreq = gpuData.maxFrequencyMhz
                        
                        val utilization = if (maxFreq != null && maxFreq > 0) {
                            min(100f, (currentFreq.toFloat() / maxFreq.toFloat()) * 100f)
                        } else {
                            val availableFreqs = gpuData.availableFrequencies
                            if (availableFreqs != null && availableFreqs.isNotEmpty()) {
                                val maxAvailableFreq = availableFreqs.maxOrNull()
                                if (maxAvailableFreq != null && maxAvailableFreq > 0) {
                                    min(100f, (currentFreq.toFloat() / maxAvailableFreq.toFloat()) * 100f)
                                } else {
                                    if (currentFreq > 0) {
                                        val typicalMaxFreq = 1000L
                                        min(100f, (currentFreq.toFloat() / typicalMaxFreq.toFloat()) * 100f)
                                    } else {
                                        0f
                                    }
                                }
                            } else {
                                if (currentFreq > 0) {
                                    val typicalMaxFreq = 1000L
                                    min(100f, (currentFreq.toFloat() / typicalMaxFreq.toFloat()) * 100f)
                                } else {
                                    0f
                                }
                            }
                        }
                        
                        val newPoint = GpuDataPoint(
                            timestamp = System.currentTimeMillis(),
                            utilization = utilization
                        )
                        
                        val updatedHistory = (_gpuHistory.value + newPoint)
                            .filter { it.timestamp > System.currentTimeMillis() - 30_000L }
                        
                        _gpuHistory.value = updatedHistory
                    }
                    
                    _gpuFrequencyState.value = result
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                delay(1000)
            }
        }
    }
}
