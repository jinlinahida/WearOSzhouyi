package com.boompala.ui.pulse

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.boompala.engine.pulse.PulseDiagnosisResult
import com.boompala.pulse.PulseDataSourceFactory
import com.boompala.pulse.PulseSensorState
import com.boompala.ui.AppHaptics
import com.boompala.ui.BoompalaButtonDefaults
import com.boompala.ui.BoompalaCardButton
import com.boompala.ui.LocalHapticFeedbackEnabled
import com.boompala.ui.LocalHapticIntensity
import com.boompala.ui.wearPressFeedback

/**
 * 把脉测量态界面。
 * 纯黑透明底色透出翡翠青漫反射顶光，C 位发光脉冲线横贯表盘中轴，由手腕真实脉搏物理驱动。
 * 具备离腕检测拦截、置信度与覆盖率质检重测、双通道授权。
 */
@Composable
fun PulseMeasureScreen(
    onResult: (PulseDiagnosisResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val hapticIntensity = LocalHapticIntensity.current

    // 检查是否有 BODY_SENSORS 权限
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BODY_SENSORS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    // 监听生命周期变化：当从系统授权弹窗或手表系统设置页返回（ON_RESUME）时无缝重新刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BODY_SENSORS,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var restartKey by remember { mutableStateOf(0) }
    val dataSource = remember(restartKey) {
        PulseDataSourceFactory.createDataSource(context)
    }
    val state by dataSource.state.collectAsState()

    DisposableEffect(dataSource, hasPermission, restartKey) {
        if (hasPermission) {
            dataSource.start(scope, durationSeconds = 20)
        }
        onDispose {
            dataSource.stop()
        }
    }

    // 监听测量完成状态
    LaunchedEffect(state) {
        val currentState = state
        if (currentState is PulseSensorState.Completed) {
            onResult(currentState.result)
        }
    }

    // 脉冲波峰触觉同步微震动
    LaunchedEffect(state) {
        val currentState = state
        if (currentState is PulseSensorState.Measuring && currentState.isPeakNow) {
            AppHaptics.pulseBeat(context, intensity = hapticIntensity, enabled = hapticEnabled)
        }
    }

    // 根 Box 保持透明背景，自然透出 TopSpotlightBackground 专属翡翠青 (0xFF00E5A3) 环境光
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasPermission) {
            // 权限请求引导页
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "把脉需要传感器权限",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "请授予身体传感器权限以读取微血管搏动节律",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))

                val requestPressInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null) {
                            ActivityCompat.requestPermissions(
                                activity,
                                arrayOf(Manifest.permission.BODY_SENSORS),
                                1001,
                            )
                        } else {
                            openAppSettings(context)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(
                            interactionSource = requestPressInteraction,
                            hapticEnabled = hapticEnabled,
                            intensity = hapticIntensity,
                        ),
                    interactionSource = requestPressInteraction,
                ) {
                    Text("授权并开始")
                }

                Spacer(modifier = Modifier.height(6.dp))

                val settingsPressInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = {
                        openAppSettings(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(
                            interactionSource = settingsPressInteraction,
                            hapticEnabled = hapticEnabled,
                            intensity = hapticIntensity,
                        ),
                    interactionSource = settingsPressInteraction,
                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                ) {
                    Text("前往手表设置开启")
                }
            }
        } else {
            // 正常测量、未佩戴与质检重测状态
            when (val currentState = state) {
                is PulseSensorState.Measuring -> {
                    // 1. 边缘极细进度弧 (0.5dp 弧度，暗琥珀微光)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 1.6.dp.toPx()
                        val inset = strokeW / 2
                        drawArc(
                            color = Color(0xFFE0A96D).copy(alpha = 0.75f),
                            startAngle = -90f,
                            sweepAngle = currentState.progress * 360f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                    }

                    // 2. 核心 C 位发光脉冲线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PulseWaveCanvas(points = currentState.wavePoints)
                    }

                    // 3. 底部倒计时提示
                    val remainingSeconds = ((1f - currentState.progress) * 20).toInt().coerceAtLeast(1)
                    Text(
                        text = "平心静气 · ${remainingSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                    )
                }

                is PulseSensorState.NotWorn -> {
                    // 未佩戴或脱离手腕拦截提示（含重试与返回操作闭环）
                    val scrollState = rememberScrollState()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                    ) {
                        Text(
                            text = "未贴紧手腕",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE0A96D),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "请将手表贴紧手腕内侧动脉处\n感知脉搏后自动开始",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val retryNotWornPress = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = {
                                restartKey++
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(
                                    interactionSource = retryNotWornPress,
                                    hapticEnabled = hapticEnabled,
                                    intensity = hapticIntensity,
                                ),
                            interactionSource = retryNotWornPress,
                        ) {
                            Text("重新把脉")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val backPress = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(
                                    interactionSource = backPress,
                                    hapticEnabled = hapticEnabled,
                                    intensity = hapticIntensity,
                                ),
                            interactionSource = backPress,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text("返回")
                        }
                    }
                }

                is PulseSensorState.Preparing, PulseSensorState.Idle -> {
                    // 寻脉准备状态
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = "静心 · 感应脉搏中",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF00E5A3),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请平置手腕，安神定气",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                is PulseSensorState.Analyzing -> {
                    // 辨识脉象中
                    Text(
                        text = "正在辨析脉象...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFE0A96D),
                    )
                }

                is PulseSensorState.QualityFailed -> {
                    // 数据覆盖率与置信度不足要求重测
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "气脉微弱或手腕晃动",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFF6B6B),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${currentState.reason}\n请保持手腕平放静止",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        val retryPressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = {
                                restartKey++
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(
                                    interactionSource = retryPressInteraction,
                                    hapticEnabled = hapticEnabled,
                                    intensity = hapticIntensity,
                                ),
                            interactionSource = retryPressInteraction,
                        ) {
                            Text("重新把脉")
                        }
                    }
                }

                is PulseSensorState.Error -> {
                    val isTimeout = currentState.reason == "wear_timeout"
                    val title = if (isTimeout) "感应超时" else "传感器未就绪"
                    val desc = if (isTimeout) {
                        "未检测到手腕脉搏，请佩戴紧实后重试"
                    } else {
                        "未能启动手表传感器，请重试"
                    }

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val errorRetryPress = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = {
                                restartKey++
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(
                                    interactionSource = errorRetryPress,
                                    hapticEnabled = hapticEnabled,
                                    intensity = hapticIntensity,
                                ),
                            interactionSource = errorRetryPress,
                        ) {
                            Text("重新把脉")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val errorBackPress = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(
                                    interactionSource = errorBackPress,
                                    hapticEnabled = hapticEnabled,
                                    intensity = hapticIntensity,
                                ),
                            interactionSource = errorBackPress,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text("返回")
                        }
                    }
                }

                is PulseSensorState.Completed -> {
                    // Completed 由 LaunchedEffect 处理导航跳转
                }
            }
        }
    }
}

/**
 * 唤起手表系统应用详情设置页，供用户手动开启身体传感器权限。
 */
private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
