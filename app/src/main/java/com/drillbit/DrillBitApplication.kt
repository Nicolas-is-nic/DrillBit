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
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching {
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                    .format(Date())
                crashFile().writeText(
                    "时间：$time\n线程：${thread.name}\n\n${e.stackTraceToString()}"
                )
            }
            defaultHandler?.uncaughtException(thread, e)
        }
    }

    private fun crashFile(): File = File(filesDir, "crash_last.txt")

    companion object {
        /** 崩溃日志文件（MainActivity 读取展示后删除） */
        fun crashFile(context: android.content.Context): File =
            File(context.filesDir, "crash_last.txt")
    }
}
