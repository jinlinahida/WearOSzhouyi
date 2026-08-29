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

// 十神通俗释义与人生大运阶段指引
private data class ShiShenInfo(
    val title: String,
    val theme: String,
    val description: String,
)

private val SHI_SHEN_INFO_MAP = mapOf(
    "正官" to ShiShenInfo("正官", "事业规范 · 贵人提携", "主循规守序、责任担当、职业进阶与声誉积累。有上级或体制相助，宜守正求进。"),
    "七杀" to ShiShenInfo("七杀", "开拓魄力 · 权威变革", "主果断威严、敢闯敢拼、直面竞争与破局。宜克服考验建立权威，忌急躁冲动。"),
    "偏官" to ShiShenInfo("偏官", "开拓魄力 · 权威变革", "主果断威严、敢闯敢拼、直面竞争与破局。宜克服考验建立权威，忌急躁冲动。"),
    "正印" to ShiShenInfo("正印", "学识涵养 · 仁厚福荫", "主文化修养、长辈师长庇佑、声望清贵。此期利进修深造、考学评职、蓄力厚发。"),
    "偏印" to ShiShenInfo("偏印", "独门才智 · 洞察玄妙", "主钻研专长、直觉敏锐、偏门技艺与独立思考。利从事学术、设计研发与专业探索。"),
    "枭神" to ShiShenInfo("枭神", "独门才智 · 洞察玄妙", "主钻研专长、直觉敏锐、偏门技艺与独立思考。利从事学术、设计研发与专业探索。"),
    "比肩" to ShiShenInfo("比肩", "朋辈同道 · 自强立身", "主朋友助力、志同道合、自信笃定。宜结伴创业共赢、自立自强，注意包容不同意见。"),
    "劫财" to ShiShenInfo("劫财", "广聚人脉 · 敢拼敢闯", "主社交活跃、敢打硬仗、善抓商机。人脉虽广但竞争激烈，利果断行动，注意守财防耗。"),
    "食神" to ShiShenInfo("食神", "才艺灵感 · 福寿闲雅", "主温润宽厚、口福康泰、才华自然流露。利文化艺术、创意输出，生活悠然自得。"),
    "伤官" to ShiShenInfo("伤官", "创新突围 · 锋芒才情", "主才思敏捷、打破常规、个性鲜明。利革新突破、巧思立功，注意谨言慎行防招忌。"),
    "正财" to ShiShenInfo("正财", "勤勉致富 · 稳筑家业", "主本职收入、踏实经营、资产稳固积累。付出与回报成正比，宜循序渐进、稳健理财。"),
    "偏财" to ShiShenInfo("偏财", "敏锐商机 · 财气亨通", "主眼界开阔、机缘巧合、资金流动活跃。利商务拓展、人脉变现，切忌盲目投机。"),
)

private fun getShiShenInfo(raw: String): ShiShenInfo? {
    return SHI_SHEN_INFO_MAP[raw] ?: SHI_SHEN_INFO_MAP.entries.firstOrNull { raw.contains(it.key) }?.value
}

private fun getFiveElementDesc(wx: FiveElement): String = when (wx) {
    FiveElement.METAL -> "金主义，沉稳果断、刚毅严正"
    FiveElement.WOOD -> "木主仁，生发向上、宽厚仁爱"
    FiveElement.WATER -> "水主智，聪颖机敏、润下通达"
    FiveElement.FIRE -> "火主礼，热情明朗、豪迈进取"
    FiveElement.EARTH -> "土主信，厚重敦实、稳健守诺"
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
                    Text(
                        text = stringResource(R.string.destiny_chart_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }

                // Profile Configuration Header Card
                item(key = "profile-header") {
                    ResultCard {
                        Text(
                            text = "当前推算生日时辰",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${SHICHEN_NAMES[selectedShichenIndex]} · ${selectedGender.titleZh}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.wearMarquee(settings.animationsEnabled),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val editDateInter = remember { MutableInteractionSource() }
                            BoompalaCardButton(
                                onClick = { activePicker = DestinyPickerMode.DATE },
                                modifier = Modifier
                                    .weight(1f)
                                    .wearPressFeedback(editDateInter),
                                interactionSource = editDateInter,
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text("改日期", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }

                            val editHourInter = remember { MutableInteractionSource() }
                            BoompalaCardButton(
                                onClick = { activePicker = DestinyPickerMode.SHICHEN },
                                modifier = Modifier
                                    .weight(1f)
                                    .wearPressFeedback(editHourInter),
                                interactionSource = editHourInter,
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text("改时辰", style = MaterialTheme.typography.labelSmall, maxLines = 1)
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
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text(selectedGender.titleZh, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }

                // 1. 生辰八字 (BaZi)
                item(key = "feature-bazi") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToBazi(selectedDate, selectedHour, selectedGender) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "生辰八字排盘",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "四柱十神 · 纳音藏干 · 十年大运",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 2. 西方本命星盘 (Western Natal Chart & 4 Elements)
                item(key = "feature-western") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToWestern(selectedDate, selectedHour) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "西方本命星盘",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "三大巨头 · 四象元素能量条",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 3. 生命灵数 (Numerology)
                item(key = "feature-numerology") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToNumerology(selectedDate) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "生命灵数",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "生命道路数 · 洛书九宫天赋",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 4. 袁天罡称骨算命 (Bone Weight)
                item(key = "feature-bone") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToBoneWeight(selectedDate, selectedHour) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "袁天罡称骨",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "四柱骨重 · 绝句传世歌诀",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 5. 九星气学 (Nine Star Ki)
                item(key = "feature-ninestar") {
                    val pressInter = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { onNavigateToNineStar(selectedDate) },
                        modifier = fullWidthModifier.wearPressFeedback(pressInter),
                        interactionSource = pressInter,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "九星气学",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "本命九星 · 守护五行 · 吉凶方位",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
            Text(
                text = "生辰八字排盘",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Summary Card
        item(key = "bazi-summary-card") {
            ResultCard {
                Text(
                    text = profile.shortSummaryZh,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.wearMarquee(animationsEnabled),
                )
                Text(
                    text = "元神日主：${profile.dayMaster.displayName} (${profile.dayMasterElement.displayName})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "日干代表自身根本心性与原动力，坐支代表内心归属。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DetailField(label = "四柱干支", value = profile.fourPillarsText)
                DetailField(label = "生肖属相", value = profile.shengXiao)
                DetailField(label = "胎元 · 命宫", value = "${profile.taiYuan} · ${profile.mingGong}")
            }
        }

        // Four Pillars Card with Stage Explanations
        item(key = "bazi-pillars-card") {
            ResultCard {
                Text(
                    text = "四柱格局与十神",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                DetailField(
                    label = "年柱 · ${profile.yearPillar.ganzhi.displayName} (祖基)",
                    value = "${profile.yearPillar.stemShiShen} · ${profile.yearPillar.naYin}",
                )
                DetailField(
                    label = "月柱 · ${profile.monthPillar.ganzhi.displayName} (事业)",
                    value = "${profile.monthPillar.stemShiShen} · ${profile.monthPillar.naYin}",
                )
                DetailField(
                    label = "日柱 · ${profile.dayPillar.ganzhi.displayName} (自身)",
                    value = "${profile.dayPillar.stemShiShen} · ${profile.dayPillar.naYin}",
                )
                val hourPillar = profile.hourPillar
                if (hourPillar != null) {
                    DetailField(
                        label = "时柱 · ${hourPillar.ganzhi.displayName} (归宿)",
                        value = "${hourPillar.stemShiShen} · ${hourPillar.naYin}",
                    )
                }
            }
        }

        // Five Elements Distribution Card
        item(key = "bazi-wuxing-card") {
            val wx = profile.wuXingDistribution
            ResultCard {
                Text(
                    text = "五行力量统计",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "主导五行：${wx.dominantElement.displayName}旺",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = getFiveElementDesc(wx.dominantElement),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // DaYun List with Detailed Plain Explanations
        if (profile.daYunList.isNotEmpty()) {
            item(key = "bazi-dayun-title") {
                Column {
                    Text(
                        text = "十年大运详解",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "每步大运主导十年人生阶段重点与气运导向",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            profile.daYunList.forEach { dy ->
                item(key = "dayun-${dy.index}") {
                    val shiShenInfo = getShiShenInfo(dy.stemShiShen)
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
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = dy.stemShiShen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        if (shiShenInfo != null) {
                            Text(
                                text = shiShenInfo.theme,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = shiShenInfo.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
            Text(
                text = "西方本命星盘",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Big Three Card with Plain Meaning
        item(key = "big-three-card") {
            ResultCard {
                Text(
                    text = "三主星 · Big Three",
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
                Spacer(modifier = Modifier.height(2.dp))

                // Sun
                Text(
                    text = "太阳 ☉ ${reading.sun.sign.displayNameZh} (${reading.sun.sign.element.displayNameZh})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "【核心自我】意志追求、外在人格与生命目标",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(2.dp))
                // Moon
                Text(
                    text = "月亮 ☽ ${reading.moon.sign.displayNameZh} (${reading.moon.sign.element.displayNameZh})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "【内心潜意识】情绪安全感来源与脆弱的情感需求",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val asc = reading.ascendant
                if (asc != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "上升 ASC ${asc.sign.displayNameZh} (${asc.sign.element.displayNameZh})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "【外在面具】他人初见印象、处事风格与对外窗口",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 4 Elements Balance Card - Clean, Dedicated Rows with Progress Bars
        item(key = "elements-card") {
            val eb = reading.elementBalance
            ResultCard {
                Text(
                    text = "四象元素能量分布",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = eb.balanceSummaryZh,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Detailed individual breakdown for all 4 elements
                val elementList = listOf(
                    Triple(ZodiacElement.FIRE, "🔥 火象", "行动 · 热情 · 勇气探索"),
                    Triple(ZodiacElement.EARTH, "🌍 土象", "务实 · 稳健 · 秩序筑基"),
                    Triple(ZodiacElement.AIR, "💨 风象", "思维 · 理智 · 沟通洞察"),
                    Triple(ZodiacElement.WATER, "💧 水象", "情感 · 直觉 · 深刻共情"),
                )

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    elementList.forEach { (elem, name, keywords) ->
                        val pct = eb.percentage(elem)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${pct}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(elem.colorHex),
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (pct / 100f).coerceIn(0.01f, 1f))
                                        .fillMaxHeight()
                                        .background(Color(elem.colorHex)),
                                )
                            }
                            Text(
                                text = keywords,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }

        // Planets Placement with Astrological Meanings
        item(key = "planets-card") {
            ResultCard {
                Text(
                    text = "各大星体黄道落座",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                val planetThemes = mapOf(
                    CelestialBody.MERCURY to "思维与沟通表达",
                    CelestialBody.VENUS to "情感审美与财禄",
                    CelestialBody.MARS to "执行力与竞争冲劲",
                    CelestialBody.JUPITER to "幸运机遇与宏观扩张",
                    CelestialBody.SATURN to "责任纪律与人生考验",
                )
                reading.planets.forEach { p ->
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${p.body.displayNameZh} ${p.body.symbol}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${p.sign.displayNameZh} · 第${p.houseNumber}宫",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        planetThemes[p.body]?.let { theme ->
                            Text(
                                text = "掌管：$theme",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                            )
                        }
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
            Text(
                text = "毕达哥拉斯生命灵数",
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

        // Core Numbers with Clear Explanations
        item(key = "core-numbers-card") {
            ResultCard {
                Text(
                    text = "核心天赋灵数",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailField(label = "生日数", value = reading.birthdayNumber.toString())
                    Text(
                        text = "先天自带的直觉性格与基础天赋",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    DetailField(label = "态度数", value = reading.attitudeNumber.toString())
                    Text(
                        text = "面对外界与生活变故时的第一反应态度",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    DetailField(label = "当年流年数", value = reading.personalYearNumber.toString())
                    Text(
                        text = "当前年度所处的9年灵数运势周期主题",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        // Lo Shu Grid
        item(key = "loshu-card") {
            ResultCard {
                Text(
                    text = "九宫数阵天赋连线",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                // Render 3x3 Lo Shu visual grid
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
                        text = "能量均匀分布，多维平衡发展",
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
            Text(
                text = "袁天罡称骨算命",
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "【称骨说明】年月日时四柱相加，十钱为一两。三至四两为常人格局；四两以上渐入佳境；五六两主富贵福寿。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
        }

        item(key = "bone-poem-card") {
            ResultCard {
                Text(
                    text = "称骨绝句歌诀",
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
                    text = "白话命运评注",
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
            Text(
                text = "九星气学命盘",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item(key = "year-star-card") {
            ResultCard {
                Text(
                    text = "本命年星 (终身根本气运)",
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
                DetailField(label = "吉神生旺方", value = reading.yearStar.luckyDirectionsZh)
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
                    text = "月命星 (潜意识本能性格)",
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
                    text = "命局五行气场主题",
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
