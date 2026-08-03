package com.boompala.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.AppScaffold
import com.boompala.engine.LiuYaoEngine
import com.boompala.engine.calendar.SixTailGanzhiCalendar
import com.boompala.engine.data.EmptyLineTextRepository
import com.boompala.engine.data.EmptyHexagramInterpretationRepository
import com.boompala.engine.data.HexagramInterpretationRepository
import com.boompala.engine.data.JsonHexagramInterpretationRepository
import com.boompala.engine.data.JsonLineTextRepository
import com.boompala.engine.data.LineTextRepository
import com.boompala.engine.data.KnowledgeArticle
import com.boompala.engine.data.JsonKnowledgeRepository
import com.boompala.engine.data.JsonClassicalTextRepository
import com.boompala.engine.data.EmptyClassicalTextRepository
import com.boompala.engine.data.hexagramReferences
import com.boompala.engine.model.DivinationResult
import com.boompala.engine.model.HexagramInput
import com.boompala.engine.meihua.MeiHuaTimeEngine
import com.boompala.engine.meihua.MeiHuaTimeReading
import com.boompala.engine.xiaoliuren.*
import com.boompala.archive.*
import android.widget.Toast
import com.boompala.settings.AppSettings
import com.boompala.settings.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class AppScreen {
    HOME,
    YAO_INPUT,
    RESULT,
    MEIHUA_TIME,
    MEIHUA_RESULT,
    SETTINGS,
    ABOUT,
    XIAO_LIU_REN,
    ARCHIVES,
    ARCHIVE_TAG,
    ARCHIVE_DETAIL,
    COMPASS,
    BROWSE, HEXAGRAM_BROWSER, HEXAGRAM_DETAIL, KNOWLEDGE_LIST, KNOWLEDGE_DETAIL,
}

internal fun AppScreen.backDestination(): AppScreen? = when (this) {
    AppScreen.HOME -> null
    AppScreen.YAO_INPUT -> AppScreen.HOME
    AppScreen.RESULT -> AppScreen.YAO_INPUT
    AppScreen.MEIHUA_TIME -> AppScreen.HOME
    AppScreen.MEIHUA_RESULT -> AppScreen.MEIHUA_TIME
    AppScreen.SETTINGS -> AppScreen.HOME
    AppScreen.ABOUT -> AppScreen.SETTINGS
    AppScreen.XIAO_LIU_REN -> AppScreen.HOME
    AppScreen.ARCHIVES -> AppScreen.HOME
    AppScreen.ARCHIVE_TAG -> AppScreen.XIAO_LIU_REN
    AppScreen.ARCHIVE_DETAIL -> AppScreen.ARCHIVES
    AppScreen.COMPASS -> AppScreen.HOME
    AppScreen.BROWSE -> AppScreen.HOME
    AppScreen.HEXAGRAM_BROWSER -> AppScreen.BROWSE
    AppScreen.HEXAGRAM_DETAIL -> AppScreen.HEXAGRAM_BROWSER
    AppScreen.KNOWLEDGE_LIST -> AppScreen.BROWSE
    AppScreen.KNOWLEDGE_DETAIL -> AppScreen.KNOWLEDGE_LIST
}

data class GeneratedReading(
    val input: HexagramInput,
    val result: DivinationResult,
    val interpretations: HexagramInterpretationRepository,
)

data class GeneratedMeiHuaReading(
    val reading: MeiHuaTimeReading,
    val interpretations: HexagramInterpretationRepository,
)

private data class OfflineReadingDependencies(
    val engine: LiuYaoEngine,
    val interpretations: HexagramInterpretationRepository,
)

@Composable
fun BoompalaApp() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val settingsRepository = remember(context) {
        SettingsRepository(context.applicationContext)
    }
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings.DEFAULT)
    val screenShape = settings.resolvedScreenShape(configuration.isScreenRound)
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var reading by remember { mutableStateOf<GeneratedReading?>(null) }
    var meiHuaReading by remember { mutableStateOf<MeiHuaTimeReading?>(null) }
    var generatedMeiHuaReading by remember { mutableStateOf<GeneratedMeiHuaReading?>(null) }
    var xlrReading by remember { mutableStateOf<XiaoLiuRenReading?>(null) }
    var archiveDraft by remember { mutableStateOf<ArchiveDraft?>(null) }
    var archiveDetailId by remember { mutableStateOf<Long?>(null) }
    var archiveReturnScreen by remember { mutableStateOf(AppScreen.HOME) }
    var archiveRefresh by remember { mutableIntStateOf(0) }
    var browserData by remember { mutableStateOf<BrowserData?>(null) }
    var selectedHexagram by remember { mutableStateOf<com.boompala.engine.data.HexagramReference?>(null) }
    var selectedKnowledge by remember { mutableStateOf<KnowledgeArticle?>(null) }
    val archiveRepository = remember(context) { ArchiveRepository(context) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationId by remember { mutableIntStateOf(0) }
    var meiHuaGenerationId by remember { mutableIntStateOf(0) }
    val dependenciesReady = remember(context) {
        CompletableDeferred<OfflineReadingDependencies>()
    }
    val meiHuaEngine = remember { MeiHuaTimeEngine(SixTailGanzhiCalendar()) }
    val xlrEngine = remember { XiaoLiuRenEngine(SixTailGanzhiCalendar()) }
    LaunchedEffect(dependenciesReady) {
        val dependencies = withContext(Dispatchers.IO) {
            createOfflineReadingDependencies(context.applicationContext)
        }
        dependenciesReady.complete(dependencies)
    }
    val backDestination = screen.backDestination()

    BackHandler(enabled = backDestination != null) {
        generationId++
        meiHuaGenerationId++
        isGenerating = false
        screen = requireNotNull(backDestination)
    }

    MaterialTheme {
        WithContentScale(
            settings = settings,
            screenShape = screenShape,
        ) {
            AppScaffold {
                val screenContent: @Composable (AppScreen) -> Unit = { currentScreen ->
                    when (currentScreen) {
                        AppScreen.HOME -> HomeScreen(
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onSixYaoClick = { screen = AppScreen.YAO_INPUT },
                            onMeiHuaClick = {
                                meiHuaGenerationId++
                                meiHuaReading = null
                                generatedMeiHuaReading = null
                                screen = AppScreen.MEIHUA_TIME
                            },
                            onSettingsClick = { screen = AppScreen.SETTINGS }, onXiaoLiuRenClick = { xlrReading = null; screen = AppScreen.XIAO_LIU_REN }, onArchiveClick = { screen = AppScreen.ARCHIVES }, onCompassClick = { screen = AppScreen.COMPASS }, onBrowseClick = { screen = AppScreen.BROWSE },
                        )

                        AppScreen.YAO_INPUT -> YaoInputScreen(
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            isGenerating = isGenerating,
                            onBack = {
                                generationId++
                                isGenerating = false
                                screen = AppScreen.HOME
                            },
                            onGenerate = { input ->
                                if (!isGenerating) {
                                    isGenerating = true
                                    val requestId = ++generationId
                                    scope.launch {
                                        try {
                                            val dependencies = dependenciesReady.await()
                                            val result = withContext(Dispatchers.Default) {
                                                dependencies.engine.calculate(input)
                                            }
                                            if (
                                                requestId == generationId &&
                                                screen == AppScreen.YAO_INPUT
                                            ) {
                                                reading = GeneratedReading(
                                                    input = input,
                                                    result = result,
                                                    interpretations = dependencies.interpretations,
                                                )
                                                screen = AppScreen.RESULT
                                            }
                                        } finally {
                                            if (requestId == generationId) {
                                                isGenerating = false
                                            }
                                        }
                                    }
                                }
                            },
                        )

                        AppScreen.RESULT -> reading?.let { currentReading ->
                            LiuYaoResultContent(
                                reading = currentReading,
                                rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                                onBack = { screen = AppScreen.YAO_INPUT },
                                onArchive = { r -> archiveReturnScreen=AppScreen.RESULT; archiveDraft = ArchiveDraft("", "", 0xFF4CAF50, ArchiveSource.LIU_YAO, r.castAt.toEpochMilli(), "本卦${r.original.name}", ArchiveSnapshotCodec.encode(r, currentReading.interpretations)); screen = AppScreen.ARCHIVE_TAG },
                            )
                        } ?: HomeScreen(
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onSixYaoClick = { screen = AppScreen.YAO_INPUT },
                            onMeiHuaClick = {
                                meiHuaGenerationId++
                                meiHuaReading = null
                                generatedMeiHuaReading = null
                                screen = AppScreen.MEIHUA_TIME
                            },
                            onSettingsClick = { screen = AppScreen.SETTINGS }, onXiaoLiuRenClick = { screen = AppScreen.XIAO_LIU_REN }, onArchiveClick = { screen = AppScreen.ARCHIVES }, onCompassClick = { screen = AppScreen.COMPASS }, onBrowseClick = { screen = AppScreen.BROWSE },
                        )

                        AppScreen.XIAO_LIU_REN -> XiaoLiuRenScreen(xlrEngine, xlrReading, settings.rotaryScrollingEnabled, { xlrReading = it }, { r -> archiveReturnScreen=AppScreen.XIAO_LIU_REN; archiveDraft = ArchiveDraft("", "", 0xFF4CAF50, ArchiveSource.XIAO_LIU_REN, r.timeInfo.gregorianDateTime.toInstant().toEpochMilli(), "最终${r.finalPalace.displayName}", ArchiveSnapshotCodec.encode(r)); screen = AppScreen.ARCHIVE_TAG }, { screen = AppScreen.HOME })
                        AppScreen.ARCHIVE_TAG -> archiveDraft?.let { d -> ArchiveTagScreen(d, archiveRepository, settings.rotaryScrollingEnabled, { Toast.makeText(context, "已保存归档", Toast.LENGTH_SHORT).show(); screen = archiveReturnScreen }, { screen = archiveReturnScreen }) }
                        AppScreen.ARCHIVES -> { archiveRefresh; ArchiveListScreen(archiveRepository, settings.rotaryScrollingEnabled, { id -> archiveDetailId=id; screen=AppScreen.ARCHIVE_DETAIL }, { screen = AppScreen.HOME }) }
                        AppScreen.ARCHIVE_DETAIL -> archiveDetailId?.let { id -> archiveRepository.get(id)?.let { record -> ArchiveDetailScreen(record, archiveRepository, settings.rotaryScrollingEnabled, { archiveRefresh++; screen=AppScreen.ARCHIVES }, { screen=AppScreen.ARCHIVES }) } }
                        AppScreen.COMPASS -> CompassScreen(settings.rotaryScrollingEnabled) { screen = AppScreen.HOME }
                        AppScreen.BROWSE -> browserData?.let { d -> BrowseHomeScreen(d, settings.rotaryScrollingEnabled, { screen = AppScreen.HEXAGRAM_BROWSER }, { screen = AppScreen.KNOWLEDGE_LIST }, { screen = AppScreen.HOME }) }
                        AppScreen.HEXAGRAM_BROWSER -> browserData?.let { d -> HexagramBrowserScreen(d, settings.rotaryScrollingEnabled, { selectedHexagram = it; screen = AppScreen.HEXAGRAM_DETAIL }, { screen = AppScreen.BROWSE }) }
                        AppScreen.HEXAGRAM_DETAIL -> { val h = selectedHexagram; val d = browserData; if (h != null && d != null) HexagramDetailScreen(h, d, settings.rotaryScrollingEnabled) { screen = AppScreen.HEXAGRAM_BROWSER } }
                        AppScreen.KNOWLEDGE_LIST -> browserData?.let { d -> KnowledgeListScreen(d.knowledge, settings.rotaryScrollingEnabled, { selectedKnowledge = it; screen = AppScreen.KNOWLEDGE_DETAIL }, { screen = AppScreen.BROWSE }) }
                        AppScreen.KNOWLEDGE_DETAIL -> selectedKnowledge?.let { KnowledgeDetailScreen(it, settings.rotaryScrollingEnabled) { screen = AppScreen.KNOWLEDGE_LIST } }

                        AppScreen.MEIHUA_TIME -> MeiHuaTimeScreen(
                            engine = meiHuaEngine,
                            initialReading = meiHuaReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onReadingChanged = { meiHuaReading = it },
                            onViewReading = { currentReading ->
                                val requestId = ++meiHuaGenerationId
                                scope.launch {
                                    val dependencies = dependenciesReady.await()
                                    if (
                                        requestId == meiHuaGenerationId &&
                                        screen == AppScreen.MEIHUA_TIME
                                    ) {
                                        meiHuaReading = currentReading
                                        generatedMeiHuaReading = GeneratedMeiHuaReading(
                                            reading = currentReading,
                                            interpretations = dependencies.interpretations,
                                        )
                                        screen = AppScreen.MEIHUA_RESULT
                                    }
                                }
                            },
                            onBack = { screen = AppScreen.HOME },
                        )

                        AppScreen.MEIHUA_RESULT -> generatedMeiHuaReading?.let { currentReading ->
                            MeiHuaResultContent(
                                reading = currentReading.reading,
                                interpretations = currentReading.interpretations,
                                rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                                onBack = { screen = AppScreen.MEIHUA_TIME },
                                onArchive = { r -> archiveReturnScreen=AppScreen.MEIHUA_RESULT; archiveDraft = ArchiveDraft("", "", 0xFF4CAF50, ArchiveSource.MEI_HUA, r.timeInfo.gregorianDateTime.toInstant().toEpochMilli(), "本卦${r.original.name}", ArchiveSnapshotCodec.encode(r, currentReading.interpretations)); screen = AppScreen.ARCHIVE_TAG },
                            )
                        } ?: MeiHuaTimeScreen(
                            engine = meiHuaEngine,
                            initialReading = meiHuaReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onReadingChanged = { meiHuaReading = it },
                            onViewReading = { },
                            onBack = { screen = AppScreen.HOME },
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onScreenModeSelected = { mode ->
                                scope.launch { settingsRepository.setScreenMode(mode) }
                            },
                            onContentSizeSelected = { size ->
                                scope.launch { settingsRepository.setContentSize(size) }
                            },
                            onAnimationsEnabledChange = { enabled ->
                                scope.launch { settingsRepository.setAnimationsEnabled(enabled) }
                            },
                            onRotaryScrollingEnabledChange = { enabled ->
                                scope.launch {
                                    settingsRepository.setRotaryScrollingEnabled(enabled)
                                }
                            },
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onAboutClick = { screen = AppScreen.ABOUT },
                            onBack = { screen = AppScreen.HOME },
                        )

                        AppScreen.ABOUT -> AboutScreen(
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onBack = { screen = AppScreen.SETTINGS },
                        )
                    }
                }

                SingleScreenFade(
                    screen = screen,
                    animationsEnabled = settings.animationsEnabled,
                ) {
                    screenContent(screen)
                }
                LaunchedEffect(screen) {
                    if (screen == AppScreen.BROWSE && browserData == null) browserData = withContext(Dispatchers.IO) { loadBrowserData(context) }
                }
            }
        }
    }
}

@Composable
private fun SingleScreenFade(
    screen: AppScreen,
    animationsEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val alpha = remember(screen, animationsEnabled) {
        Animatable(if (animationsEnabled) 0f else 1f)
    }
    LaunchedEffect(alpha) {
        if (animationsEnabled) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 90),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha.value
            },
    ) {
        content()
    }
}

private fun createOfflineReadingDependencies(context: android.content.Context): OfflineReadingDependencies =
    OfflineReadingDependencies(
        engine = LiuYaoEngine(
            calendar = SixTailGanzhiCalendar(),
            lineTexts = loadLineTexts(context),
        ),
        interpretations = loadHexagramInterpretations(context),
    )

private fun loadLineTexts(context: android.content.Context): LineTextRepository =
    runCatching {
        context.assets.open("yao_text.json").bufferedReader().use(JsonLineTextRepository::fromReader)
    }.getOrDefault(EmptyLineTextRepository)

private fun loadHexagramInterpretations(context: android.content.Context): HexagramInterpretationRepository =
    runCatching {
        context.assets.open("hexagram_interpretations.json").bufferedReader()
            .use(JsonHexagramInterpretationRepository::fromReader)
    }.getOrDefault(EmptyHexagramInterpretationRepository)

private fun loadBrowserData(context: android.content.Context): BrowserData = BrowserData(
    hexagrams = hexagramReferences(),
    lines = loadLineTexts(context),
    classics = runCatching { context.assets.open("yao_text.json").bufferedReader().use(JsonClassicalTextRepository::fromReader) }.getOrDefault(EmptyClassicalTextRepository),
    interpretations = loadHexagramInterpretations(context),
    knowledge = runCatching {
        context.assets.open("knowledge.json").bufferedReader().use(JsonKnowledgeRepository::fromReader).articles()
    }.getOrDefault(emptyList()),
)
