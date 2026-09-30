package com.drillbit

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用入口：安装全局崩溃捕获。
 *
 * 崩溃时把完整堆栈写入 filesDir/crash_last.txt（覆盖式，只留最后一次），
 * 下次启动由 MainActivity 弹出展示，便于无 adb 环境下定位闪退原因。
 */
class DrillBitApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化全局服务定位器（数据库/配置/网络客户端从此取）
        ServiceLocator.init(this)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching {
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                    .format(Date())
                crashFile().writeText(
                    "时间：$time\n线程：${thread.name}\n版本：v${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）\n\n${e.stackTraceToString()}"
                )
            }
            defaultHandler?.uncaughtException(thread, e)
        }
    }

    private fun crashFile(): File = File(filesDir, "crash_last.txt")

    companion object {
        /** 本次启动读到的崩溃文本（弹窗展示与设置页「有崩溃日志」状态用） */
        @Volatile
        var lastCrashLog: String? = null
        /** 崩溃日志文件（MainActivity 读取展示后删除） */
        fun crashFile(context: android.content.Context): File =
            File(context.filesDir, "crash_last.txt")
    }
}
