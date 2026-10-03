package com.boompala.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import com.boompala.R
import com.boompala.engine.astrology.*
import com.boompala.engine.bazi.*
import com.boompala.engine.bone.*
import com.boompala.engine.model.FiveElement
import com.boompala.engine.ninestar.*
import com.boompala.engine.numerology.*
import com.boompala.settings.AppSettings
import java.time.LocalDate

private val SHICHEN_NAMES = listOf(
    "子时 · 23-01点",
    "丑时 · 01-03点",
    "寅时 · 03-05点",
    "卯时 · 05-07点",
    "辰时 · 07-09点",
    "巳时 · 09-11点",
    "午时 · 11-13点",
    "未时 · 13-15点",
    "申时 · 15-17点",
    "酉时 · 17-19点",
    "戌时 · 19-21点",
    "亥时 · 21-23点",
    "时辰未知",
)

private fun hourToShichenIdx(hour: Int?): Int {
    if (hour == null) return 12
    if (hour >= 23 || hour == 0) return 0
    return ((hour + 1) / 2).coerceIn(0, 11)
}

private fun shichenIdxToHour(idx: Int): Int? {
    return when (idx) {
        0 -> 0; 1 -> 2; 2 -> 4; 3 -> 6; 4 -> 8; 5 -> 10
        6 -> 12; 7 -> 14; 8 -> 16; 9 -> 18; 10 -> 20; 11 -> 22
        else -> null
    }
}

private enum class DestinyPickerMode {
    NONE, DATE, SHICHEN
}

@Composable
fun DestinyChartMenuScreen(
    settings: AppSettings,
    onNavigateToBazi: (LocalDate, Int?, BaziGender) -> Unit,
    onNavigateToWestern: (LocalDate, Int?) -> Unit,
    onNavigateToNumerology: (LocalDate) -> Unit,
    onNavigateToBoneWeight: (LocalDate, Int?) -> Unit,
    onNavigateToNineStar: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current
    val initialDate = remember(settings.userBirthDate) {
        val parsed = settings.userBirthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        parsed ?: LocalDate.of(1995, 6, 15)
    }

    var selectedDate by rememberSaveable { mutableStateOf(initialDate) }
    var selectedShichenIndex by rememberSaveable { mutableIntStateOf(hourToShichenIdx(settings.userBirthHour)) }
    var selectedGender by rememberSaveable { mutableStateOf(settings.userGender) }
    var activePicker by rememberSaveable { mutableStateOf(DestinyPickerMode.NONE) }

    BackHandler(enabled = activePicker != DestinyPickerMode.NONE) {
        activePicker = DestinyPickerMode.NONE
    }

    when (activePicker) {
        DestinyPickerMode.DATE -> {
            DatePicker(
                initialDate = selectedDate,
                onDatePicked = { picked ->
                    selectedDate = picked
                    activePicker = DestinyPickerMode.NONE
                },
                minValidDate = LocalDate.of(1920, 1, 1),
                maxValidDate = LocalDate.now(),
            )
        }

        DestinyPickerMode.SHICHEN -> {
            DestinyShichenPicker(
                initialIndex = selectedShichenIndex,
                onShichenPicked = { picked ->
                    selectedShichenIndex = picked
                    activePicker = DestinyPickerMode.NONE
                },
                onDismiss = { activePicker = DestinyPickerMode.NONE },
            )
        }

        DestinyPickerMode.NONE -> {
            val selectedHour = remember(selectedShichenIndex) { shichenIdxToHour(selectedShichenIndex) }
            val fullWidthModifier = Modifier.fillMaxWidth()

            RotaryScrollColumn(
                rotaryEnabled = settings.rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "title") {
                    ScreenTitle(
                        text = stringResource(R.string.destiny_chart_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }

                // Profile Configuration Header Card
                item(key = "profile-header") {
                    val lunarDateText = remember(selectedDate, selectedHour) {
                        BaziEngine.formatLunarDate(selectedDate, selectedHour)
                    }
                    ResultCard {
                        Text(
                            text = "生辰信息",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "公历 ${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 · ${SHICHEN_NAMES[selectedShichenIndex].substringBefore(" ·")}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (lunarDateText.isNotBlank()) {
                            Text(
                                text = "农历 $lunarDateText · ${selectedGender.titleZh}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.wearMarquee(settings.animationsEnabled),
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val editDateInter = remember { MutableInteractionSource() }
                            BoompalaCardButton(
                                onClick = { activePicker = DestinyPickerMode.DATE },
                                modifier = Modifier
                                    .weight(1f)
                                    .wearPressFeedback(editDateInter),
                                interactionSource = editDateInter,
                                contentPadding = PaddingValues(vertical = 5.dp, horizontal = 4.dp),
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text("修改生辰", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }

                            val genderInter = remember { MutableInteractionSource() }
                            BoompalaCardButton(
                                onClick = {
                                    selectedGender = if (selectedGender == BaziGender.MALE) BaziGender.FEMALE else BaziGender.MALE
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .wearPressFeedback(genderInter),
                                interactionSource = genderInter,
                                contentPadding = PaddingValues(vertical = 5.dp, horizontal = 4.dp),
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text(selectedGender.titleZh, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }

                // 1. 生辰八字
                item(key = "feature-bazi") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToBazi(selectedDate, selectedHour, selectedGender) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Text(
                            text = "生辰八字",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // 2. 本命星盘
                item(key = "feature-western") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToWestern(selectedDate, selectedHour) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Text(
                            text = "本命星盘",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // 3. 袁天罡称骨
                item(key = "feature-bone") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToBoneWeight(selectedDate, selectedHour) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Text(
                            text = "袁天罡称骨",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // 4. 生命灵数
                item(key = "feature-numerology") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToNumerology(selectedDate) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Text(
                            text = "生命灵数",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // 5. 九星气学
                item(key = "feature-ninestar") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToNineStar(selectedDate) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Text(
                            text = "九星气学",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                item(key = "back-btn") {
                    val backInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = onBack,
                        modifier = fullWidthModifier.wearPressFeedback(backInter),
                        interactionSource = backInter,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }
    }
}

@Composable
fun BaziDetailScreen(
    profile: BaziProfile,
    rotaryEnabled: Boolean,
    animationsEnabled: Boolean,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotaryEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "bazi-title") {
            ScreenTitle(
                text = "生辰八字",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Summary Card
        item(key = "bazi-summary-card") {
            ResultCard {
                Text(
                    text = "${profile.gender.titleZh} · 日主 ${profile.dayMaster.displayName}${profile.dayMasterElement.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.wearMarquee(animationsEnabled),
                )
                if (profile.lunarDateText.isNotBlank()) {
                    DetailField(label = "农历生辰", value = profile.lunarDateText)
                }
                DetailField(label = "四柱干支", value = profile.fourPillarsText)
                DetailField(label = "生肖属相", value = profile.shengXiao)
                DetailField(label = "胎元 · 命宫", value = "${profile.taiYuan} · ${profile.mingGong}")
            }
        }

        // Four Pillars Card
        item(key = "bazi-pillars-card") {
            val pillars = listOfNotNull(
                Triple("年柱", profile.yearPillar, false),
                Triple("月柱", profile.monthPillar, false),
                Triple("日柱", profile.dayPillar, true),
                profile.hourPillar?.let { Triple("时柱", it, false) },
            )
            ResultCard {
                Text(
                    text = "四柱排盘",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    pillars.forEach { (name, pillar, isDay) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                            )
                            Text(
                                text = if (isDay) "日元" else pillar.stemShiShen,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pillar.ganzhi.heavenlyStem.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = pillar.ganzhi.earthlyBranch.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pillar.hiddenStemsText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                            )
                            Text(
                                text = pillar.naYin,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                maxLines = 1,
                            )
                            Text(
                                text = pillar.diShi,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }
        }

        // Five Elements Distribution Card
        item(key = "bazi-wuxing-card") {
            val wx = profile.wuXingDistribution
            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "五行分布",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${wx.dominantElement.displayName}旺",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("金 ${wx.metalCount}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("木 ${wx.woodCount}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("水 ${wx.waterCount}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("火 ${wx.fireCount}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("土 ${wx.earthCount}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // DaYun List
        if (profile.daYunList.isNotEmpty()) {
            item(key = "bazi-dayun-title") {
                Text(
                    text = "大运 · ${if (profile.isForward) "顺行" else "逆行"} · ${profile.startAge}岁起运",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            val currentYear = LocalDate.now().year
            profile.daYunList.forEach { dy ->
                item(key = "dayun-${dy.index}") {
                    val isCurrent = currentYear in dy.startYear..dy.endYear
                    ResultCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${dy.startAge}-${dy.endAge}岁 · ${dy.ganzhi.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isCurrent) {
                                    Text(
                                        text = "当前",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(
                                    text = dy.stemShiShen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                        Text(
                            text = "${dy.startYear}年 - ${dy.endYear}年",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }

        item(key = "bazi-back-button") {
            val backInter = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().wearPressFeedback(backInter),
                interactionSource = backInter,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
    }
}

@Composable
fun WesternChartScreen(
    reading: WesternChartReading,
    rotaryEnabled: Boolean,
    animationsEnabled: Boolean,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotaryEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "western-title") {
            ScreenTitle(
                text = "本命星盘",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // 核心落座 (太阳、月亮、上升)
        item(key = "big-three-card") {
            ResultCard {
                Text(
                    text = "核心落座",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = reading.bigThreeSummary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.wearMarquee(animationsEnabled),
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Sun
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "太阳 ☉ ${reading.sun.sign.displayNameZh}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "第${reading.sun.houseNumber}宫 · ${reading.sun.formattedDegree}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                // Moon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "月亮 ☽ ${reading.moon.sign.displayNameZh}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "第${reading.moon.houseNumber}宫 · ${reading.moon.formattedDegree}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val asc = reading.ascendant
                if (asc != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "上升 ASC ${asc.sign.displayNameZh}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "第${asc.houseNumber}宫 · ${asc.formattedDegree}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 四象元素分布：展示客观落座星体，彻底清除猎奇百分比与进度条
        item(key = "elements-card") {
            val eb = reading.elementBalance
            ResultCard {
                Text(
                    text = "四象元素分布",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = eb.balanceSummaryZh,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))

                val elements = listOf(
                    ZodiacElement.FIRE,
                    ZodiacElement.EARTH,
                    ZodiacElement.AIR,
                    ZodiacElement.WATER,
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    elements.forEach { elem ->
                        val bodies = eb.bodiesFor(elem)
                        val bodiesText = if (bodies.isNotEmpty()) {
                            bodies.joinToString("、") { it.displayNameZh }
                        } else {
                            "无落座"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${elem.displayNameZh} (${bodies.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(elem.colorHex),
                            )
                            Text(
                                text = bodiesText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // 主要行星落座：水金火木土（排除日月升，移除冗余套话）
        item(key = "planets-card") {
            ResultCard {
                Text(
                    text = "主要行星落座",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                reading.majorPlanets.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${p.body.displayNameZh} ${p.body.symbol}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${p.sign.displayNameZh} · 第${p.houseNumber}宫 (${p.formattedDegree})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item(key = "western-back-btn") {
            val backInter = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().wearPressFeedback(backInter),
                interactionSource = backInter,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
    }
}

@Composable
fun NumerologyDetailScreen(
    reading: NumerologyReading,
    rotaryEnabled: Boolean,
    animationsEnabled: Boolean,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotaryEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "num-title") {
            ScreenTitle(
                text = "生命灵数",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Life Path Number Big Badge
        item(key = "life-path-badge") {
            ResultCard {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = reading.lifePathNumber.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = "生命道路数 · ${reading.lifePathInfo.titleZh}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = reading.lifePathInfo.keywordsZh,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = reading.lifePathInfo.descriptionZh,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 核心灵数
        item(key = "core-numbers-card") {
            ResultCard {
                Text(
                    text = "核心灵数",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailField(label = "生日数", value = reading.birthdayNumber.toString())
                    DetailField(label = "态度数", value = reading.attitudeNumber.toString())
                    DetailField(label = "流年数", value = reading.personalYearNumber.toString())
                }
            }
        }

        // 九宫数盘
        item(key = "loshu-card") {
            ResultCard {
                Text(
                    text = "九宫数盘",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                val loShuLayout = listOf(
                    listOf(4, 9, 2),
                    listOf(3, 5, 7),
                    listOf(8, 1, 6),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    loShuLayout.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { digit ->
                                val count = reading.loShuGrid.digitCounts[digit] ?: 0
                                val isActive = count > 0
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainer
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (isActive) "$digit" else "-",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    )
                                }
                            }
                        }
                    }
                }

                reading.loShuGrid.lines.filter { it.isComplete }.forEach { line ->
                    DetailField(label = "✓ ${line.nameZh}", value = line.descriptionZh)
                }
                if (reading.loShuGrid.lines.none { it.isComplete }) {
                    Text(
                        text = "能量分布均匀",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "num-back-btn") {
            val backInter = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().wearPressFeedback(backInter),
                interactionSource = backInter,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
    }
}

@Composable
fun BoneWeightDetailScreen(
    reading: BoneWeightReading,
    rotaryEnabled: Boolean,
    animationsEnabled: Boolean,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotaryEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "bone-title") {
            ScreenTitle(
                text = "袁天罡称骨",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item(key = "bone-weight-card") {
            ResultCard {
                Text(
                    text = "称骨总重",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = reading.formattedWeightZh,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = reading.lunarDateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("年骨：${reading.yearWeightQian}钱", style = MaterialTheme.typography.labelSmall)
                    Text("月骨：${reading.monthWeightQian}钱", style = MaterialTheme.typography.labelSmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("日骨：${reading.dayWeightQian}钱", style = MaterialTheme.typography.labelSmall)
                    Text("时骨：${reading.hourWeightQian}钱", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item(key = "bone-poem-card") {
            ResultCard {
                Text(
                    text = "称骨歌诀",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                reading.poemLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item(key = "bone-explanation-card") {
            ResultCard {
                Text(
                    text = "注解",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = reading.explanationZh,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "bone-back-btn") {
            val backInter = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().wearPressFeedback(backInter),
                interactionSource = backInter,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
    }
}

@Composable
fun NineStarDetailScreen(
    reading: NineStarKiReading,
    rotaryEnabled: Boolean,
    animationsEnabled: Boolean,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotaryEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "ninestar-title") {
            ScreenTitle(
                text = "九星气学",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item(key = "year-star-card") {
            ResultCard {
                Text(
                    text = "本命年星",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = reading.yearStar.nameZh,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                DetailField(label = "本命卦象", value = reading.yearStar.trigramZh)
                DetailField(label = "守护五行", value = reading.yearStar.element.displayName)
                DetailField(label = "吉旺方位", value = reading.yearStar.luckyDirectionsZh)
                Text(
                    text = reading.yearStar.personalityZh,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item(key = "month-star-card") {
            ResultCard {
                Text(
                    text = "月命星",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = reading.monthStar.nameZh,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                DetailField(label = "月命卦象", value = reading.monthStar.trigramZh)
                DetailField(label = "气场特质", value = reading.monthStar.natureZh)
            }
        }

        item(key = "theme-card") {
            ResultCard {
                Text(
                    text = "气场主题",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = reading.energyThemeZh,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item(key = "ninestar-back-btn") {
            val backInter = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().wearPressFeedback(backInter),
                interactionSource = backInter,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
    }
}

@Composable
private fun DestinyShichenPicker(
    initialIndex: Int,
    onShichenPicked: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    val pickerState = rememberPickerState(
        initialNumberOfOptions = SHICHEN_NAMES.size,
        initiallySelectedIndex = initialIndex.coerceIn(0, SHICHEN_NAMES.lastIndex),
        shouldRepeatOptions = false,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = "选择出生时辰",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                maxLines = 1,
            )
            Picker(
                state = pickerState,
                contentDescription = { "选择出生时辰" },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { optionIndex ->
                val isSelected = optionIndex == pickerState.selectedOptionIndex
                Text(
                    text = SHICHEN_NAMES[optionIndex],
                    style = if (isSelected) {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val confirmInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = { onShichenPicked(pickerState.selectedOptionIndex) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 6.dp)
                    .wearPressFeedback(confirmInteraction),
                interactionSource = confirmInteraction,
                colors = BoompalaButtonDefaults.buttonColors(),
            ) {
                Text(
                    text = "✓ 确定",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}
