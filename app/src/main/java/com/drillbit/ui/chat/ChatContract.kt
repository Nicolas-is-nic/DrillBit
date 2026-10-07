package com.drillbit.ui.chat

import com.drillbit.ui.components.ChatRole

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.6 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 说明：ChatRole 文档未给声明，按 7.6 节注释「ME / AI」定义在 ui/components（ChatBubble.kt）。
 */

data class ChatMessageUi(
    val id: Long,                    // 会话内消息自增标识（保存时定位具体回复）
    val role: ChatRole,              // ME / AI
    val text: String,
    val streaming: Boolean,          // AI 回复生成中
    val showSave: Boolean            // AI 完整回复后显示「保存到笔记」
)

data class SaveNoteDialogState(
    val title: String,               // 预填
    val content: String              // 预填，含来源行「来源：题库名 · 第 N 题」
)

data class ChatUiState(
    val modelName: String,
    val contextSummary: String,      // 如「题干 + 4 个选项 + 解析（来自大模型基础 第 12 题）」；无上下文时为空串
    val messages: List<ChatMessageUi>,
    val input: String,
    val sending: Boolean,
    val errorBannerText: String?,    // 网络失败/流式中断提示
    val saveDialog: SaveNoteDialogState?,
    val debugText: String = ""       // 临时诊断（v19）：保存按钮失效定位用，定位后删除
)

sealed interface ChatEvent {
    data class InputChange(val text: String) : ChatEvent
    data object Send : ChatEvent
    data class SaveClick(val messageId: Long) : ChatEvent   // 打开保存弹窗（对该条 AI 回复）
    data class SaveConfirm(val title: String, val content: String) : ChatEvent
    data object SaveCancel : ChatEvent
    data object Back : ChatEvent
}
