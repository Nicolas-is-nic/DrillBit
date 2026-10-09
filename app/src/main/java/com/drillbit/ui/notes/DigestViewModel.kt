package com.drillbit.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.repo.NoteRepository
import com.drillbit.data.net.LlmMessage
import com.drillbit.util.TimeFmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 归纳稿 ViewModel（spec 4.3.4）：
 * 展示最近一次梳理结果（按「一、二、…」分段）；Regenerate 走流式重新生成并入库。
 * 流式期间以单 section 实时显示已生成文本，完成后解析分段。
 */
class DigestViewModel : ViewModel() {

    private val repo: NoteRepository = ServiceLocator.noteRepository

    private val stateFlow = MutableStateFlow(
        DigestUiState(
            metaText = "尚无归纳稿，点击「重新梳理」生成",
            sections = emptyList(),
            streaming = false,
            footerText = "",
        ),
    )
    val state: StateFlow<DigestUiState> = stateFlow.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: DigestEvent) {
        when (event) {
            DigestEvent.Regenerate -> regenerate()
            else -> Unit // Back 由导航层处理
        }
    }

    /** 读最近一篇归纳稿并分段展示 */
    private fun refresh() {
        viewModelScope.launch {
            val digest = latestDigest()
            if (digest == null) {
                stateFlow.value = stateFlow.value.copy(
                    metaText = "尚无归纳稿，点击「重新梳理」由大模型梳理全部笔记",
                    sections = emptyList(),
                    footerText = "",
                )
                return@launch
            }
            val sections = parseSections(digest.content)
            stateFlow.value = stateFlow.value.copy(
                metaText = "梳理于 ${TimeFmt.medium(digest.updatedAt)} · 已存入笔记",
                sections = sections,
                footerText = "共 ${sections.size} 个主题 · 已去除重复表述",
            )
        }
    }

    /** 流式重新梳理：全部笔记 → 大模型 → 归纳稿入库 → 刷新展示 */
    private fun regenerate() {
        if (stateFlow.value.streaming) return
        viewModelScope.launch {
            val settings = ServiceLocator.settingsStore.snapshot()
            if (settings.llmUrl.isBlank()) {
                stateFlow.value = stateFlow.value.copy(
                    footerText = "模型未配置，请先到「设置 - 模型配置」填写",
                )
                return@launch
            }
            val prompt = repo.buildSummarizePrompt()
            if (prompt == null) {
                stateFlow.value = stateFlow.value.copy(
                    footerText = "还没有笔记，先在做题或问答中积累一些吧",
                )
                return@launch
            }
            stateFlow.value = stateFlow.value.copy(
                metaText = "正在梳理全部笔记…",
                sections = emptyList(),
                streaming = true,
                footerText = "",
            )
            val builder = StringBuilder()
            val (system, user) = prompt
            runCatching {
                // 分发梳理：system + user 两条消息（LlmMessage 列表接口）
                val chatMessages = listOf(LlmMessage("system", system), LlmMessage("user", user))
                ServiceLocator.llmClient.chatStream(settings, chatMessages).collect { delta ->
                    builder.append(delta)
                    // 流式中间态：已生成文本作为单 section 实时展示
                    stateFlow.value = stateFlow.value.copy(
                        sections = listOf(DigestSection("生成中…", builder.toString())),
                    )
                }
            }.onFailure { e ->
                stateFlow.value = stateFlow.value.copy(
                    streaming = false,
                    footerText = "梳理失败：${e.message ?: "连接中断"}（已生成内容未保存）",
                )
                return@launch
            }.onSuccess {
                val content = builder.toString().trim()
                if (content.isNotEmpty()) {
                    repo.saveNote(
                        noteId = 0,
                        title = "知识点归纳稿 · ${TimeFmt.short(System.currentTimeMillis())}",
                        content = content,
                        source = "归纳稿",
                        sourceQuestionId = null,
                        bankName = null,
                    )
                }
                stateFlow.value = stateFlow.value.copy(streaming = false)
                refresh()
            }
        }
    }

    private suspend fun latestDigest() =
        ServiceLocator.database.noteDao().getAllOnce().firstOrNull { it.source == "归纳稿" }

    /** 按「一、二、…」标题行分段；无标题行时整篇作为一段 */
    private fun parseSections(content: String): List<DigestSection> {
        val sections = mutableListOf<DigestSection>()
        val titleRegex = Regex("^[一二三四五六七八九十]+、.+")
        var currentTitle: String? = null
        val currentBody = StringBuilder()
        content.lines().forEach { raw ->
            val line = raw.trim()
            if (titleRegex.matches(line)) {
                currentTitle?.let { sections.add(DigestSection(it, currentBody.toString().trim())) }
                currentTitle = line
                currentBody.clear()
            } else {
                currentBody.append(raw).append('\n')
            }
        }
        currentTitle?.let { sections.add(DigestSection(it, currentBody.toString().trim())) }
        if (sections.isEmpty() && content.isNotBlank()) {
            sections.add(DigestSection("要点", content.trim()))
        }
        return sections
    }
}
