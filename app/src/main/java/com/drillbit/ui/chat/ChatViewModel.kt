package com.drillbit.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.db.QuestionEntity
import com.drillbit.data.net.LlmMessage
import com.drillbit.data.parseAnswers
import com.drillbit.data.parseOptions
import com.drillbit.data.repo.NoteRepository
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.BannerUi
import com.drillbit.ui.components.ChatRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AI 问答 ViewModel（spec 4.2.1 流式、4.3.4 多轮）：
 * 题目上下文放入 system；请求携带最近若干轮已完成对话；回答流式增量渲染；
 * 中断保留已收内容；每条 AI 回复可保存到笔记（按消息 id 定位）。
 */
class ChatViewModel(private val questionId: String) : ViewModel() {

    private val llm = ServiceLocator.llmClient
    private val noteRepo: NoteRepository = ServiceLocator.noteRepository

    /** 当前题（从题目进入时非空） */
    private var question: QuestionEntity? = null
    private var bankName: String = ""

    /** 会话内消息自增 id：保存时定位具体是哪条回复 */
    private var nextMessageId = 1L

    /** 历史携带上限（轮）：一轮 = user + assistant */
    private val historyMaxRounds = 6

    // ===== 临时诊断（v19）：定位保存按钮失效后随调试面板一起删除 =====
    private var saveClickCount = 0
    private var lastOpenSaveResult = "未触发"

    private val stateFlow = MutableStateFlow(
        ChatUiState(
            modelName = "未配置",
            contextSummary = "",
            messages = emptyList(),
            input = "",
            sending = false,
            errorBannerText = null,
            saveDialog = null,
            debugText = "",
        ),
    )
    val state: StateFlow<ChatUiState> = stateFlow.asStateFlow()

    /** 进行中的流式请求（可取消） */
    private var sendJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = ServiceLocator.settingsStore.snapshot()
            val typeLabel = if (settings.llmType == "anthropic") "Anthropic" else "OpenAI 兼容"
            val model = if (settings.llmModel.isBlank()) {
                "未配置"
            } else {
                "${settings.llmModel} · $typeLabel"
            }
            var summary = ""
            if (questionId.isNotBlank()) {
                val q = ServiceLocator.database.questionDao().getById(questionId)
                if (q != null) {
                    question = q
                    bankName = ServiceLocator.bankRepository.getBank(q.bankId)?.name ?: ""
                    val optionCount = parseOptions(q.optionsJson).size
                    summary = "题干 + $optionCount 个选项 + 解析（来自 $bankName · 第 ${q.orderIndex + 1} 题）"
                }
            }
            stateFlow.value = stateFlow.value.copy(
                modelName = model,
                contextSummary = summary,
            )
        }
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.InputChange -> stateFlow.value = stateFlow.value.copy(input = event.text)
            ChatEvent.Send -> send()
            is ChatEvent.SaveClick -> {
                saveClickCount++            // 临时诊断（v19）
                openSaveDialog(event.messageId)
                refreshDebug()               // 临时诊断（v19）
            }
            is ChatEvent.SaveConfirm -> saveNote(event.title, event.content)
            ChatEvent.SaveCancel -> stateFlow.value = stateFlow.value.copy(saveDialog = null)
            else -> Unit // Back 由导航层处理
        }
    }

    private fun send() {
        val text = stateFlow.value.input.trim()
        if (text.isEmpty() || stateFlow.value.sending) return
        // 请求消息：system（角色 + 篇幅约束 + 题目上下文）+ 最近 6 轮历史 + 本轮问题
        val requestMessages = buildRequestMessages(text)
        stateFlow.value = stateFlow.value.copy(
            messages = stateFlow.value.messages + listOf(
                ChatMessageUi(nextMessageId++, ChatRole.ME, text, streaming = false, showSave = false),
                ChatMessageUi(nextMessageId++, ChatRole.AI, "", streaming = true, showSave = false),
            ),
            input = "",
            sending = true,
            errorBannerText = null,
        )

        sendJob = viewModelScope.launch {
            val settings = ServiceLocator.settingsStore.snapshot()
            if (settings.llmUrl.isBlank() || settings.llmKey.isBlank()) {
                finishStreamWithError("模型未配置，请先到「设置 - 模型配置」填写")
                return@launch
            }
            runCatching {
                llm.chatStream(settings, requestMessages).collect { delta ->
                    appendDelta(delta)
                }
            }.onFailure { e ->
                finishStreamWithError(e.message ?: "连接中断")
            }.onSuccess {
                // 正常完成：去掉 streaming 态，有内容则显示保存入口
                updateLastAi { msg -> msg.copy(streaming = false, showSave = msg.text.isNotEmpty()) }
                stateFlow.value = stateFlow.value.copy(sending = false)
                refreshDebug()   // 临时诊断（v19）
            }
        }
    }

    /** system：答疑角色 + 篇幅约束；有题目时附带题目上下文（每轮可见，历史截断也不丢） */
    private fun buildSystemPrompt(): String {
        val base = "你是一位耐心的中文技术答疑助手，面向刷题学习者。" +
            "回答准确、条理清晰，先直接回答问题，再补充必要的背景与关联知识。" +
            "默认控制篇幅、简洁作答（一般不超过 200 字）；仅当用户明确要求详细讲解时才展开。"
        val q = question ?: return base
        val options = parseOptions(q.optionsJson)
        val answers = parseAnswers(q.answersJson).distinct().filter { it in options.indices }
        // 判断题用选项文本更自然（如「对」）；其余题型用字母拼接（如「ACD」）
        val answerText = if (q.type == "judge") {
            answers.joinToString("、") { options[it] }
        } else {
            answers.joinToString("") { ('A' + it).toString() }
        }
        return buildString {
            append(base).append("\n\n【当前题目】\n")
            append("题干：").append(q.stem).append('\n')
            options.forEachIndexed { i, opt -> append('A' + i).append("．").append(opt).append('\n') }
            append("正确答案：").append(answerText).append('\n')
            append("参考解析：").append(q.explanation)
        }
    }

    /** 组多轮请求：system + 最近 6 轮已完成对话（同角色合并，超限截断）+ 本轮问题 */
    private fun buildRequestMessages(currentInput: String): List<LlmMessage> {
        val history = mutableListOf<LlmMessage>()
        stateFlow.value.messages.forEach { message ->
            if (message.text.isBlank()) return@forEach
            val role = if (message.role == ChatRole.ME) "user" else "assistant"
            val last = history.lastOrNull()
            if (last != null && last.role == role) {
                history[history.lastIndex] = last.copy(content = last.content + "\n\n" + message.text)
            } else {
                history += LlmMessage(role, message.text)
            }
        }
        val result = mutableListOf(LlmMessage("system", buildSystemPrompt()))
        result += trimHistory(history)
        val last = result.lastOrNull()
        if (last != null && last.role == "user") {
            // 上一条 AI 回复为空被跳过时会出现连续 user，合并避免协议报错
            result[result.lastIndex] = last.copy(content = last.content + "\n\n" + currentInput)
        } else {
            result += LlmMessage("user", currentInput)
        }
        return result
    }

    /** 历史上限：最近 6 轮（12 条）；截断后确保首条为 user（协议要求） */
    private fun trimHistory(history: List<LlmMessage>): List<LlmMessage> {
        if (history.size <= historyMaxRounds * 2) return history
        return history.takeLast(historyMaxRounds * 2).dropWhile { it.role != "user" }
    }

    /** 追加增量到最后一条 AI 消息 */
    private fun appendDelta(delta: String) {
        updateLastAi { msg -> msg.copy(text = msg.text + delta) }
    }

    private fun updateLastAi(transform: (ChatMessageUi) -> ChatMessageUi) {
        val messages = stateFlow.value.messages.toMutableList()
        val lastIdx = messages.indexOfLast { it.role == ChatRole.AI }
        if (lastIdx >= 0) {
            messages[lastIdx] = transform(messages[lastIdx])
            stateFlow.value = stateFlow.value.copy(messages = messages)
        }
    }

    /** 失败/中断：保留已收内容，标记错误横幅 */
    private fun finishStreamWithError(reason: String) {
        updateLastAi { msg -> msg.copy(streaming = false, showSave = msg.text.isNotEmpty()) }
        stateFlow.value = stateFlow.value.copy(
            sending = false,
            errorBannerText = reason,
        )
        refreshDebug()   // 临时诊断（v19）
    }

    /** 打开保存弹窗：按消息 id 取该条 AI 回复，标题取它之前最近一条 ME 消息 */
    private fun openSaveDialog(messageId: Long) {
        val messages = stateFlow.value.messages
        val aiIndex = messages.indexOfLast { it.id == messageId && it.role == ChatRole.AI }
        if (aiIndex < 0) {
            lastOpenSaveResult = "未找到消息"   // 临时诊断（v19）
            return
        }
        val ai = messages[aiIndex]
        if (ai.text.isEmpty()) {
            lastOpenSaveResult = "空文本"       // 临时诊断（v19）
            return
        }
        val meText = messages.subList(0, aiIndex).lastOrNull { it.role == ChatRole.ME }?.text.orEmpty()
        val titleSource = meText.ifBlank { question?.stem.orEmpty() }
        val sourceLine = if (question != null) {
            "\n\n来源：$bankName · 第 ${question!!.orderIndex + 1} 题"
        } else {
            ""
        }
        lastOpenSaveResult = "正常"             // 临时诊断（v19）
        stateFlow.value = stateFlow.value.copy(
            saveDialog = SaveNoteDialogState(
                title = if (titleSource.length > 20) titleSource.take(20) else titleSource.ifBlank { "AI 回答笔记" },
                content = ai.text + sourceLine,
            ),
        )
    }

    private fun saveNote(title: String, content: String) {
        viewModelScope.launch {
            noteRepo.saveNote(
                noteId = 0,
                title = title,
                content = content,
                source = "AI问答",
                sourceQuestionId = questionId.ifBlank { null },
                bankName = bankName.ifBlank { null },
            )
            stateFlow.value = stateFlow.value.copy(saveDialog = null)
            refreshDebug()   // 临时诊断（v19）
        }
    }

    /** 临时诊断（v19）：汇总保存链路关键状态，定位保存按钮失效后删除 */
    private fun refreshDebug() {
        val messages = stateFlow.value.messages
        val lastAi = messages.lastOrNull { it.role == ChatRole.AI }
        val dialogOpen = if (stateFlow.value.saveDialog != null) "开" else "关"
        stateFlow.value = stateFlow.value.copy(
            debugText = "点击到达=$saveClickCount 弹窗=$dialogOpen 早退=$lastOpenSaveResult 末条长度=${lastAi?.text?.length ?: -1}",
        )
    }

    class Factory(private val questionId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(questionId) as T
    }
}
