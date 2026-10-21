package com.ivarna.finalbenchmark2.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

object ShizukuUtils {
    private const val TAG = "ShizukuUtils"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 8888

    private var newProcessMethod: Method? = null
    private var reflectionAttempted = false

    private fun getNewProcessMethod(): Method? {
        if (!reflectionAttempted) {
            reflectionAttempted = true
            try {
                newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply {
                    isAccessible = true
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to reflect Shizuku.newProcess method", e)
            }
        }
        return newProcessMethod
    }

    /**
     * Checks if Shizuku service is running and binder is alive.
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            Log.d(TAG, "Shizuku pingBinder failed: ${e.message}")
            false
        }
    }

    /**
     * Checks if Shizuku permission has been granted to the application.
     */
    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error checking Shizuku permission: ${e.message}")
            false
        }
    }

    /**
     * Requests Shizuku permission if service is running and permission not yet granted.
     */
    fun requestShizukuPermission(requestCode: Int = SHIZUKU_PERMISSION_REQUEST_CODE) {
        try {
            if (isShizukuAvailable()) {
                Log.d(TAG, "Requesting Shizuku permission with code $requestCode")
                Shizuku.requestPermission(requestCode)
            } else {
                Log.d(TAG, "Shizuku service not available")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting Shizuku permission: ${e.message}", e)
        }
    }

    fun addPermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            if (isShizukuAvailable()) {
                Shizuku.addRequestPermissionResultListener(listener)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error adding permission listener", e)
        }
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            if (isShizukuAvailable()) {
                Shizuku.removeRequestPermissionResultListener(listener)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error removing permission listener", e)
        }
    }

    fun addBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
        try {
            Shizuku.addBinderReceivedListenerSticky(listener)
        } catch (e: Throwable) {
            Log.e(TAG, "Error adding binder received listener", e)
        }
    }

    fun removeBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
        try {
            if (isShizukuAvailable()) {
                Shizuku.removeBinderReceivedListener(listener)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error removing binder received listener", e)
        }
    }

    fun addBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
        try {
            if (isShizukuAvailable()) {
                Shizuku.addBinderDeadListener(listener)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error adding binder dead listener", e)
        }
    }

    fun removeBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
        try {
            if (isShizukuAvailable()) {
                Shizuku.removeBinderDeadListener(listener)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error removing binder dead listener", e)
        }
    }

    /**
     * Executes a shell command via Shizuku.
     * @param command The shell command to execute
     * @return Standard output of the command if successful or non-empty, null otherwise.
     */
    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) {
            Log.d(TAG, "Cannot execute Shizuku command: permission not granted")
            return@withContext null
        }

        var process: Process? = null
        try {
            Log.d(TAG, "Executing Shizuku command: $command")
            val method = getNewProcessMethod()
            if (method == null) {
                Log.e(TAG, "newProcessMethod reflection is null")
                return@withContext null
            }

            process = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (output.isNotEmpty()) output.append("\n")
                output.append(line)
            }

            val exitCode = process.waitFor()
            val result = output.toString().trim()
            if (exitCode == 0 || result.isNotEmpty()) {
                Log.d(TAG, "Shizuku command succeeded (exit code $exitCode): $command")
                result
            } else {
                Log.e(TAG, "Shizuku command failed with exit code $exitCode: $command")
                null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception executing Shizuku command: $command", e)
            null
        } finally {
            try {
                process?.destroy()
            } catch (_: Throwable) {
            }
        }
    }
}
