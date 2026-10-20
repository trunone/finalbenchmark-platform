package com.ivarna.finalbenchmark2.utils

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes commands using Root (libsu) or Shizuku privileges.
 */
class RootCommandExecutor {
    companion object {
        private const val TAG = "RootCommandExecutor"

        init {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }

    /**
     * Executes a shell command with root or Shizuku privileges
     * @param command The command to execute
     * @return Output of the command or null if failed
     */
    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        // First attempt via root (libsu)
        try {
            if (hasRootAccess()) {
                Log.d(TAG, "Executing root command: $command")
                val result = Shell.cmd(command).exec()

                if (result.isSuccess) {
                    val output = result.out.joinToString("\n").trim()
                    Log.d(TAG, "Root command executed successfully: $command")
                    return@withContext output
                } else {
                    Log.d(TAG, "Root command failed: $command, stderr: ${result.err.joinToString("\n")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in root execution for command: $command", e)
        }

        // Fallback to Shizuku if root is not available or failed
        if (ShizukuUtils.hasShizukuPermission()) {
            Log.d(TAG, "Executing command via Shizuku: $command")
            val shizukuResult = ShizukuUtils.executeCommand(command)
            if (shizukuResult != null) {
                return@withContext shizukuResult
            }
        }

        null
    }

    /**
     * Reads content from a file using root or Shizuku privileges
     * @param path The path to the file
     * @return Content of the file or null if failed
     */
    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        executeCommand("cat $path")
    }

    /**
     * Checks if a file exists using root or Shizuku privileges
     * @param path The path to check
     * @return True if file exists, false otherwise
     */
    suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        val result = executeCommand("ls $path 2>/dev/null")
        result != null && !result.contains("No such file or directory", ignoreCase = true)
    }

    /**
     * Checks if the device has root access via libsu
     * @return True if root access is available, false otherwise
     */
    fun hasRootAccess(): Boolean {
        return try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            Log.e(TAG, "Error checking root access", e)
            false
        }
    }

    /**
     * Checks if elevated access (Root or Shizuku) is available
     */
    fun hasElevatedAccess(): Boolean {
        return hasRootAccess() || ShizukuUtils.hasShizukuPermission()
    }

    /**
     * Lists files in a directory using root or Shizuku privileges
     * @param path The directory path to list
     * @return List of files/directories in the path or null if failed
     */
    suspend fun listDirectory(path: String): List<String>? = withContext(Dispatchers.IO) {
        val output = executeCommand("ls -1 $path 2>/dev/null")
        output?.lines()?.filter { it.isNotBlank() }
    }

    /**
     * Finds files matching a pattern using root or Shizuku privileges
     * @param pattern The pattern to search for
     * @return List of matching file paths or null if failed
     */
    suspend fun findFiles(pattern: String): List<String>? = withContext(Dispatchers.IO) {
        val output = executeCommand("find $pattern 2>/dev/null")
        output?.lines()?.filter { it.isNotBlank() }
    }
}
