package com.boompala.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.SwipeToDismissBox
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
import com.boompala.engine.astrology.WesternAstrologyEngine
import com.boompala.engine.astrology.WesternChartReading
import com.boompala.engine.bazi.BaziEngine
import com.boompala.engine.bazi.BaziGender
import com.boompala.engine.bazi.BaziProfile
import com.boompala.engine.bone.BoneWeightEngine
import com.boompala.engine.bone.BoneWeightReading
import com.boompala.engine.ninestar.NineStarKiEngine
import com.boompala.engine.ninestar.NineStarKiReading
import com.boompala.engine.numerology.NumerologyEngine
import com.boompala.engine.numerology.NumerologyReading
import com.boompala.settings.AppSettings
import com.boompala.settings.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class AppScreen {
    WELCOME,
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
    TAROT_CELTIC_CROSS,
    DESTINY_CHART_MENU,
    BAZI_DETAIL,
    WESTERN_CHART_DETAIL,
    NUMEROLOGY_DETAIL,
    BONE_WEIGHT_DETAIL,
    NINE_STAR_DETAIL,
    PULSE_MEASURE,
    PULSE_RESULT,
}

internal fun AppScreen.backDestination(): AppScreen? = when (this) {
    AppScreen.HOME -> null
    AppScreen.WELCOME -> AppScreen.HOME
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
    AppScreen.TAROT_CELTIC_CROSS -> AppScreen.HOME
    AppScreen.DESTINY_CHART_MENU -> AppScreen.HOME
    AppScreen.BAZI_DETAIL -> AppScreen.DESTINY_CHART_MENU
    AppScreen.WESTERN_CHART_DETAIL -> AppScreen.DESTINY_CHART_MENU
    AppScreen.NUMEROLOGY_DETAIL -> AppScreen.DESTINY_CHART_MENU
    AppScreen.BONE_WEIGHT_DETAIL -> AppScreen.DESTINY_CHART_MENU
    AppScreen.NINE_STAR_DETAIL -> AppScreen.DESTINY_CHART_MENU
    AppScreen.PULSE_MEASURE -> AppScreen.HOME
    AppScreen.PULSE_RESULT -> AppScreen.HOME
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

    val activity = remember(context) {
        generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>()
            .firstOrNull()
    }
    androidx.compose.runtime.DisposableEffect(activity, settings.keepScreenOnEnabled) {
        if (settings.keepScreenOnEnabled) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val screenShape = settings.resolvedScreenShape(configuration.isScreenRound)
    val homeListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var screen by remember {
        mutableStateOf(if (!settings.hasCompletedOnboarding) AppScreen.WELCOME else AppScreen.HOME)
    }
    var welcomeReturnScreen by remember { mutableStateOf(AppScreen.HOME) }
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
    // 设置页内层分区可返回时，需屏蔽外层滑动返回手势，避免直接退回首页。
    var settingsInnerBackAvailable by remember { mutableStateOf(false) }
    var tarotCelticCrossInnerBackAvailable by remember { mutableStateOf(false) }
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
    var tarotCelticCrossReading by remember { mutableStateOf<TarotReading?>(null) }
    var currentBaziProfile by remember { mutableStateOf<BaziProfile?>(null) }
    var currentWesternReading by remember { mutableStateOf<WesternChartReading?>(null) }
    var currentNumerologyReading by remember { mutableStateOf<NumerologyReading?>(null) }
    var currentBoneWeightReading by remember { mutableStateOf<BoneWeightReading?>(null) }
    var currentNineStarReading by remember { mutableStateOf<NineStarKiReading?>(null) }
    var currentPulseResult by remember { mutableStateOf<com.boompala.engine.pulse.PulseDiagnosisResult?>(null) }
    LaunchedEffect(Unit) {
        if (tarotCardRepository == null) {
            val repository = withContext(Dispatchers.IO) {
                loadTarotCardRepository(context.applicationContext)
            }
            tarotCardRepository = repository
        }
        if (browserData == null) {
            val data = withContext(Dispatchers.IO) {
                loadBrowserData(context.applicationContext)
            }
            browserData = data
        }
    }
    LaunchedEffect(dependenciesReady) {
        val dependencies = withContext(Dispatchers.IO) {
            createOfflineReadingDependencies(context.applicationContext)
        }
        dependenciesReady.complete(dependencies)
    }
    val backDestination = screen.backDestination()

    val isFirstRunWelcome = screen == AppScreen.WELCOME && !settings.hasCompletedOnboarding
    val effectiveBackDestination = if (screen == AppScreen.WELCOME) welcomeReturnScreen else backDestination

    // 页面转场动画状态：前进推入与返回视差复位各自独立，避免互相打断。
    val forwardEnterAnimatable = remember { Animatable(1f) }
    val popEnterAnimatable = remember { Animatable(1f) }
    var lastTransitionPop by remember { mutableStateOf(false) }

    // 前进导航：动画进行中被打断时从当前进度平滑接续，否则先同步 snap 到初始位移再切屏，
    // 避免新页面首帧闪现最终状态。
    val navigateTo = remember(scope, settings) {
        { target: AppScreen ->
            if (target != screen) {
                lastTransitionPop = false
                scope.launch {
                    if (settings.animationsEnabled) {
                        // 动画进行中被打断时不重置，由 animateTo 从当前进度平滑接续。
                        if (!forwardEnterAnimatable.isRunning) {
                            forwardEnterAnimatable.snapTo(0f)
                        }
                        forwardEnterAnimatable.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 300, easing = EmphasizedDecelEasing),
                        )
                    } else {
                        forwardEnterAnimatable.snapTo(1f)
                    }
                }
                screen = target
            }
        }
    }

    // 统一返回入口：WELCOME 回到打开它的页面，ARCHIVE_TAG 回到归档来源页，其余按 backDestination 返回。
    // animate=false 用于滑动返回手势完成后的收尾（手势本身已提供退出动效）。
    val goBack = remember(scope, settings) {
        { animate: Boolean ->
            val target = when (screen) {
                AppScreen.WELCOME -> welcomeReturnScreen
                AppScreen.ARCHIVE_TAG -> archiveReturnScreen
                else -> screen.backDestination()
            }
            if (target != null && target != screen) {
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
                if (screen == AppScreen.TAROT_CELTIC_CROSS) {
                    tarotCelticCrossReading = null
                }
                lastTransitionPop = true
                scope.launch {
                    if (settings.animationsEnabled && animate) {
                        // 动画进行中被打断时不重置，由 animateTo 从当前进度平滑接续。
                        if (!popEnterAnimatable.isRunning) {
                            popEnterAnimatable.snapTo(0f)
                        }
                        popEnterAnimatable.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 300, easing = EmphasizedDecelEasing),
                        )
                    } else {
                        popEnterAnimatable.snapTo(1f)
                    }
                }
                screen = target
            }
        }
    }

    val onSixYaoClick = remember(navigateTo) { { navigateTo(AppScreen.YAO_INPUT) } }
    val onMeiHuaClick = remember(navigateTo) {
        {
            meiHuaGenerationId++
            meiHuaReading = null
            generatedMeiHuaReading = null
            navigateTo(AppScreen.MEIHUA_TIME)
        }
    }
    val onSettingsClick = remember(navigateTo) { { navigateTo(AppScreen.SETTINGS) } }
    val onXiaoLiuRenClick = remember(navigateTo) {
        {
            xlrReading = null
            navigateTo(AppScreen.XIAO_LIU_REN)
        }
    }
    val onArchiveClick = remember(navigateTo) { { navigateTo(AppScreen.ARCHIVES) } }
    val onCompassClick = remember(navigateTo) { { navigateTo(AppScreen.COMPASS) } }
    val onBrowseClick = remember(navigateTo) { { navigateTo(AppScreen.BROWSE) } }
    val onDailyFortuneClick = remember(navigateTo) { { navigateTo(AppScreen.DAILY_FORTUNE) } }
    val onTarotClick = remember(navigateTo) {
        {
            tarotReading = null
            navigateTo(AppScreen.TAROT_ONE_CARD)
        }
    }
    val onTarotThreeCardClick = remember(navigateTo) {
        {
            tarotThreeReading = null
            navigateTo(AppScreen.TAROT_THREE_CARD)
        }
    }
    val onTarotHolyTriangleClick = remember(navigateTo) {
        {
            tarotHolyTriangleReading = null
            navigateTo(AppScreen.TAROT_HOLY_TRIANGLE)
        }
    }
    val onTarotCelticCrossClick = remember(navigateTo) {
        {
            tarotCelticCrossReading = null
            navigateTo(AppScreen.TAROT_CELTIC_CROSS)
        }
    }
    val onDestinyChartClick = remember(navigateTo) {
        {
            navigateTo(AppScreen.DESTINY_CHART_MENU)
        }
    }
    val onPulseClick = remember(navigateTo) {
        {
            navigateTo(AppScreen.PULSE_MEASURE)
        }
    }

    BackHandler(enabled = !isFirstRunWelcome && effectiveBackDestination != null) {
        goBack(true)
    }

    MaterialTheme {
        WithContentScale(
            settings = settings,
            screenShape = screenShape,
        ) {
            AppScaffold {
                TopSpotlightBackground(
                    screen = screen,
                    isGenerating = isGenerating,
                    animationsEnabled = settings.animationsEnabled,
                ) {
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
                            onDestinyChartClick = onDestinyChartClick,
                            onDailyFortuneClick = onDailyFortuneClick,
                            onTarotClick = onTarotClick,
                            onTarotThreeCardClick = onTarotThreeCardClick,
                            onTarotHolyTriangleClick = onTarotHolyTriangleClick,
                            onTarotCelticCrossClick = onTarotCelticCrossClick,
                            onPulseClick = onPulseClick,
                            state = homeListState,
                        )

                        AppScreen.YAO_INPUT -> YaoInputScreen(
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            animationsEnabled = settings.animationsEnabled,
                            isGenerating = isGenerating,
                            onBack = { goBack(true) },
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
                                                navigateTo(AppScreen.RESULT)
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
                                animationsEnabled = settings.animationsEnabled,
                                onBack = { goBack(true) },
                                onArchive = { r -> archiveReturnScreen=AppScreen.RESULT; archiveDraft = ArchiveDraft("", "", 0xFF4CAF50, ArchiveSource.LIU_YAO, r.castAt.toEpochMilli(), "本卦${r.original.name}", ArchiveSnapshotCodec.encode(r, currentReading.interpretations)); navigateTo(AppScreen.ARCHIVE_TAG) },
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
                            onPulseClick = onPulseClick,
                            state = homeListState,
                        )

                        AppScreen.XIAO_LIU_REN -> XiaoLiuRenScreen(xlrEngine, xlrReading, settings.rotaryScrollingEnabled, { xlrReading = it }, { r -> archiveReturnScreen=AppScreen.XIAO_LIU_REN; archiveDraft = ArchiveDraft("", "", 0xFF4CAF50, ArchiveSource.XIAO_LIU_REN, r.timeInfo.gregorianDateTime.toInstant().toEpochMilli(), "最终${r.finalPalace.displayName}", ArchiveSnapshotCodec.encode(r)); navigateTo(AppScreen.ARCHIVE_TAG) }, { goBack(true) })
                        AppScreen.ARCHIVE_TAG -> archiveDraft?.let { d -> ArchiveTagScreen(d, archiveRepository, settings.rotaryScrollingEnabled, { Toast.makeText(context, context.getString(R.string.archive_save_toast), Toast.LENGTH_SHORT).show(); goBack(true) }, { goBack(true) }) }
                        AppScreen.ARCHIVES -> ArchiveListScreen(archiveRepository, settings.rotaryScrollingEnabled, archiveRefresh, { id -> archiveDetail=null; archiveDetailId=id; navigateTo(AppScreen.ARCHIVE_DETAIL) }, { goBack(true) })
                        AppScreen.ARCHIVE_DETAIL -> {
                            val id = archiveDetailId
                            LaunchedEffect(id) {
                                archiveDetail = withContext(Dispatchers.IO) { id?.let(archiveRepository::get) }
                            }
                            val record = archiveDetail
                            AnimatedContent(
                                targetState = record,
                                transitionSpec = { loadingContentTransitionSpec(settings.animationsEnabled) },
                                label = "ArchiveDetailLoadingTransition",
                            ) { snapshot ->
                                if (snapshot == null) ArchiveLoadingScreen(settings.rotaryScrollingEnabled)
                                else ArchiveDetailScreen(snapshot, archiveRepository, settings.rotaryScrollingEnabled, { archiveRefresh++; goBack(true) }, { goBack(true) })
                            }
                        }
                        AppScreen.COMPASS -> CompassScreen(
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            trueNorthEnabled = settings.compassTrueNorthEnabled,
                            declination = settings.compassDeclination,
                            onBack = { goBack(true) },
                        )
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
                            AnimatedContent(
                                targetState = currentReading,
                                transitionSpec = { loadingContentTransitionSpec(settings.animationsEnabled) },
                                label = "DailyFortuneLoadingTransition",
                            ) { reading ->
                                if (reading != null) {
                                    DailyFortuneScreen(
                                        reading = reading,
                                        rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                                        onBack = { goBack(true) },
                                        baziProfile = settings.resolvedBaziProfile(),
                                        animationsEnabled = settings.animationsEnabled,
                                        onConfigureBazi = {
                                            navigateTo(AppScreen.SETTINGS)
                                        },
                                    )
                                } else {
                                    WearLoadingIndicator(
                                        label = stringResource(R.string.daily_fortune_loading),
                                        animationsEnabled = settings.animationsEnabled,
                                    )
                                }
                            }
                        }
                        AppScreen.BROWSE -> {
                            val data = browserData
                            AnimatedContent(
                                targetState = data,
                                transitionSpec = { loadingContentTransitionSpec(settings.animationsEnabled) },
                                label = "BrowseHomeLoadingTransition",
                            ) { snapshot ->
                                if (snapshot != null) {
                                    BrowseHomeScreen(
                                        data = snapshot,
                                        rotary = settings.rotaryScrollingEnabled,
                                        onHexagrams = { navigateTo(AppScreen.HEXAGRAM_BROWSER) },
                                        onKnowledge = { navigateTo(AppScreen.KNOWLEDGE_LIST) },
                                        onTarot = { navigateTo(AppScreen.TAROT_BROWSER) },
                                        onBack = { goBack(true) },
                                    )
                                } else {
                                    WearLoadingIndicator(label = stringResource(R.string.browse_loading))
                                }
                            }
                        }

                        AppScreen.HEXAGRAM_BROWSER -> {
                            val data = browserData
                            if (data != null) {
                                HexagramBrowserScreen(
                                    data = data,
                                    rotary = settings.rotaryScrollingEnabled,
                                    onOpen = { selectedHexagram = it; navigateTo(AppScreen.HEXAGRAM_DETAIL) },
                                    onBack = { goBack(true) },
                                )
                            } else {
                                WearLoadingIndicator(label = stringResource(R.string.browse_loading))
                            }
                        }

                        AppScreen.HEXAGRAM_DETAIL -> {
                            val h = selectedHexagram
                            val d = browserData
                            if (h != null && d != null) {
                                HexagramDetailScreen(
                                    hex = h,
                                    data = d,
                                    rotary = settings.rotaryScrollingEnabled,
                                    animationsEnabled = settings.animationsEnabled,
                                    onBack = { goBack(true) },
                                )
                            } else {
                                WearLoadingIndicator(label = stringResource(R.string.browse_loading))
                            }
                        }

                        AppScreen.KNOWLEDGE_LIST -> {
                            val d = browserData
                            if (d != null) {
                                KnowledgeListScreen(
                                    articles = d.knowledge,
                                    rotary = settings.rotaryScrollingEnabled,
                                    onOpen = { selectedKnowledge = it; navigateTo(AppScreen.KNOWLEDGE_DETAIL) },
                                    onBack = { goBack(true) },
                                )
                            } else {
                                WearLoadingIndicator(label = stringResource(R.string.browse_loading))
                            }
                        }

                        AppScreen.KNOWLEDGE_DETAIL -> {
                            val article = selectedKnowledge
                            if (article != null) {
                                KnowledgeDetailScreen(
                                    article = article,
                                    rotary = settings.rotaryScrollingEnabled,
                                    animationsEnabled = settings.animationsEnabled,
                                    onBack = { goBack(true) },
                                )
                            } else {
                                WearLoadingIndicator(label = stringResource(R.string.browse_loading))
                            }
                        }

                        AppScreen.TAROT_BROWSER -> {
                            val d = browserData
                            if (d != null) {
                                TarotBrowserScreen(
                                    cards = d.tarotCards.allCards(),
                                    rotary = settings.rotaryScrollingEnabled,
                                    onOpen = { selectedTarotCard = it; navigateTo(AppScreen.TAROT_CARD_DETAIL) },
                                    onBack = { goBack(true) },
                                )
                            } else {
                                WearLoadingIndicator(label = stringResource(R.string.browse_loading))
                            }
                        }

                        AppScreen.TAROT_CARD_DETAIL -> {
                            val card = selectedTarotCard
                            if (card != null) {
                                TarotCardDetailScreen(
                                    card = card,
                                    rotary = settings.rotaryScrollingEnabled,
                                    animationsEnabled = settings.animationsEnabled,
                                    onBack = { goBack(true) },
                                )
                            } else {
                                WearLoadingIndicator(label = stringResource(R.string.browse_loading))
                            }
                        }

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
                                        navigateTo(AppScreen.MEIHUA_RESULT)
                                    }
                                }
                            },
                            onBack = { goBack(true) },
                        )

                        AppScreen.MEIHUA_RESULT -> generatedMeiHuaReading?.let { currentReading ->
                            MeiHuaResultContent(
                                reading = currentReading.reading,
                                interpretations = currentReading.interpretations,
                                rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                                onBack = { goBack(true) },
                                onArchive = { r -> archiveReturnScreen=AppScreen.MEIHUA_RESULT; archiveDraft = ArchiveDraft("", "", 0xFF4CAF50, ArchiveSource.MEI_HUA, r.timeInfo.gregorianDateTime.toInstant().toEpochMilli(), "本卦${r.original.name}", ArchiveSnapshotCodec.encode(r, currentReading.interpretations)); navigateTo(AppScreen.ARCHIVE_TAG) },
                            )
                        } ?: MeiHuaTimeScreen(
                            engine = meiHuaEngine,
                            initialReading = meiHuaReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onReadingChanged = { meiHuaReading = it },
                            onViewReading = { },
                            onBack = { goBack(true) },
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
                            onHapticIntensityChange = { intensity ->
                                scope.launch {
                                    settingsRepository.setHapticIntensity(intensity)
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
                            onSaveUserBirth = { birthDate, birthHour, gender ->
                                scope.launch {
                                    settingsRepository.setUserBirth(birthDate, birthHour, gender)
                                }
                            },
                            onClearUserBirth = {
                                scope.launch {
                                    settingsRepository.clearUserBirth()
                                }
                            },
                            onKeepScreenOnEnabledChange = { enabled ->
                                scope.launch { settingsRepository.setKeepScreenOnEnabled(enabled) }
                            },
                            onCompassTrueNorthEnabledChange = { enabled ->
                                scope.launch { settingsRepository.setCompassTrueNorthEnabled(enabled) }
                            },
                            onCompassDeclinationChange = { declination ->
                                scope.launch { settingsRepository.setCompassDeclination(declination) }
                            },
                            onTarotReversedEnabledChange = { enabled ->
                                scope.launch { settingsRepository.setTarotReversedEnabled(enabled) }
                            },
                            onTarotMajorArcanaOnlyChange = { enabled ->
                                scope.launch { settingsRepository.setTarotMajorArcanaOnly(enabled) }
                            },
                            onResetAllPreferences = {
                                scope.launch { settingsRepository.resetAllPreferences() }
                            },
                            archiveRepository = archiveRepository,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onAboutClick = { navigateTo(AppScreen.ABOUT) },
                            onBack = { goBack(true) },
                            onInnerBackAvailabilityChanged = { settingsInnerBackAvailable = it },
                        )

                        AppScreen.TAROT_ONE_CARD -> TarotOneCardScreen(
                            engine = tarotEngine,
                            reading = tarotReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            animationsEnabled = settings.animationsEnabled,
                            allowReversed = settings.tarotReversedEnabled,
                            deckType = if (settings.tarotMajorArcanaOnly) com.boompala.engine.tarot.DeckType.MAJOR_22 else com.boompala.engine.tarot.DeckType.FULL_78,
                            onReadingChanged = { tarotReading = it },
                            onBack = { goBack(true) },
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
                                navigateTo(AppScreen.ARCHIVE_TAG)
                            },
                        )

                        AppScreen.TAROT_THREE_CARD -> TarotThreeCardScreen(
                            engine = tarotEngine,
                            reading = tarotThreeReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            animationsEnabled = settings.animationsEnabled,
                            allowReversed = settings.tarotReversedEnabled,
                            deckType = if (settings.tarotMajorArcanaOnly) com.boompala.engine.tarot.DeckType.MAJOR_22 else com.boompala.engine.tarot.DeckType.FULL_78,
                            onReadingChanged = { tarotThreeReading = it },
                            onBack = { goBack(true) },
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
                                navigateTo(AppScreen.ARCHIVE_TAG)
                            },
                        )

                        AppScreen.TAROT_HOLY_TRIANGLE -> TarotHolyTriangleScreen(
                            engine = tarotEngine,
                            reading = tarotHolyTriangleReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            animationsEnabled = settings.animationsEnabled,
                            allowReversed = settings.tarotReversedEnabled,
                            deckType = if (settings.tarotMajorArcanaOnly) com.boompala.engine.tarot.DeckType.MAJOR_22 else com.boompala.engine.tarot.DeckType.FULL_78,
                            onReadingChanged = { tarotHolyTriangleReading = it },
                            onBack = { goBack(true) },
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
                                navigateTo(AppScreen.ARCHIVE_TAG)
                            },
                        )

                        AppScreen.TAROT_CELTIC_CROSS -> TarotCelticCrossScreen(
                            engine = tarotEngine,
                            reading = tarotCelticCrossReading,
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            animationsEnabled = settings.animationsEnabled,
                            allowReversed = settings.tarotReversedEnabled,
                            deckType = if (settings.tarotMajorArcanaOnly) com.boompala.engine.tarot.DeckType.MAJOR_22 else com.boompala.engine.tarot.DeckType.FULL_78,
                            onReadingChanged = { tarotCelticCrossReading = it },
                            onBack = { goBack(true) },
                            onInnerBackAvailabilityChanged = { tarotCelticCrossInnerBackAvailable = it },
                            onArchive = { r ->
                                archiveReturnScreen = AppScreen.TAROT_CELTIC_CROSS
                                archiveDraft = ArchiveDraft(
                                    name = r.spread.name,
                                    note = "",
                                    color = 0xFF4CAF50L,
                                    source = ArchiveSource.TAROT,
                                    castAt = r.castAt,
                                    summary = r.drawnCards.joinToString(" · ") { "${it.slot.name}:${it.card.nameZh}(${if (it.orientation == com.boompala.engine.tarot.TarotOrientation.REVERSED) "逆" else "正"})" },
                                    snapshotJson = ArchiveSnapshotCodec.encode(r),
                                )
                                navigateTo(AppScreen.ARCHIVE_TAG)
                            },
                        )

                        AppScreen.WELCOME -> WelcomeScreen(
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            animationsEnabled = settings.animationsEnabled,
                            onFinish = {
                                if (!settings.hasCompletedOnboarding) {
                                    scope.launch {
                                        settingsRepository.setOnboardingCompleted(true)
                                    }
                                }
                                navigateTo(welcomeReturnScreen)
                            },
                            onBack = if (welcomeReturnScreen != AppScreen.HOME) {
                                { goBack(true) }
                            } else null,
                        )

                        AppScreen.DESTINY_CHART_MENU -> DestinyChartMenuScreen(
                            settings = settings,
                            onNavigateToBazi = { date, hour, gender ->
                                currentBaziProfile = BaziEngine.calculate(date, hour, gender)
                                navigateTo(AppScreen.BAZI_DETAIL)
                            },
                            onNavigateToWestern = { date, hour ->
                                currentWesternReading = WesternAstrologyEngine.calculate(date, hour)
                                navigateTo(AppScreen.WESTERN_CHART_DETAIL)
                            },
                            onNavigateToNumerology = { date ->
                                currentNumerologyReading = NumerologyEngine.calculate(date)
                                navigateTo(AppScreen.NUMEROLOGY_DETAIL)
                            },
                            onNavigateToBoneWeight = { date, hour ->
                                currentBoneWeightReading = BoneWeightEngine.calculate(date, hour)
                                navigateTo(AppScreen.BONE_WEIGHT_DETAIL)
                            },
                            onNavigateToNineStar = { date ->
                                currentNineStarReading = NineStarKiEngine.calculate(date)
                                navigateTo(AppScreen.NINE_STAR_DETAIL)
                            },
                            onBack = { goBack(true) },
                        )

                        AppScreen.BAZI_DETAIL -> currentBaziProfile?.let { profile ->
                            BaziDetailScreen(
                                profile = profile,
                                rotaryEnabled = settings.rotaryScrollingEnabled,
                                animationsEnabled = settings.animationsEnabled,
                                onBack = { goBack(true) },
                            )
                        } ?: run {
                            DestinyChartMenuScreen(
                                settings = settings,
                                onNavigateToBazi = { date, hour, gender ->
                                    currentBaziProfile = BaziEngine.calculate(date, hour, gender)
                                    navigateTo(AppScreen.BAZI_DETAIL)
                                },
                                onNavigateToWestern = { date, hour ->
                                    currentWesternReading = WesternAstrologyEngine.calculate(date, hour)
                                    navigateTo(AppScreen.WESTERN_CHART_DETAIL)
                                },
                                onNavigateToNumerology = { date ->
                                    currentNumerologyReading = NumerologyEngine.calculate(date)
                                    navigateTo(AppScreen.NUMEROLOGY_DETAIL)
                                },
                                onNavigateToBoneWeight = { date, hour ->
                                    currentBoneWeightReading = BoneWeightEngine.calculate(date, hour)
                                    navigateTo(AppScreen.BONE_WEIGHT_DETAIL)
                                },
                                onNavigateToNineStar = { date ->
                                    currentNineStarReading = NineStarKiEngine.calculate(date)
                                    navigateTo(AppScreen.NINE_STAR_DETAIL)
                                },
                                onBack = { goBack(true) },
                            )
                        }

                        AppScreen.WESTERN_CHART_DETAIL -> currentWesternReading?.let { reading ->
                            WesternChartScreen(
                                reading = reading,
                                rotaryEnabled = settings.rotaryScrollingEnabled,
                                animationsEnabled = settings.animationsEnabled,
                                onBack = { goBack(true) },
                            )
                        } ?: run {
                            DestinyChartMenuScreen(
                                settings = settings,
                                onNavigateToBazi = { date, hour, gender ->
                                    currentBaziProfile = BaziEngine.calculate(date, hour, gender)
                                    navigateTo(AppScreen.BAZI_DETAIL)
                                },
                                onNavigateToWestern = { date, hour ->
                                    currentWesternReading = WesternAstrologyEngine.calculate(date, hour)
                                    navigateTo(AppScreen.WESTERN_CHART_DETAIL)
                                },
                                onNavigateToNumerology = { date ->
                                    currentNumerologyReading = NumerologyEngine.calculate(date)
                                    navigateTo(AppScreen.NUMEROLOGY_DETAIL)
                                },
                                onNavigateToBoneWeight = { date, hour ->
                                    currentBoneWeightReading = BoneWeightEngine.calculate(date, hour)
                                    navigateTo(AppScreen.BONE_WEIGHT_DETAIL)
                                },
                                onNavigateToNineStar = { date ->
                                    currentNineStarReading = NineStarKiEngine.calculate(date)
                                    navigateTo(AppScreen.NINE_STAR_DETAIL)
                                },
                                onBack = { goBack(true) },
                            )
                        }

                        AppScreen.NUMEROLOGY_DETAIL -> currentNumerologyReading?.let { reading ->
                            NumerologyDetailScreen(
                                reading = reading,
                                rotaryEnabled = settings.rotaryScrollingEnabled,
                                animationsEnabled = settings.animationsEnabled,
                                onBack = { goBack(true) },
                            )
                        } ?: run {
                            DestinyChartMenuScreen(
                                settings = settings,
                                onNavigateToBazi = { date, hour, gender ->
                                    currentBaziProfile = BaziEngine.calculate(date, hour, gender)
                                    navigateTo(AppScreen.BAZI_DETAIL)
                                },
                                onNavigateToWestern = { date, hour ->
                                    currentWesternReading = WesternAstrologyEngine.calculate(date, hour)
                                    navigateTo(AppScreen.WESTERN_CHART_DETAIL)
                                },
                                onNavigateToNumerology = { date ->
                                    currentNumerologyReading = NumerologyEngine.calculate(date)
                                    navigateTo(AppScreen.NUMEROLOGY_DETAIL)
                                },
                                onNavigateToBoneWeight = { date, hour ->
                                    currentBoneWeightReading = BoneWeightEngine.calculate(date, hour)
                                    navigateTo(AppScreen.BONE_WEIGHT_DETAIL)
                                },
                                onNavigateToNineStar = { date ->
                                    currentNineStarReading = NineStarKiEngine.calculate(date)
                                    navigateTo(AppScreen.NINE_STAR_DETAIL)
                                },
                                onBack = { goBack(true) },
                            )
                        }

                        AppScreen.BONE_WEIGHT_DETAIL -> currentBoneWeightReading?.let { reading ->
                            BoneWeightDetailScreen(
                                reading = reading,
                                rotaryEnabled = settings.rotaryScrollingEnabled,
                                animationsEnabled = settings.animationsEnabled,
                                onBack = { goBack(true) },
                            )
                        } ?: run {
                            DestinyChartMenuScreen(
                                settings = settings,
                                onNavigateToBazi = { date, hour, gender ->
                                    currentBaziProfile = BaziEngine.calculate(date, hour, gender)
                                    navigateTo(AppScreen.BAZI_DETAIL)
                                },
                                onNavigateToWestern = { date, hour ->
                                    currentWesternReading = WesternAstrologyEngine.calculate(date, hour)
                                    navigateTo(AppScreen.WESTERN_CHART_DETAIL)
                                },
                                onNavigateToNumerology = { date ->
                                    currentNumerologyReading = NumerologyEngine.calculate(date)
                                    navigateTo(AppScreen.NUMEROLOGY_DETAIL)
                                },
                                onNavigateToBoneWeight = { date, hour ->
                                    currentBoneWeightReading = BoneWeightEngine.calculate(date, hour)
                                    navigateTo(AppScreen.BONE_WEIGHT_DETAIL)
                                },
                                onNavigateToNineStar = { date ->
                                    currentNineStarReading = NineStarKiEngine.calculate(date)
                                    navigateTo(AppScreen.NINE_STAR_DETAIL)
                                },
                                onBack = { goBack(true) },
                            )
                        }

                        AppScreen.NINE_STAR_DETAIL -> currentNineStarReading?.let { reading ->
                            NineStarDetailScreen(
                                reading = reading,
                                rotaryEnabled = settings.rotaryScrollingEnabled,
                                animationsEnabled = settings.animationsEnabled,
                                onBack = { goBack(true) },
                            )
                        } ?: run {
                            DestinyChartMenuScreen(
                                settings = settings,
                                onNavigateToBazi = { date, hour, gender ->
                                    currentBaziProfile = BaziEngine.calculate(date, hour, gender)
                                    navigateTo(AppScreen.BAZI_DETAIL)
                                },
                                onNavigateToWestern = { date, hour ->
                                    currentWesternReading = WesternAstrologyEngine.calculate(date, hour)
                                    navigateTo(AppScreen.WESTERN_CHART_DETAIL)
                                },
                                onNavigateToNumerology = { date ->
                                    currentNumerologyReading = NumerologyEngine.calculate(date)
                                    navigateTo(AppScreen.NUMEROLOGY_DETAIL)
                                },
                                onNavigateToBoneWeight = { date, hour ->
                                    currentBoneWeightReading = BoneWeightEngine.calculate(date, hour)
                                    navigateTo(AppScreen.BONE_WEIGHT_DETAIL)
                                },
                                onNavigateToNineStar = { date ->
                                    currentNineStarReading = NineStarKiEngine.calculate(date)
                                    navigateTo(AppScreen.NINE_STAR_DETAIL)
                                },
                                onBack = { goBack(true) },
                            )
                        }

                        AppScreen.ABOUT -> AboutScreen(
                            rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
                            onViewWelcomeClick = {
                                welcomeReturnScreen = AppScreen.ABOUT
                                navigateTo(AppScreen.WELCOME)
                            },
                            onBack = { goBack(true) },
                        )

                        AppScreen.PULSE_MEASURE -> com.boompala.ui.pulse.PulseMeasureScreen(
                            onResult = { result ->
                                currentPulseResult = result
                                navigateTo(AppScreen.PULSE_RESULT)
                            },
                            onBack = { goBack(true) },
                        )

                        AppScreen.PULSE_RESULT -> currentPulseResult?.let { result ->
                            com.boompala.ui.pulse.PulseResultScreen(
                                result = result,
                                rotaryEnabled = settings.rotaryScrollingEnabled,
                                onSaveArchive = {
                                    archiveReturnScreen = AppScreen.PULSE_RESULT
                                    archiveDraft = ArchiveDraft(
                                        name = "脉象 · ${result.category.chineseName}",
                                        note = "${result.meridianInfo.earthlyBranch} ${result.category.classicPhrase}",
                                        color = 0xFF00E5A3L,
                                        source = ArchiveSource.PULSE,
                                        castAt = result.timestampMillis,
                                        summary = "【${result.category.chineseName}】${result.category.natureSummary}",
                                        snapshotJson = ArchiveSnapshotCodec.encode(result),
                                    )
                                    navigateTo(AppScreen.ARCHIVE_TAG)
                                },
                                onBack = { goBack(true) },
                            )
                        } ?: run {
                            HomeScreen(
                                settings = settings,
                                onSixYaoClick = onSixYaoClick,
                                onMeiHuaClick = onMeiHuaClick,
                                onSettingsClick = onSettingsClick,
                                onXiaoLiuRenClick = onXiaoLiuRenClick,
                                onArchiveClick = onArchiveClick,
                                onCompassClick = onCompassClick,
                                onBrowseClick = onBrowseClick,
                                onDestinyChartClick = onDestinyChartClick,
                                onDailyFortuneClick = onDailyFortuneClick,
                                onTarotClick = onTarotClick,
                                onTarotThreeCardClick = onTarotThreeCardClick,
                                onTarotHolyTriangleClick = onTarotHolyTriangleClick,
                                onTarotCelticCrossClick = onTarotCelticCrossClick,
                                onPulseClick = onPulseClick,
                                state = homeListState,
                            )
                        }
                    }
                }

                val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
                val swipeEnabled = !isFirstRunWelcome &&
                    effectiveBackDestination != null &&
                    !settingsInnerBackAvailable &&
                    !tarotCelticCrossInnerBackAvailable
                val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

                CompositionLocalProvider(
                    LocalHapticFeedbackEnabled provides settings.hapticFeedbackEnabled,
                    LocalHapticIntensity provides settings.hapticIntensity,
                ) {
                    SwipeToDismissBox(
                        state = swipeToDismissBoxState,
                        modifier = Modifier.fillMaxSize(),
                        userSwipeEnabled = swipeEnabled,
                        backgroundKey = effectiveBackDestination ?: AppScreen.HOME,
                        contentKey = screen,
                        backgroundScrimColor = Color.Transparent,
                        contentScrimColor = Color.Transparent,
                        onDismissed = { goBack(false) },
                    ) { isBackground ->
                        if (isBackground) {
                            val bgScreen = effectiveBackDestination ?: AppScreen.HOME
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (forwardEnterAnimatable.value < 1f) {
                                            val progress = forwardEnterAnimatable.value
                                            translationX = -0.20f * screenWidthPx * (1f - progress)
                                            // 背景压暗幅度收窄，减少整屏亮度拖影闪烁。
                                            alpha = 0.75f + 0.25f * progress
                                        }
                                    }
                            ) {
                                screenContent(bgScreen)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (lastTransitionPop) {
                                            // 返回：父页从左侧滑入复位，与前进的右滑入方向成镜像。
                                            if (popEnterAnimatable.value < 1f) {
                                                val progress = popEnterAnimatable.value
                                                translationX = -0.30f * screenWidthPx * (1f - progress)
                                                // 淡入先于位移完成，避免半透明幽灵感。
                                                alpha = (progress * 2f).coerceAtMost(1f)
                                            }
                                        } else if (forwardEnterAnimatable.value < 1f) {
                                            val progress = forwardEnterAnimatable.value
                                            translationX = screenWidthPx * (1f - progress)
                                            // 淡入先于位移完成，避免半透明幽灵感。
                                            alpha = (progress * 2f).coerceAtMost(1f)
                                        }
                                    }
                            ) {
                                screenContent(screen)
                            }
                        }
                    }
                }
            }
        }
    }
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
