package com.boompala.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.boompala.R
import com.boompala.engine.LiuYaoEngine
import com.boompala.engine.calendar.SixTailDailyAlmanac
import com.boompala.engine.calendar.SixTailGanzhiCalendar
import com.boompala.engine.dailyfortune.DailyFortuneEngine
import com.boompala.engine.dailyfortune.DailyFortuneReading
import java.time.Instant
import java.time.ZoneId
import com.boompala.engine.data.EmptyClassicalTextRepository
import com.boompala.engine.data.EmptyHexagramInterpretationRepository
import com.boompala.engine.data.EmptyLineTextRepository
import com.boompala.engine.data.EmptyTarotCardRepository
import com.boompala.engine.data.HexagramInterpretationRepository
import com.boompala.engine.data.JsonClassicalTextRepository
import com.boompala.engine.data.JsonHexagramInterpretationRepository
import com.boompala.engine.data.JsonKnowledgeRepository
import com.boompala.engine.data.JsonLineTextRepository
import com.boompala.engine.data.JsonTarotCardRepository
import com.boompala.engine.data.KnowledgeArticle
import com.boompala.engine.data.LineTextRepository
import com.boompala.engine.data.TarotCardRepository
import com.boompala.engine.data.hexagramReferences
import com.boompala.engine.model.DivinationResult
import com.boompala.engine.model.HexagramInput
import com.boompala.engine.meihua.MeiHuaTimeEngine
import com.boompala.engine.meihua.MeiHuaTimeReading
import com.boompala.engine.tarot.TarotEngine
import com.boompala.engine.tarot.TarotReading
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
    TAROT_BROWSER, TAROT_CARD_DETAIL,
    DAILY_FORTUNE,
    TAROT_ONE_CARD,
    TAROT_THREE_CARD,
    TAROT_HOLY_TRIANGLE,
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
    AppScreen.ARCHIVE_TAG -> AppScreen.HOME
    AppScreen.ARCHIVE_DETAIL -> AppScreen.ARCHIVES
    AppScreen.COMPASS -> AppScreen.HOME
    AppScreen.DAILY_FORTUNE -> AppScreen.HOME
    AppScreen.TAROT_ONE_CARD -> AppScreen.HOME
    AppScreen.TAROT_THREE_CARD -> AppScreen.HOME
    AppScreen.TAROT_HOLY_TRIANGLE -> AppScreen.HOME
    AppScreen.BROWSE -> AppScreen.HOME
    AppScreen.HEXAGRAM_BROWSER -> AppScreen.BROWSE
    AppScreen.HEXAGRAM_DETAIL -> AppScreen.HEXAGRAM_BROWSER
    AppScreen.KNOWLEDGE_LIST -> AppScreen.BROWSE
    AppScreen.KNOWLEDGE_DETAIL -> AppScreen.KNOWLEDGE_LIST
    AppScreen.TAROT_BROWSER -> AppScreen.BROWSE
    AppScreen.TAROT_CARD_DETAIL -> AppScreen.TAROT_BROWSER
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
    val settingsState by settingsRepository.settings.collectAsState(initial = null)
    if (settingsState == null) return
    val settings = settingsState!!
    val screenShape = settings.resolvedScreenShape(configuration.isScreenRound)
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var reading by remember { mutableStateOf<GeneratedReading?>(null) }
    var meiHuaReading by remember { mutableStateOf<MeiHuaTimeReading?>(null) }
    var generatedMeiHuaReading by remember { mutableStateOf<GeneratedMeiHuaReading?>(null) }
    var xlrReading by remember { mutableStateOf<XiaoLiuRenReading?>(null) }
    var archiveDraft by remember { mutableStateOf<ArchiveDraft?>(null) }
    var archiveDetailId by remember { mutableStateOf<Long?>(null) }
    var archiveDetail by remember { mutableStateOf<ArchiveRecord?>(null) }
    var archiveReturnScreen by remember { mutableStateOf(AppScreen.HOME) }
    var archiveRefresh by remember { mutableIntStateOf(0) }
    var browserData by remember { mutableStateOf<BrowserData?>(null) }
    var selectedHexagram by remember { mutableStateOf<com.boompala.engine.data.HexagramReference?>(null) }
    var selectedKnowledge by remember { mutableStateOf<KnowledgeArticle?>(null) }
    var selectedTarotCard by remember { mutableStateOf<com.boompala.engine.tarot.TarotCard?>(null) }
    val archiveRepository = remember(context) { ArchiveRepository(context) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationId by remember { mutableIntStateOf(0) }
    var meiHuaGenerationId by remember { mutableIntStateOf(0) }
    val dependenciesReady = remember(context) {
        CompletableDeferred<OfflineReadingDependencies>()
    }
    val meiHuaEngine = remember { MeiHuaTimeEngine(SixTailGanzhiCalendar()) }
    val xlrEngine = remember { XiaoLiuRenEngine(SixTailGanzhiCalendar()) }
    val dailyFortuneEngineReady = remember(context) { CompletableDeferred<DailyFortuneEngine>() }
    var dailyFortuneReading by remember { mutableStateOf<DailyFortuneReading?>(null) }
    var tarotCardRepository by remember { mutableStateOf<TarotCardRepository?>(null) }
    val tarotEngine = remember(tarotCardRepository) {
        TarotEngine(tarotCardRepository ?: EmptyTarotCardRepository)
    }
    var tarotReading by remember { mutableStateOf<TarotReading?>(null) }
    var tarotThreeReading by remember { mutableStateOf<TarotReading?>(null) }
    var tarotHolyTriangleReading by remember { mutableStateOf<TarotReading?>(null) }
    LaunchedEffect(Unit) {
        if (tarotCardRepository == null) {
            val repository = withContext(Dispatchers.IO) {
                loadTarotCardRepository(context.applicationContext)
            }
            tarotCardRepository = repository
        }
    }
    LaunchedEffect(dependenciesReady) {
        val dependencies = withContext(Dispatchers.IO) {
            createOfflineReadingDependencies(context.applicationContext)
        }
        dependenciesReady.complete(dependencies)
    }
    val backDestination = screen.backDestination()

    val onSixYaoClick = remember { { screen = AppScreen.YAO_INPUT } }
    val onMeiHuaClick = remember {
        {
            meiHuaGenerationId++
            meiHuaReading = null
            generatedMeiHuaReading = null
            screen = AppScreen.MEIHUA_TIME
        }
    }
    val onSettingsClick = remember { { screen = AppScreen.SETTINGS } }
    val onXiaoLiuRenClick = remember {
        {
            xlrReading = null
            screen = AppScreen.XIAO_LIU_REN
        }
    }
    val onArchiveClick = remember { { screen = AppScreen.ARCHIVES } }
    val onCompassClick = remember { { screen = AppScreen.COMPASS } }
    val onBrowseClick = remember { { screen = AppScreen.BROWSE } }
    val onDailyFortuneClick = remember { { screen = AppScreen.DAILY_FORTUNE } }
    val onTarotClick = remember {
        {
            tarotReading = null
            screen = AppScreen.TAROT_ONE_CARD
        }
    }
    val onTarotThreeCardClick = remember {
        {
            tarotThreeReading = null
            screen = AppScreen.TAROT_THREE_CARD
        }
    }
    val onTarotHolyTriangleClick = remember {
        {
            tarotHolyTriangleReading = null
            screen = AppScreen.TAROT_HOLY_TRIANGLE
        }
    }

    BackHandler(enabled = backDestination != null) {
        generationId++
        meiHuaGenerationId++
        isGenerating = false
        if (screen == AppScreen.TAROT_ONE_CARD) {
            tarotReading = null
        }
        if (screen == AppScreen.TAROT_THREE_CARD) {
            tarotThreeReading = null
        }
        if (screen == AppScreen.TAROT_HOLY_TRIANGLE) {
            tarotHolyTriangleReading = null
        }
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
                            settings = settings,
                            onSixYaoClick = onSixYaoClick,
                            onMeiHuaClick = onMeiHuaClick,
                            onSettingsClick = onSettingsClick,
                            onXiaoLiuRenClick = onXiaoLiuRenClick,
                            onArchiveClick = onArchiveClick,
                            onCompassClick = onCompassClick,
                            onBrowseClick = onBrowseClick,
                            onDailyFortuneClick = onDailyFortuneClick,
                            onTarotClick = onTarotClick,
                            onTarotThreeCardClick = onTarotThreeCardClick,
                            onTarotHolyTriangleClick = onTarotHolyTriangleClick,
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
                            settings = settings,
                            onSixYaoClick = onSixYaoClick,
                            onMeiHuaClick = onMeiHuaClick,
                            onSettingsClick = onSettingsClick,
                            onXiaoLiuRenClick = onXiaoLiuRenClick,
                            onArchiveClick = onArchiveClick,
                            onCompassClick = onCompassClick,
                            onBrowseClick = onBrowseClick,
                            onDailyFortuneClick = onDailyFortuneClick,
                            onTarotClick = onTarotClick,
                            onTarotThreeCardClick = onTarotThreeCardClick,
                            onTarotHolyTriangleClick = onTarotHolyTriangleClick,
                        )

                        AppScreen.XIAO_LIU_REN -> XiaoLiuRenScreen(xlrEngine, xlrReading, settings.rotaryScrollingEnabled, { xlrReading = it }, { r -> archiveReturnScreen=AppScreen.XIAO_LIU_REN; archiveDraft = ArchiveDraft("", "", 0xFF4CAF50, ArchiveSource.XIAO_LIU_REN, r.timeInfo.gregorianDateTime.toInstant().toEpochMilli(), "最终${r.finalPalace.displayName}", ArchiveSnapshotCodec.encode(r)); screen = AppScreen.ARCHIVE_TAG }, { screen = AppScreen.HOME })
                        AppScreen.ARCHIVE_TAG -> archiveDraft?.let { d -> ArchiveTagScreen(d, archiveRepository, settings.rotaryScrollingEnabled, { Toast.makeText(context, context.getString(R.string.archive_save_toast), Toast.LENGTH_SHORT).show(); screen = archiveReturnScreen }, { screen = archiveReturnScreen }) }
                        AppScreen.ARCHIVES -> ArchiveListScreen(archiveRepository, settings.rotaryScrollingEnabled, archiveRefresh, { id -> archiveDetail=null; archiveDetailId=id; screen=AppScreen.ARCHIVE_DETAIL }, { screen = AppScreen.HOME })
                        AppScreen.ARCHIVE_DETAIL -> {
                            val id = archiveDetailId
                            LaunchedEffect(id) {
                                archiveDetail = withContext(Dispatchers.IO) { id?.let(archiveRepository::get) }
                            }
                            val record = archiveDetail
                            if (record == null) ArchiveLoadingScreen(settings.rotaryScrollingEnabled)
                            else ArchiveDetailScreen(record, archiveRepository, settings.rotaryScrollingEnabled, { archiveRefresh++; screen=AppScreen.ARCHIVES }, { screen=AppScreen.ARCHIVES })
                        }
                        AppScreen.COMPASS -> CompassScreen(settings.rotaryScrollingEnabled) { screen = AppScreen.HOME }
                        AppScreen.DAILY_FORTUNE -> {
                            LaunchedEffect(dailyFortuneEngineReady) {
                                if (!dailyFortuneEngineReady.isCompleted) {
                                    val engine = withContext(Dispatchers.IO) {
                                        DailyFortuneEngine(
                                            calendar = SixTailGanzhiCalendar(),
                                            almanac = SixTailDailyAlmanac(),
                                            lineTextRepository = loadLineTexts(context.applicationContext),
                                            interpretationRepository = loadHexagramInterpretations(context.applicationContext),
                                        )
                                    }
                                    dailyFortuneEngineReady.complete(engine)
                                }
                            }
                            LaunchedEffect(Unit) {
                                val now = Instant.now()
                                val zone = ZoneId.systemDefault()
                                val cached = dailyFortuneReading
                                if (cached == null || cached.date != now.atZone(zone).toLocalDate()) {
                                    val engine = dailyFortuneEngineReady.await()
                                    dailyFortuneReading = withContext(Dispatchers.Default) {
                                        engine.fortuneFor(now, zone)
                                    }
                                }
                            }
                            val currentReading = dailyFortuneReading
                            if (currentReading != null) {
                                DailyFortuneScreen(
                                    reading = currentReading,
                                    rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                                    onBack = { screen = AppScreen.HOME },
                                )
                            } else {
                                WearLoadingIndicator(label = stringResource(R.string.daily_fortune_loading))
                            }
                        }
                        AppScreen.BROWSE -> browserData?.let { d -> BrowseHomeScreen(d, settings.rotaryScrollingEnabled, { screen = AppScreen.HEXAGRAM_BROWSER }, { screen = AppScreen.KNOWLEDGE_LIST }, { screen = AppScreen.TAROT_BROWSER }, { screen = AppScreen.HOME }) }
                        AppScreen.HEXAGRAM_BROWSER -> browserData?.let { d -> HexagramBrowserScreen(d, settings.rotaryScrollingEnabled, { selectedHexagram = it; screen = AppScreen.HEXAGRAM_DETAIL }, { screen = AppScreen.BROWSE }) }
                        AppScreen.HEXAGRAM_DETAIL -> { val h = selectedHexagram; val d = browserData; if (h != null && d != null) HexagramDetailScreen(h, d, settings.rotaryScrollingEnabled) { screen = AppScreen.HEXAGRAM_BROWSER } }
                        AppScreen.KNOWLEDGE_LIST -> browserData?.let { d -> KnowledgeListScreen(d.knowledge, settings.rotaryScrollingEnabled, { selectedKnowledge = it; screen = AppScreen.KNOWLEDGE_DETAIL }, { screen = AppScreen.BROWSE }) }
                        AppScreen.KNOWLEDGE_DETAIL -> selectedKnowledge?.let { KnowledgeDetailScreen(it, settings.rotaryScrollingEnabled) { screen = AppScreen.KNOWLEDGE_LIST } }
                        AppScreen.TAROT_BROWSER -> browserData?.let { d -> TarotBrowserScreen(d.tarotCards.allCards(), settings.rotaryScrollingEnabled, { selectedTarotCard = it; screen = AppScreen.TAROT_CARD_DETAIL }, { screen = AppScreen.BROWSE }) }
                        AppScreen.TAROT_CARD_DETAIL -> selectedTarotCard?.let { c -> TarotCardDetailScreen(c, settings.rotaryScrollingEnabled) { screen = AppScreen.TAROT_BROWSER } }

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
                            onHapticFeedbackEnabledChange = { enabled ->
                                scope.launch {
                                    settingsRepository.setHapticFeedbackEnabled(enabled)
                                }
                            },
                            onLanguageSelected = { lang ->
                                scope.launch {
                                    settingsRepository.setLanguage(lang)
                                }
                            },
                            onMoveHomeFeature = { feature, up ->
                                scope.launch {
                                    settingsRepository.moveHomeFeature(feature, up)
                                }
                            },
                            onToggleHomeFeatureVisibility = { feature ->
                                scope.launch {
                                    settingsRepository.toggleHomeFeatureVisibility(feature)
                                }
                            },
                            archiveRepository = archiveRepository,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onAboutClick = { screen = AppScreen.ABOUT },
                            onBack = { screen = AppScreen.HOME },
                        )

                        AppScreen.TAROT_ONE_CARD -> TarotOneCardScreen(
                            engine = tarotEngine,
                            reading = tarotReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onReadingChanged = { tarotReading = it },
                            onBack = {
                                tarotReading = null
                                screen = AppScreen.HOME
                            },
                            onArchive = { r ->
                                archiveReturnScreen = AppScreen.TAROT_ONE_CARD
                                archiveDraft = ArchiveDraft(
                                    name = r.spread.name,
                                    note = "",
                                    color = 0xFF4CAF50L,
                                    source = ArchiveSource.TAROT,
                                    castAt = r.castAt,
                                    summary = r.drawnCards.joinToString(" · ") { "${it.card.nameZh}(${if (it.orientation == com.boompala.engine.tarot.TarotOrientation.REVERSED) "逆" else "正"})" },
                                    snapshotJson = ArchiveSnapshotCodec.encode(r),
                                )
                                screen = AppScreen.ARCHIVE_TAG
                            },
                        )

                        AppScreen.TAROT_THREE_CARD -> TarotThreeCardScreen(
                            engine = tarotEngine,
                            reading = tarotThreeReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onReadingChanged = { tarotThreeReading = it },
                            onBack = {
                                tarotThreeReading = null
                                screen = AppScreen.HOME
                            },
                            onArchive = { r ->
                                archiveReturnScreen = AppScreen.TAROT_THREE_CARD
                                archiveDraft = ArchiveDraft(
                                    name = r.spread.name,
                                    note = "",
                                    color = 0xFF4CAF50L,
                                    source = ArchiveSource.TAROT,
                                    castAt = r.castAt,
                                    summary = r.drawnCards.joinToString(" · ") { "${it.slot.name}:${it.card.nameZh}(${if (it.orientation == com.boompala.engine.tarot.TarotOrientation.REVERSED) "逆" else "正"})" },
                                    snapshotJson = ArchiveSnapshotCodec.encode(r),
                                )
                                screen = AppScreen.ARCHIVE_TAG
                            },
                        )

                        AppScreen.TAROT_HOLY_TRIANGLE -> TarotHolyTriangleScreen(
                            engine = tarotEngine,
                            reading = tarotHolyTriangleReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onReadingChanged = { tarotHolyTriangleReading = it },
                            onBack = {
                                tarotHolyTriangleReading = null
                                screen = AppScreen.HOME
                            },
                            onArchive = { r ->
                                archiveReturnScreen = AppScreen.TAROT_HOLY_TRIANGLE
                                archiveDraft = ArchiveDraft(
                                    name = r.spread.name,
                                    note = "",
                                    color = 0xFF4CAF50L,
                                    source = ArchiveSource.TAROT,
                                    castAt = r.castAt,
                                    summary = r.drawnCards.joinToString(" · ") { "${it.slot.name}:${it.card.nameZh}(${if (it.orientation == com.boompala.engine.tarot.TarotOrientation.REVERSED) "逆" else "正"})" },
                                    snapshotJson = ArchiveSnapshotCodec.encode(r),
                                )
                                screen = AppScreen.ARCHIVE_TAG
                            },
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

private fun loadTarotCardRepository(context: android.content.Context): TarotCardRepository =
    runCatching {
        context.assets.open("tarot_cards.json").bufferedReader().use(JsonTarotCardRepository::fromReader)
    }.getOrDefault(EmptyTarotCardRepository)

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
    tarotCards = loadTarotCardRepository(context),
)
