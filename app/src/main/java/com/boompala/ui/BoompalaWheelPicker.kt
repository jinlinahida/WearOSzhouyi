package com.boompala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 专为 Wear OS 打造的精美居中平滑吸附滚轮选择器（CommandIron 架构）。
 */
@Composable
fun <T> BoompalaWheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 34.dp,
    visibleItemsCount: Int = 3,
    labelProvider: (T) -> String = { it.toString() },
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val hapticIntensity = LocalHapticIntensity.current

    val totalHeight = itemHeight * visibleItemsCount
    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex),
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    // 监听滚轮吸附与居中 index 变化
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                lazyListState.firstVisibleItemIndex
            } else {
                val centerOffset = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
                val closestItem = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - centerOffset)
                }
                closestItem?.index ?: lazyListState.firstVisibleItemIndex
            }
        }
            .distinctUntilChanged()
            .filter { it in items.indices }
            .collect { centerIndex ->
                if (centerIndex != selectedIndex) {
                    onSelectedIndexChanged(centerIndex)
                    AppHaptics.coinToss(
                        context = context,
                        isChanging = false,
                        intensity = hapticIntensity,
                        enabled = hapticEnabled,
                    )
                }
            }
    }

    // 外部传入 selectedIndex 时联动滚动
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && !lazyListState.isScrollInProgress) {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val centerOffset = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            val currentCenterItem = visibleItems.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                kotlin.math.abs(itemCenter - centerOffset)
            }
            if (currentCenterItem?.index != selectedIndex) {
                lazyListState.animateScrollToItem(selectedIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .height(totalHeight)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // 1. 中心高亮选中胶囊槽位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF221E18))
                .border(
                    width = 1.2.dp,
                    color = Color(0xFFC5A059).copy(alpha = 0.55f),
                    shape = RoundedCornerShape(8.dp),
                ),
        )

        // 2. 滚轮 LazyColumn
        LazyColumn(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * ((visibleItemsCount - 1) / 2)),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(
                count = items.size,
                key = { index -> index },
            ) { index ->
                val isSelected by remember(index, selectedIndex) {
                    derivedStateOf { index == selectedIndex }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            scaleX = if (isSelected) 1.08f else 0.88f
                            scaleY = if (isSelected) 1.08f else 0.88f
                            alpha = if (isSelected) 1.0f else 0.42f
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelProvider(items[index]),
                        style = if (isSelected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // 3. 上下渐隐暗角遮罩（营造深邃圆柱曲面立体感）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.75f),
                        0.25f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.75f),
                    ),
                ),
        )
    }
}
