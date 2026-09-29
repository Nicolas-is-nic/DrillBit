package com.drillbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.drillbit.ui.banks.BankDetailEvent
import com.drillbit.ui.banks.BankDetailScreen
import com.drillbit.ui.banks.BankDetailUiState
import com.drillbit.ui.banks.BankListEvent
import com.drillbit.ui.banks.BankListScreen
import com.drillbit.ui.banks.BankListUiState
import com.drillbit.ui.chat.ChatEvent
import com.drillbit.ui.chat.ChatScreen
import com.drillbit.ui.chat.ChatUiState
import com.drillbit.ui.components.CrashDialog
import com.drillbit.ui.components.CrashDialogEvent
import com.drillbit.ui.components.CrashDialogUi
import com.drillbit.ui.components.DbBottomNavBar
import com.drillbit.ui.components.dbTabs
import com.drillbit.ui.notes.BackupEvent
import com.drillbit.ui.notes.BackupScreen
import com.drillbit.ui.notes.BackupUiState
import com.drillbit.ui.notes.DigestEvent
import com.drillbit.ui.notes.DigestScreen
import com.drillbit.ui.notes.DigestUiState
import com.drillbit.ui.notes.NoteEditEvent
import com.drillbit.ui.notes.NoteEditScreen
import com.drillbit.ui.notes.NoteEditUiState
import com.drillbit.ui.notes.NoteListEvent
import com.drillbit.ui.notes.NoteListScreen
import com.drillbit.ui.notes.NoteListUiState
import com.drillbit.ui.quiz.MixConfigEvent
import com.drillbit.ui.quiz.MixConfigScreen
import com.drillbit.ui.quiz.MixConfigUiState
import com.drillbit.ui.quiz.QuestionUi
import com.drillbit.ui.quiz.QuizEvent
import com.drillbit.ui.quiz.QuizMode
import com.drillbit.ui.quiz.QuizPhase
import com.drillbit.ui.quiz.QuizScreen
import com.drillbit.ui.quiz.QuizUiState
import com.drillbit.ui.settings.ApiType
import com.drillbit.ui.settings.ModelConfigEvent
import com.drillbit.ui.settings.ModelConfigScreen
import com.drillbit.ui.settings.ModelConfigUiState
import com.drillbit.ui.settings.ServerConfigEvent
import com.drillbit.ui.settings.ServerConfigScreen
import com.drillbit.ui.settings.ServerConfigUiState
import com.drillbit.ui.settings.SettingsEvent
import com.drillbit.ui.settings.SettingsScreen
import com.drillbit.ui.settings.SettingsUiState
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors
import com.drillbit.ui.wrong.WrongListEvent
import com.drillbit.ui.wrong.WrongListScreen
import com.drillbit.ui.wrong.WrongListUiState

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
            // 阶段 1 由内存态持有主题开关（默认亮色），阶段 2 接 DataStore 持久化
            var darkTheme by remember { mutableStateOf(false) }
            var crashVisible by remember { mutableStateOf(!crashText.isNullOrBlank()) }
            DrillBitTheme(darkTheme = darkTheme) {
                DrillBitApp(
                    darkTheme = darkTheme,
                    onDarkModeChange = { on -> darkTheme = on },
                )
                if (crashVisible) {
                    CrashDialog(
                        state = CrashDialogUi(
                            timeText = "",
                            versionText = "",
                            stackText = crashText.orEmpty(),
                        ),
                        onEvent = { event ->
                            if (event is CrashDialogEvent.Close) crashVisible = false
                        },
                    )
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
                BankListScreen(
                    state = BankListUiState(
                        banks = emptyList(),
                        syncing = false,
                        lastSyncText = "尚未同步",
                        updateDialog = null,
                    ),
                    onEvent = { event ->
                        when (event) {
                            is BankListEvent.BankClick ->
                                navController.navigate("bankDetail/${event.bankId}")

                            BankListEvent.MixClick -> navController.navigate("mixConfig")
                            BankListEvent.SyncClick,
                            BankListEvent.UpdateConfirm,
                            BankListEvent.UpdateCancel,
                            -> Unit
                        }
                    },
                )
            }
            composable("wrong") {
                WrongListScreen(
                    state = WrongListUiState(
                        items = emptyList(),
                        summaryText = "共 0 题待清 · 每答对一次，计数减一，减到 0 移出错题集",
                    ),
                    onEvent = { event ->
                        when (event) {
                            is WrongListEvent.ItemClick -> Unit
                            WrongListEvent.RetryAll -> navController.navigate("quiz?mode=retry")
                        }
                    },
                )
            }
            composable("notes") {
                NoteListScreen(
                    state = NoteListUiState(
                        items = emptyList(),
                        summaryText = "共 0 条 · 尚未备份",
                        summarizing = false,
                    ),
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
                SettingsScreen(
                    state = SettingsUiState(
                        darkMode = darkTheme,
                        serverConfigured = false,
                        updateAvailableText = null,
                        modelSummary = "未配置",
                        lastBackupText = "--",
                        versionText = "v${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                        hasCrashLog = false,
                    ),
                    onEvent = { event ->
                        when (event) {
                            is SettingsEvent.DarkModeChange -> onDarkModeChange(event.on)
                            SettingsEvent.ServerClick -> navController.navigate("serverConfig")
                            SettingsEvent.ModelClick -> navController.navigate("modelConfig")
                            SettingsEvent.BackupClick -> navController.navigate("backup")
                            SettingsEvent.CheckUpdate,
                            SettingsEvent.CrashLogClick,
                            -> Unit
                        }
                    },
                )
            }

            // ===== 二级页 =====
            composable("bankDetail/{bankId}") { entry ->
                val bankId = entry.arguments?.getString("bankId").orEmpty()
                BankDetailScreen(
                    state = BankDetailUiState(
                        bankId = bankId,
                        name = "题库详情",
                        total = 0,
                        done = 0,
                        lastSyncText = "--",
                        serverVersionText = "--",
                        hasUpdate = false,
                        deleteConfirmVisible = false,
                    ),
                    onEvent = { event ->
                        when (event) {
                            BankDetailEvent.ContinueClick,
                            BankDetailEvent.RestartClick,
                            -> navController.navigate("quiz?mode=single&bankId=$bankId")

                            BankDetailEvent.Back -> navController.popBackStack()
                            BankDetailEvent.CheckUpdateClick,
                            BankDetailEvent.DeleteClick,
                            BankDetailEvent.DeleteConfirm,
                            BankDetailEvent.DeleteCancel,
                            -> Unit
                        }
                    },
                )
            }
            composable("mixConfig") {
                MixConfigScreen(
                    state = MixConfigUiState(
                        banks = emptyList(),
                        count = 10,
                        minCount = 10,
                        step = 10,
                    ),
                    onEvent = { event ->
                        when (event) {
                            MixConfigEvent.Start -> navController.navigate("quiz?mode=mix")
                            MixConfigEvent.Back -> navController.popBackStack()
                            is MixConfigEvent.ToggleBank,
                            MixConfigEvent.Minus,
                            MixConfigEvent.Plus,
                            -> Unit
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
                    else -> QuizMode.SINGLE
                }
                QuizScreen(
                    state = QuizUiState(
                        mode = mode,
                        title = when (mode) {
                            QuizMode.MIX -> "混合卷"
                            QuizMode.RETRY -> "错题重考"
                            QuizMode.SINGLE -> "刷题"
                        },
                        currentIndex = 0,
                        totalCount = 0,
                        correctCount = 0,
                        progress = 0f,
                        phase = QuizPhase.ANSWERING,
                        question = QuestionUi(
                            stem = "",
                            options = emptyList(),
                            sourceBankName = null,
                        ),
                        answered = null,
                        finished = false,
                    ),
                    onEvent = { event ->
                        when (event) {
                            QuizEvent.AskAi -> navController.navigate("chat")
                            QuizEvent.Back -> navController.popBackStack()
                            is QuizEvent.OptionClick,
                            QuizEvent.Next,
                            -> Unit
                        }
                    },
                )
            }
            composable("noteEdit/{noteId}") { entry ->
                NoteEditScreen(
                    state = NoteEditUiState(
                        noteId = entry.arguments?.getString("noteId").orEmpty(),
                        title = "",
                        content = "",
                        sourceQuestionText = null,
                        deleteConfirmVisible = false,
                    ),
                    onEvent = { event ->
                        when (event) {
                            NoteEditEvent.Back -> navController.popBackStack()
                            is NoteEditEvent.TitleChange,
                            is NoteEditEvent.ContentChange,
                            NoteEditEvent.Save,
                            NoteEditEvent.SourceClick,
                            NoteEditEvent.DeleteClick,
                            NoteEditEvent.DeleteConfirm,
                            NoteEditEvent.DeleteCancel,
                            -> Unit
                        }
                    },
                )
            }
            composable("digest") {
                DigestScreen(
                    state = DigestUiState(
                        metaText = "尚无归纳稿",
                        sections = emptyList(),
                        streaming = false,
                        footerText = "",
                    ),
                    onEvent = { event ->
                        when (event) {
                            DigestEvent.Back -> navController.popBackStack()
                            DigestEvent.Regenerate,
                            DigestEvent.ViewFull,
                            -> Unit
                        }
                    },
                )
            }
            composable("backup") {
                BackupScreen(
                    state = BackupUiState(
                        lastBackupText = "--",
                        backedCount = 0,
                        serverHost = "未配置",
                        uploading = false,
                        resultBanner = null,
                    ),
                    onEvent = { event ->
                        when (event) {
                            BackupEvent.Back -> navController.popBackStack()
                            BackupEvent.Upload -> Unit
                        }
                    },
                )
            }
            composable(
                route = "chat?questionId={questionId}",
                arguments = listOf(
                    navArgument("questionId") { defaultValue = "" },
                ),
            ) {
                ChatScreen(
                    state = ChatUiState(
                        modelName = "未配置",
                        contextSummary = "",
                        messages = emptyList(),
                        input = "",
                        sending = false,
                        errorBannerText = null,
                        saveDialog = null,
                    ),
                    onEvent = { event ->
                        when (event) {
                            ChatEvent.Back -> navController.popBackStack()
                            is ChatEvent.InputChange,
                            ChatEvent.Send,
                            ChatEvent.SaveClick,
                            is ChatEvent.SaveConfirm,
                            ChatEvent.SaveCancel,
                            -> Unit
                        }
                    },
                )
            }
            composable("modelConfig") {
                ModelConfigScreen(
                    state = ModelConfigUiState(
                        apiType = ApiType.OPENAI,
                        url = "",
                        apiKey = "",
                        modelName = "",
                        testing = false,
                        testResult = null,
                    ),
                    onEvent = { event ->
                        when (event) {
                            ModelConfigEvent.Back -> navController.popBackStack()
                            is ModelConfigEvent.TypeChange,
                            is ModelConfigEvent.UrlChange,
                            is ModelConfigEvent.KeyChange,
                            is ModelConfigEvent.NameChange,
                            ModelConfigEvent.Test,
                            ModelConfigEvent.Save,
                            -> Unit
                        }
                    },
                )
            }
            composable("serverConfig") {
                ServerConfigScreen(
                    state = ServerConfigUiState(
                        url = "",
                        token = "",
                        testing = false,
                        testResult = null,
                    ),
                    onEvent = { event ->
                        when (event) {
                            ServerConfigEvent.Back -> navController.popBackStack()
                            is ServerConfigEvent.UrlChange,
                            is ServerConfigEvent.TokenChange,
                            ServerConfigEvent.Test,
                            ServerConfigEvent.Save,
                            -> Unit
                        }
                    },
                )
            }
        }
    }
}
