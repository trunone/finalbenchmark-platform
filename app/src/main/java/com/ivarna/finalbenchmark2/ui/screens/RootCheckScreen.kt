package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import rikka.shizuku.Shizuku
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivarna.finalbenchmark2.ui.components.GlassCard
import com.ivarna.finalbenchmark2.utils.OnboardingPreferences
import com.ivarna.finalbenchmark2.utils.RootAccessManager
import com.ivarna.finalbenchmark2.utils.ShizukuUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface RootUiState {
    object Checking : RootUiState
    data class Granted(val isShizuku: Boolean = false) : RootUiState
    data class ShizukuAvailable(val permissionGranted: Boolean = false) : RootUiState
    object NotAvailable : RootUiState
}

@Composable
fun RootCheckScreen(
    onNextClicked: () -> Unit,
    onBackClicked: () -> Unit = {},
    onRetryRootAccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var rootUiState by remember { mutableStateOf<RootUiState>(RootUiState.Checking) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }

    val hasRootChecked = remember { mutableStateOf(false) }

    fun performAccessCheck() {
        scope.launch {
            rootUiState = RootUiState.Checking
            val hasShizuku = RootAccessManager.isShizukuGranted()
            val isShizukuAvailable = ShizukuUtils.isShizukuAvailable()
            if (hasShizuku) {
                rootUiState = RootUiState.Granted(isShizuku = true)
                hasRootChecked.value = true
                return@launch
            }

            val hasRoot = RootAccessManager.isRootGranted()
            rootUiState = when {
                hasRoot -> RootUiState.Granted(isShizuku = false)
                isShizukuAvailable -> RootUiState.ShizukuAvailable(permissionGranted = false)
                else -> RootUiState.NotAvailable
            }
            hasRootChecked.value = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        if (!hasRootChecked.value) {
            performAccessCheck()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                performAccessCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                performAccessCheck()
            }
        }
        ShizukuUtils.addPermissionListener(listener)
        onDispose {
            ShizukuUtils.removePermissionListener(listener)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Central content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                val iconColor = when (rootUiState) {
                    is RootUiState.Checking -> MaterialTheme.colorScheme.onSurfaceVariant
                    is RootUiState.Granted -> MaterialTheme.colorScheme.primary
                    is RootUiState.ShizukuAvailable -> MaterialTheme.colorScheme.secondary
                    is RootUiState.NotAvailable -> MaterialTheme.colorScheme.error
                }

                GlassCard(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(50.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = "Elevated Access Status",
                            modifier = Modifier.size(48.dp),
                            tint = iconColor
                        )
                    }
                }

                if (rootUiState is RootUiState.Checking) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                val headlineText = when (val state = rootUiState) {
                    is RootUiState.Checking -> "Checking Elevated Access..."
                    is RootUiState.Granted -> if (state.isShizuku) "Shizuku Access Granted" else "Root Access Granted"
                    is RootUiState.ShizukuAvailable -> "Shizuku Service Detected"
                    is RootUiState.NotAvailable -> "Elevated Access Not Detected"
                }

                val headlineColor = when (rootUiState) {
                    is RootUiState.Checking -> MaterialTheme.colorScheme.onSurface
                    is RootUiState.Granted -> MaterialTheme.colorScheme.primary
                    is RootUiState.ShizukuAvailable -> MaterialTheme.colorScheme.secondary
                    is RootUiState.NotAvailable -> MaterialTheme.colorScheme.error
                }

                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = headlineColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        val statusText = when (val state = rootUiState) {
                            is RootUiState.Checking ->
                                "Checking your device for Root or Shizuku access..."
                            is RootUiState.Granted ->
                                if (state.isShizuku)
                                    "Shizuku Control Enabled. You have access to advanced features like GPU Frequency monitoring and Governor tuning via ADB permissions."
                                else
                                    "Full Control Enabled. You have access to advanced features like CPU Affinity, GPU Frequency monitoring, and Governor tuning."
                            is RootUiState.ShizukuAvailable ->
                                "Shizuku service is running on your device, but permission has not been granted yet. Tap below to request permission."
                            is RootUiState.NotAvailable ->
                                "Running in Standard Mode. Benchmarks will function correctly, but detailed hardware stats (GPU Load, Exact Frequencies) and optimizations will be disabled."
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                if (rootUiState is RootUiState.Granted) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                        borderColor = Color(0xFFFF9800).copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "⚠️ Elevated Access Warning",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Running benchmarks with elevated access will push your device to extreme limits. Your device may run significantly hotter than normal.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "⚠️ We are NOT responsible for any damage that may occur to your device during or after benchmarking.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFFF6B00),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                if (rootUiState is RootUiState.ShizukuAvailable) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            ShizukuUtils.requestShizukuPermission()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = "Grant Shizuku Permission",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (rootUiState is RootUiState.NotAvailable || rootUiState is RootUiState.ShizukuAvailable) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                delay(300)
                                RootAccessManager.forceRefresh()
                                performAccessCheck()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Retry Access Check",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Action Button
            Button(
                onClick = { onNextClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Next",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
