package com.boompala.ui

import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.boompala.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// =========================================================================
// Re-WearBili Card & clickVfx
// =========================================================================

val CardBorderColor = Color(54, 54, 54, 255)
val CardBorderWidth = 0.4f.dp
val CardBackgroundColor = Color(38, 38, 38, 77)

@Composable
fun Modifier.clickVfx(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isEnabled: Boolean = true,
    animationsEnabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    if (isEnabled) {
        if (!animationsEnabled) {
            clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
        } else {
            val isPressed by interactionSource.collectIsPressedAsState()
            val sizePercent by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "clickVfx",
            )
            scale(sizePercent).clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
        }
    } else {
        Modifier
    }
}

@Composable
fun ReWearBiliCard(
    modifier: Modifier = Modifier,
    isClickEnabled: Boolean = true,
    animationsEnabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: (() -> Unit)? = null,
    borderColor: Color = CardBorderColor,
    backgroundColor: Color = CardBackgroundColor,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickVfx(
                isEnabled = isClickEnabled && onClick != null,
                animationsEnabled = animationsEnabled,
                onClick = { onClick?.invoke() },
            )
            .clip(shape)
            .border(
                width = CardBorderWidth,
                shape = shape,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor,
                        Color.Transparent,
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .background(color = backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth(),
    ) {
        content()
    }
}

// =========================================================================
// Dynamic Silk Video Background (Continuous Fluid Playback on SurfaceView)
// =========================================================================

@Composable
fun VideoSilkBackground(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mediaPlayer = remember {
        MediaPlayer().apply {
            isLooping = true
            setVolume(0f, 0f)
        }
    }
    var isPrepared by remember { mutableStateOf(false) }
    var currentSurfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val videoFile = File(context.filesDir, "video_silk_background.mp4")
                if (!videoFile.exists() || videoFile.length() == 0L) {
                    context.resources.openRawResource(R.raw.video_silk_background).use { input ->
                        videoFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                mediaPlayer.setDataSource(videoFile.absolutePath)
                mediaPlayer.setOnPreparedListener {
                    isPrepared = true
                    currentSurfaceHolder?.let { holder ->
                        if (holder.surface.isValid) {
                            try {
                                mediaPlayer.setDisplay(holder)
                                mediaPlayer.start()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
                mediaPlayer.prepareAsync()
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (_: Exception) {
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_silk_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            currentSurfaceHolder = holder
                            try {
                                mediaPlayer.setDisplay(holder)
                                if (isPrepared && !mediaPlayer.isPlaying) {
                                    mediaPlayer.start()
                                }
                            } catch (_: Exception) {
                            }
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            currentSurfaceHolder = holder
                            try {
                                mediaPlayer.setDisplay(holder)
                                if (isPrepared && !mediaPlayer.isPlaying) {
                                    mediaPlayer.start()
                                }
                            } catch (_: Exception) {
                            }
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            currentSurfaceHolder = null
                            try {
                                mediaPlayer.setDisplay(null)
                            } catch (_: Exception) {
                            }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// =========================================================================
// Boompala Brand Gradient Text (Soft Pastel Light Sky Blue + Silver White)
// =========================================================================

@Composable
fun BoompalaBrandText(modifier: Modifier = Modifier, fontSize: TextUnit = 26.sp) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFB3E5FC), // Light Sky Blue (100)
                            Color(0xFF4FC3F7), // Clear Pastel Sky Blue (300)
                        ),
                    ),
                ),
            ) {
                append("boom")
            }
            withStyle(
                SpanStyle(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFE0E0E0),
                            Color(0xFFFFFFFF),
                        ),
                    ),
                ),
            ) {
                append("pala")
            }
        },
        modifier = modifier,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
    )
}

// =========================================================================
// Welcome Screen Composable (Step 0: StartScreen, Step 1: DisclaimerScreen)
// =========================================================================

@Composable
fun WelcomeScreen(
    rotaryScrollingEnabled: Boolean,
    animationsEnabled: Boolean,
    onFinish: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var currentStep by remember { mutableIntStateOf(0) }

    BackHandler(enabled = currentStep > 0 || onBack != null) {
        if (currentStep > 0) {
            currentStep--
        } else {
            onBack?.invoke()
        }
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)))
                    .togetherWith(slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(350)))
            } else {
                (slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)))
                    .togetherWith(slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)))
            }
        },
        label = "WelcomeStepTransition",
    ) { step ->
        when (step) {
            0 -> StartScreen(
                animationsEnabled = animationsEnabled,
                onToNext = { currentStep = 1 },
            )

            1 -> DisclaimerScreen(
                rotaryScrollingEnabled = rotaryScrollingEnabled,
                animationsEnabled = animationsEnabled,
                onFinish = onFinish,
            )
        }
    }
}

// =========================================================================
// Re-WearBili StartScreen (Hero)
// =========================================================================

@Composable
private fun StartScreen(
    animationsEnabled: Boolean,
    onToNext: () -> Unit,
) {
    val localDensity = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val floatingHintAnimation by infiniteTransition.animateFloat(
        initialValue = with(localDensity) { -5.dp.toPx() },
        targetValue = with(localDensity) { 5.dp.toPx() },
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(
                durationMillis = 1100,
                easing = EaseInOutCubic,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "",
    )
    var isWelcomeAnimationPerformed by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = Unit) {
        isWelcomeAnimationPerformed = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToNext,
            ),
    ) {
        // 1. 动态流沙视频背景
        VideoSilkBackground()

        // 2. 中部浅蓝色渐变品牌文字
        BoompalaBrandText(modifier = Modifier.align(Alignment.Center))

        // 3. 底部呼吸浮动微标与提示
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(3f))
            AnimatedVisibility(
                visible = isWelcomeAnimationPerformed,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                enter = fadeIn(
                    tween(durationMillis = 1000),
                ) + slideInVertically(
                    tween(durationMillis = 1000),
                ) { it / 2 },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = if (animationsEnabled) floatingHintAnimation else 0f
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.welcome_tap_to_start),
                        fontSize = 12.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

// =========================================================================
// Disclaimer & Terms Screen (Clean & Minimalist Card UI with Emoji Headers)
// =========================================================================

@Composable
private fun DisclaimerScreen(
    rotaryScrollingEnabled: Boolean,
    animationsEnabled: Boolean,
    onFinish: () -> Unit,
) {
    val metrics = LocalUiMetrics.current

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_silk_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        RotaryScrollColumn(
            rotaryEnabled = rotaryScrollingEnabled,
            modifier = Modifier.fillMaxSize(),
            contentPadding = metrics.screenPadding,
            itemSpacing = metrics.itemSpacing,
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_terms_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.welcome_terms_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xB3FFFFFF),
                            fontSize = 11.sp,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // 卡片 1：🔮 文化研习与娱乐参考
            item(key = "card-cultural") {
                ReWearBiliCard(animationsEnabled = animationsEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.welcome_disclaimer_card_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.welcome_disclaimer_card_content),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                            ),
                            color = Color(0xFFEEEEEE),
                        )
                    }
                }
            }

            // 卡片 2：🛡️ 离线运行 · 零数据收集
            item(key = "card-offline") {
                ReWearBiliCard(animationsEnabled = animationsEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.welcome_offline_card_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.welcome_offline_card_content),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                            ),
                            color = Color(0xFFEEEEEE),
                        )
                    }
                }
            }

            // 卡片 3：📜 典籍出处 · 开源致谢
            item(key = "card-sources") {
                ReWearBiliCard(animationsEnabled = animationsEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.welcome_sources_card_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.welcome_sources_card_content),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                            ),
                            color = Color(0xFFEEEEEE),
                        )
                    }
                }
            }

            // 底部协议提示与确认进入按钮
            item(key = "action-enter") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_terms_footer_hint),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0x80FFFFFF),
                            fontSize = 10.sp,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_agree_and_enter),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
