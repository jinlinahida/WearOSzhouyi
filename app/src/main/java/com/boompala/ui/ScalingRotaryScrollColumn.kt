package com.boompala.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingParams
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.shirokoWearScalingParams

/**
 * 转发到 ShirokoWear UI 的缩放滚动容器。
 *
 * 签名与原本一致，所以既有调用点无需改动；表冠接线、ScreenScaffold 指示器与
 * 鱼眼降级规则现在只存在于库里一份。缩放参数的默认值同样取自库，
 * 避免"关掉动画"在两个工程里各写一遍而日后走样。
 */
@Composable
fun ScalingRotaryScrollColumn(
    rotaryEnabled: Boolean,
    hapticFeedbackEnabled: Boolean = LocalHapticFeedbackEnabled.current,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxSize(),
    state: ScalingLazyListState = rememberScalingLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    itemSpacing: Dp = 8.dp,
    autoCentering: AutoCenteringParams? = AutoCenteringParams(itemIndex = 0),
    scalingParams: ScalingParams = shirokoWearScalingParams(animationsEnabled),
    content: ScalingLazyListScope.() -> Unit,
) {
    ShirokoWearScalingRotaryColumn(
        modifier = modifier,
        rotaryEnabled = rotaryEnabled,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        animationsEnabled = animationsEnabled,
        state = state,
        contentPadding = contentPadding,
        itemSpacing = itemSpacing,
        autoCentering = autoCentering,
        scalingParams = scalingParams,
        content = content,
    )
}
