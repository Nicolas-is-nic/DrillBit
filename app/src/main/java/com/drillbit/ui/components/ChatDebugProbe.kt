package com.drillbit.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 临时诊断（v20）：保存按钮点击链路探针。
 * 定位「保存到笔记」长内容点击无效问题后，随调试面板一起删除。
 */
object ChatDebugProbe {

    /** 最近一次按下的落点归属：按钮 / Markdown / 按钮点击回调 / 空串（未命中具体子节点） */
    var lastDownTarget by mutableStateOf("")

    /** 保存按钮在合成树根坐标中的 bounds，格式 [left,top,right,bottom] */
    var buttonBounds by mutableStateOf("")

    /** 纯文本对照开关：开启后 AI 回复不走 Markdown 渲染，用于验证是否渲染层导致点击失效 */
    var forcePlainText by mutableStateOf(false)
}
