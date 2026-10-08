package com.drillbit

import android.content.Context
import com.drillbit.data.SettingsStore
import com.drillbit.data.db.DrillBitDatabase
import com.drillbit.data.net.ServerApi
import com.drillbit.data.repo.BankRepository

/**
 * 服务定位器：自用 App 的极简依赖容器（object + lazy）。
 * 首次访问需传 applicationContext，之后全局复用。
 */
object ServiceLocator {

    @Volatile
    private var appContext: Context? = null

    private fun context(): Context =
        appContext ?: error("ServiceLocator 未初始化，需在 Application.onCreate 传入 applicationContext")

    /** 供仓库/ViewModel 取应用级上下文（assets、剪贴板等） */
    fun appContext(): Context = context()

    /** 由 DrillBitApplication.onCreate 调用 */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val database: DrillBitDatabase by lazy { DrillBitDatabase.get(context()) }
    val settingsStore: SettingsStore by lazy { SettingsStore(context()) }
    val serverApi: ServerApi by lazy { ServerApi() }

    val bankRepository: BankRepository by lazy {
        BankRepository(database, serverApi)
    }

    val quizRepository: com.drillbit.data.repo.QuizRepository by lazy {
        com.drillbit.data.repo.QuizRepository(database)
    }

    val wrongRepository: com.drillbit.data.repo.WrongRepository by lazy {
        com.drillbit.data.repo.WrongRepository(database)
    }

    val favoriteRepository: com.drillbit.data.repo.FavoriteRepository by lazy {
        com.drillbit.data.repo.FavoriteRepository(database)
    }

    val syncRepository: com.drillbit.data.repo.SyncRepository by lazy {
        com.drillbit.data.repo.SyncRepository(database, serverApi, settingsStore)
    }

    val llmClient: com.drillbit.data.net.LlmClient by lazy {
        com.drillbit.data.net.LlmClient()
    }

    val noteRepository: com.drillbit.data.repo.NoteRepository by lazy {
        com.drillbit.data.repo.NoteRepository(database, serverApi)
    }

    /** 本次启动是否存在未处理完的崩溃日志（设置页状态用） */
    fun hasPendingCrashLog(): Boolean = DrillBitApplication.lastCrashLog != null
}
