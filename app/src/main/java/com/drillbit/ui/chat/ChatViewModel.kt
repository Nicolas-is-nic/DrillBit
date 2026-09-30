package com.drillbit.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.db.QuestionEntity
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
 * AI 问答 ViewModel（spec 4.2.1 流式）：
 * 从题目进入时自动携带题干/选项/解析上下文；回答流式增量渲染；
 * 中断保留已收内容；一键保存到笔记。
 */
class ChatViewModel(private val questionId: String) : ViewModel() {

    private val llm = ServiceLocator.llmClient
    private val noteRepo: NoteRepository = ServiceLocator.noteRepository

    /** 当前题（从题目进入时非空） */
    private var question: QuestionEntity? = null
    private var bankName: String = ""

    private val stateFlow = MutableStateFlow(
        ChatUiState(
            modelName = "未配置",
            contextSummary = "",
            messages = emptyList(),
            input = "",
            sending = false,
            errorBannerText = null,
            saveDialog = null,
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
            ChatEvent.SaveClick -> openSaveDialog()
            is ChatEvent.SaveConfirm -> saveNote(event.title, event.content)
            ChatEvent.SaveCancel -> stateFlow.value = stateFlow.value.copy(saveDialog = null)
            else -> Unit // Back 由导航层处理
        }
    }

    private fun send() {
        val text = stateFlow.value.input.trim()
        if (text.isEmpty() || stateFlow.value.sending) return
        val q = question
        // user 消息：题目上下文（若有）+ 用户问题
        val userMessage = buildString {
            if (q != null) {
                val options = parseOptions(q.optionsJson)
                val answerIdx = parseAnswers(q.answersJson).firstOrNull() ?: 0
                append("我在刷一道选择题，请帮我理解：\n")
                append("题干：").append(q.stem).append('\n')
                options.forEachIndexed { i, opt -> append('A' + i).append("．").append(opt).append('\n') }
                append("正确答案：").append('A' + answerIdx).append('\n')
                append("参考解析：").append(q.explanation).append("\n\n")
            }
            append("我的问题：").append(text)
        }

        stateFlow.value = stateFlow.value.copy(
            messages = stateFlow.value.messages + listOf(
                ChatMessageUi(ChatRole.ME, text, streaming = false, showSave = false),
                ChatMessageUi(ChatRole.AI, "", streaming = true, showSave = false),
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
            val system = "你是一位耐心的中文技术答疑助手，面向刷题学习者。" +
                "回答准确、条理清晰，先直接回答问题，再补充必要的背景与关联知识。"
            runCatching {
                llm.chatStream(settings, system, userMessage).collect { delta ->
                    appendDelta(delta)
                }
            }.onFailure { e ->
                finishStreamWithError(e.message ?: "连接中断")
            }.onSuccess {
                // 正常完成：去掉 streaming 态，有内容则显示保存入口
                updateLastAi { msg -> msg.copy(streaming = false, showSave = msg.text.isNotEmpty()) }
                stateFlow.value = stateFlow.value.copy(sending = false)
            }
        }
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
    }

    private fun openSaveDialog() {
        val lastAi = stateFlow.value.messages.lastOrNull { it.role == ChatRole.AI } ?: return
        if (lastAi.text.isEmpty()) return
        val questionTitle = stateFlow.value.messages.lastOrNull { it.role == ChatRole.ME }?.text ?: ""
        val sourceLine = if (question != null) {
            "\n\n来源：$bankName · 第 ${question!!.orderIndex + 1} 题"
        } else {
            ""
        }
        stateFlow.value = stateFlow.value.copy(
            saveDialog = SaveNoteDialogState(
                title = if (questionTitle.length > 20) questionTitle.take(20) else questionTitle.ifBlank { "AI 回答笔记" },
                content = lastAi.text + sourceLine,
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
        }
    }

    class Factory(private val questionId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(questionId) as T
    }
}
