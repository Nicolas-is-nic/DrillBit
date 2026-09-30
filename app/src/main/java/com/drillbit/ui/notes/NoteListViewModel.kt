package com.drillbit.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.repo.NoteRepository
import com.drillbit.util.TimeFmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 笔记列表 ViewModel：列表（区分归纳稿）、统计行、备份入口时间。
 * Summarize 事件由导航层进入归纳稿页，梳理动作在 DigestViewModel 完成。
 */
class NoteListViewModel : ViewModel() {

    private val repo: NoteRepository = ServiceLocator.noteRepository

    private val summarizing = MutableStateFlow(false)

    val state: StateFlow<NoteListUiState> = combine(
        repo.observeNotes(),
        ServiceLocator.settingsStore.settings,
        summarizing,
    ) { notes, settings, summarizingNow ->
        NoteListUiState(
            items = notes.map { n ->
                NoteCard(
                    noteId = n.id.toString(),
                    title = n.title,
                    sourceText = when (n.source) {
                        "归纳稿" -> "大模型梳理生成 · ${TimeFmt.short(n.updatedAt)}"
                        "AI问答" -> "来自 AI 问答 · ${TimeFmt.short(n.updatedAt)}"
                        "手动" -> "手动创建 · ${TimeFmt.short(n.updatedAt)}"
                        else -> "来自题目 · ${n.bankName ?: ""} · ${TimeFmt.short(n.updatedAt)}"
                    },
                    isDigest = n.source == "归纳稿",
                )
            },
            summaryText = "共 ${notes.size} 条 · " +
                if (settings.lastBackupAt > 0) "上次备份 ${TimeFmt.medium(settings.lastBackupAt)}" else "尚未备份",
            summarizing = summarizingNow,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NoteListUiState(
            items = emptyList(),
            summaryText = "共 0 条 · 尚未备份",
            summarizing = false,
        ),
    )
}
