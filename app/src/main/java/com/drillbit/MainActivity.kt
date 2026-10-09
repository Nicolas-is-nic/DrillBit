package com.drillbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.drillbit.ServiceLocator
import com.drillbit.ui.banks.BankDetailEvent
import com.drillbit.ui.banks.BankDetailViewModel
import com.drillbit.ui.banks.BankDetailScreen
import com.drillbit.ui.banks.BankDetailUiState
import com.drillbit.ui.banks.BankListEvent
import com.drillbit.ui.banks.BankListViewModel
import com.drillbit.ui.banks.BankListScreen
import com.drillbit.ui.banks.BankListUiState
import com.drillbit.ui.chat.ChatEvent
import com.drillbit.ui.chat.ChatScreen
import com.drillbit.ui.chat.ChatViewModel
import com.drillbit.ui.components.CrashDialog
import com.drillbit.ui.components.CrashDialogEvent
import com.drillbit.ui.components.CrashDialogUi
import com.drillbit.ui.components.DbBottomNavBar
import com.drillbit.ui.components.dbTabs
import com.drillbit.ui.favorite.FavoriteListEvent
import com.drillbit.ui.favorite.FavoriteListScreen
import com.drillbit.ui.favorite.FavoriteListViewModel
import com.drillbit.ui.notes.BackupEvent
import com.drillbit.ui.notes.BackupScreen
import com.drillbit.ui.notes.BackupViewModel
import com.drillbit.ui.notes.DigestEvent
import com.drillbit.ui.notes.DigestScreen
import com.drillbit.ui.notes.DigestViewModel
import com.drillbit.ui.notes.NoteEditEvent
import com.drillbit.ui.notes.NoteEditScreen
import com.drillbit.ui.notes.NoteEditViewModel
import com.drillbit.ui.notes.NoteListEvent
import com.drillbit.ui.notes.NoteListScreen
import com.drillbit.ui.notes.NoteListViewModel
import com.drillbit.ui.quiz.MixConfigEvent
import com.drillbit.ui.quiz.MixConfigScreen
import com.drillbit.ui.quiz.MixConfigUiState
import com.drillbit.ui.quiz.MixConfigViewModel
import com.drillbit.ui.quiz.QuizViewModel
import com.drillbit.ui.quiz.QuestionUi
import com.drillbit.ui.quiz.QuizEvent
import com.drillbit.ui.quiz.QuizMode
import com.drillbit.ui.quiz.QuizPhase
import com.drillbit.ui.quiz.QuizScreen
import com.drillbit.ui.quiz.QuizUiState
import com.drillbit.ui.settings.ApiType
import com.drillbit.ui.settings.ModelConfigEvent
import com.drillbit.ui.settings.ModelConfigScreen
import com.drillbit.ui.settings.ModelConfigViewModel
import com.drillbit.ui.settings.ServerConfigEvent
import com.drillbit.ui.settings.ServerConfigScreen
import com.drillbit.ui.settings.ServerConfigViewModel

import com.drillbit.ui.settings.AccountEvent
import com.drillbit.ui.settings.AccountScreen
import com.drillbit.ui.settings.AccountViewModel
import com.drillbit.ui.settings.SettingsEvent
import com.drillbit.ui.settings.SettingsScreen
import com.drillbit.ui.settings.SettingsViewModel

import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors
import com.drillbit.ui.wrong.WrongListEvent
import com.drillbit.ui.wrong.WrongListScreen
import com.drillbit.ui.wrong.WrongListViewModel

/** 底部导航常驻的 Tab 路由集合 */
private val tabRoutes = dbTabs.map { it.route }.toSet()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 上次运行若崩溃，弹窗展示完整堆栈（无 adb 环境下定位闪退）
        val crashText = runCatching {
            val f = DrillBitApplication.crashFile(this)
            if (f.exists()) f.readText().also { f.delete() } else null
        }.getOrNull()
        setContent {
            // 深色模式归属（已拍板）：DataStore 唯一存储，启动后收集并持有，向下单向传递
            var darkTheme by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                ServiceLocator.settingsStore.settings.collect { darkTheme = it.darkMode }
            }
            // 冷启动云同步静默拉取（B5）：已登录且云端快照比本地新才导入，失败静默不打扰
            LaunchedEffect(Unit) {
                runCatching {
                    val s = ServiceLocator.settingsStore.snapshot()
                    if (s.serverUrl.isNotBlank() && s.authToken.isNotBlank()) {
                        val meta = ServiceLocator.syncRepository.meta() ?: return@runCatching
                        if (meta.uploadedAt > s.lastSyncAt) {
                            ServiceLocator.syncRepository.downloadAndImport()
                        }
                    }
                }
            }
            val onDarkModeChange: (Boolean) -> Unit = { on ->
                darkTheme = on
                lifecycleScope.launch { runCatching { ServiceLocator.settingsStore.setDarkMode(on) } } // review F-26
            }
            // 崩溃文本读后暂存（设置页「有崩溃日志」与重新查看入口用）
            DrillBitApplication.lastCrashLog = crashText
            var crashVisible by remember { mutableStateOf(!crashText.isNullOrBlank()) }
            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            DrillBitTheme(darkTheme = darkTheme) {
            val bgColor = dbColors().bg
            // 根部全屏底色：画满整屏（含系统栏区域），杜绝 App 内切深色后边缘露白
            Box(Modifier.fillMaxSize().background(bgColor)) {
                DrillBitApp(
                    darkTheme = darkTheme,
                    onDarkModeChange = onDarkModeChange,
                    onShowCrashLog = { crashVisible = true },
                )
                if (crashVisible) {
                    CrashDialog(
                        state = parseCrash(crashText),
                        onEvent = { event ->
                            when (event) {
                                CrashDialogEvent.Copy -> clipboard.setText(
                                    androidx.compose.ui.text.AnnotatedString(crashText.orEmpty()),
                                )

                                CrashDialogEvent.Close -> crashVisible = false
                            }
                        },
                    )
                }
            }
        }
        }
    }
}

/**
 * 应用根组件：底部导航（仅 Tab 页显示）+ 13 条路由到正式页面。
 *
 * 阶段 1 无数据层，各页以空态 state 渲染；跳转类事件在此接线，其余事件留空实现待阶段 2 接 ViewModel。
 */
@Composable
fun DrillBitApp(
    darkTheme: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onShowCrashLog: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val colors = dbColors()

    Scaffold(
        containerColor = colors.bg,
        bottomBar = {
            // 仅四个 Tab 页显示底部导航，二级页全屏（对照页面设计稿）
            if (currentRoute in tabRoutes) {
                DbBottomNavBar(
                    currentRoute = currentRoute,
                    onTabClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "banks",
            modifier = Modifier.padding(innerPadding),
        ) {
            // ===== 四 Tab 主页 =====
            composable("banks") {
                val vm: BankListViewModel = viewModel()
                val state by vm.state.collectAsState()
                BankListScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            is BankListEvent.BankClick ->
                                navController.navigate("bankDetail/${event.bankId}")

                            is BankListEvent.MixClick ->
                                navController.navigate("mixConfig?category=${event.category}")
                            is BankListEvent.CategoryChange -> vm.onEvent(event)
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable("wrong") {
                val vm: WrongListViewModel = viewModel()
                val state by vm.state.collectAsState()
                WrongListScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            WrongListEvent.RetryAll -> vm.startRetry { ok ->
                                if (ok) navController.navigate("quiz?mode=retry")
                            }
                            WrongListEvent.FavoriteEntryClick -> navController.navigate("favorites")
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable("favorites") {
                val vm: FavoriteListViewModel = viewModel()
                val state by vm.state.collectAsState()
                FavoriteListScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            FavoriteListEvent.Back -> navController.popBackStack()
                            FavoriteListEvent.StartQuiz -> vm.startQuiz { ok ->
                                if (ok) navController.navigate("quiz?mode=favorite")
                            }
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable("notes") {
                val vm: NoteListViewModel = viewModel()
                val state by vm.state.collectAsState()
                NoteListScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            is NoteListEvent.NoteClick ->
                                navController.navigate("noteEdit/${event.noteId}")

                            NoteListEvent.NewNote -> navController.navigate("noteEdit/new")
                            NoteListEvent.Summarize -> navController.navigate("digest")
                            NoteListEvent.Backup -> navController.navigate("backup")
                        }
                    },
                )
            }
            composable("settings") {
                val vm: SettingsViewModel = viewModel()
                val state by vm.state.collectAsState()
                SettingsScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            is SettingsEvent.DarkModeChange -> onDarkModeChange(event.on)
                            SettingsEvent.ServerClick -> navController.navigate("serverConfig")
                            SettingsEvent.ModelClick -> navController.navigate("modelConfig")
                            SettingsEvent.BackupClick -> navController.navigate("backup")
                            SettingsEvent.CrashLogClick -> onShowCrashLog()
                            SettingsEvent.CheckUpdate -> vm.onEvent(event)
                            SettingsEvent.AccountClick -> navController.navigate("account")
                        }
                    },
                )
            }

            // ===== 二级页 =====
            composable("bankDetail/{bankId}") { entry ->
                val bankId = entry.arguments?.getString("bankId").orEmpty()
                val vm: BankDetailViewModel = viewModel(
                    key = "bankDetail/$bankId",
                    factory = BankDetailViewModel.Factory(bankId),
                )
                val state by vm.state.collectAsState()
                val scope = rememberCoroutineScope()
                BankDetailScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            BankDetailEvent.ContinueClick ->
                                navController.navigate("quiz?mode=single&bankId=$bankId")

                            BankDetailEvent.RestartClick -> scope.launch {
                                // 从头重刷：等断点清零落库后再进刷题页，避免读到旧断点只剩末题
                                vm.restart()
                                navController.navigate("quiz?mode=single&bankId=$bankId")
                            }

                            is BankDetailEvent.TierStartClick -> scope.launch {
                                if (vm.startTier(event.tier)) {
                                    navController.navigate("quiz?mode=tier")
                                }
                            }
                            BankDetailEvent.Back -> navController.popBackStack()
                            BankDetailEvent.DeleteConfirm -> {
                                vm.onEvent(event)
                                navController.popBackStack()
                            }
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable(
                route = "mixConfig?category={category}",
                arguments = listOf(navArgument("category") { defaultValue = "knowledge" }),
            ) { entry ->
                val mixCategory = entry.arguments?.getString("category") ?: "knowledge"
                val vm: MixConfigViewModel = viewModel(factory = MixConfigViewModel.Factory(mixCategory))
                val state by vm.state.collectAsState()
                val scope = rememberCoroutineScope()
                MixConfigScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            MixConfigEvent.Start -> scope.launch {
                                // 建卷成功才进入刷题页（未勾选时不导航）
                                if (vm.buildSession()) {
                                    navController.navigate("quiz?mode=mix")
                                }
                            }
                            MixConfigEvent.Back -> navController.popBackStack()
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable(
                route = "quiz?mode={mode}&bankId={bankId}",
                arguments = listOf(
                    navArgument("mode") { defaultValue = "single" },
                    navArgument("bankId") { defaultValue = "" },
                ),
            ) { entry ->
                val mode = when (entry.arguments?.getString("mode")) {
                    "mix" -> QuizMode.MIX
                    "retry" -> QuizMode.RETRY
                    "favorite" -> QuizMode.FAVORITE
                    "tier" -> QuizMode.TIER
                    else -> QuizMode.SINGLE
                }
                val bankId = entry.arguments?.getString("bankId").orEmpty()
                val vm: QuizViewModel = viewModel(
                    key = "quiz/$mode/$bankId",
                    factory = QuizViewModel.Factory(mode, bankId),
                )
                val state by vm.state.collectAsState()
                QuizScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            QuizEvent.AskAi -> navController.navigate("chat?questionId=${vm.currentQuestionId()}")
                            QuizEvent.Back -> navController.popBackStack()
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable("noteEdit/{noteId}") { entry ->
                val noteId = entry.arguments?.getString("noteId").orEmpty()
                val vm: NoteEditViewModel = viewModel(
                    key = "noteEdit/$noteId",
                    factory = NoteEditViewModel.Factory(noteId),
                )
                val state by vm.state.collectAsState()
                val noteScope = rememberCoroutineScope()
                NoteEditScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            NoteEditEvent.Back -> navController.popBackStack()
                            NoteEditEvent.Save -> noteScope.launch {
                                vm.save()          // 等落库再退栈（review F-5：竞态曾静默丢笔记）
                                navController.popBackStack()
                            }
                            NoteEditEvent.DeleteConfirm -> noteScope.launch {
                                vm.delete()
                                navController.popBackStack()
                            }
                            NoteEditEvent.SourceClick -> {
                                vm.sourceBankId()?.let { bankId ->
                                    navController.navigate("quiz?mode=single&bankId=$bankId")
                                }
                            }
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable("digest") {
                val vm: DigestViewModel = viewModel()
                val state by vm.state.collectAsState()
                DigestScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            DigestEvent.Back -> navController.popBackStack()
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable("account") {
                val vm: AccountViewModel = viewModel()
                val state by vm.state.collectAsState()
                AccountScreen(
                    state = state,
                    onEvent = { event ->
                        if (event is AccountEvent.Back) {
                            navController.popBackStack()
                        } else {
                            vm.onEvent(event)
                        }
                    },
                )
            }
            composable("backup") {
                val vm: BackupViewModel = viewModel()
                val state by vm.state.collectAsState()
                BackupScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            BackupEvent.Back -> navController.popBackStack()
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable(
                route = "chat?questionId={questionId}",
                arguments = listOf(
                    navArgument("questionId") { defaultValue = "" },
                ),
            ) { entry ->
                val questionId = entry.arguments?.getString("questionId").orEmpty()
                val vm: ChatViewModel = viewModel(
                    key = "chat/$questionId",
                    factory = ChatViewModel.Factory(questionId),
                )
                val state by vm.state.collectAsState()
                ChatScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            ChatEvent.Back -> navController.popBackStack()
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable("modelConfig") {
                val vm: ModelConfigViewModel = viewModel()
                val state by vm.state.collectAsState()
                ModelConfigScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            ModelConfigEvent.Back -> navController.popBackStack()
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
            composable("serverConfig") {
                val vm: ServerConfigViewModel = viewModel()
                val state by vm.state.collectAsState()
                ServerConfigScreen(
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            ServerConfigEvent.Back -> navController.popBackStack()
                            else -> vm.onEvent(event)
                        }
                    },
                )
            }
        }
    }
}

/** 解析崩溃日志文本为弹窗 state：提取「时间：」「版本：」行，其余为堆栈 */
private fun parseCrash(text: String?): com.drillbit.ui.components.CrashDialogUi {
    val full = text.orEmpty()
    var time = ""
    var version = ""
    val stack = StringBuilder()
    full.lines().forEach { line ->
        when {
            line.startsWith("时间：") -> time = line.removePrefix("时间：")
            line.startsWith("版本：") -> version = line.removePrefix("版本：")
            else -> stack.appendLine(line)
        }
    }
    return com.drillbit.ui.components.CrashDialogUi(
        timeText = time,
        versionText = version,
        stackText = stack.toString().trim(),
    )
}
