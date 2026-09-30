package com.drillbit.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 时间格式化工具（展示文案统一） */
object TimeFmt {

    private val monthDay = SimpleDateFormat("MM-dd", Locale.CHINA)
    private val monthDayHm = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)

    /** 「09-27」 */
    fun short(ts: Long): String = if (ts <= 0) "--" else monthDay.format(Date(ts))

    /** 「09-27 21:40」 */
    fun medium(ts: Long): String = if (ts <= 0) "--" else monthDayHm.format(Date(ts))
}
