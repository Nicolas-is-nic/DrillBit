package com.drillbit.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.repo.NoteRepository
import com.drillbit.data.db.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 笔记编辑 ViewModel：加载/保存/删除；来源题目跳转由导航层处理（sourceBankId() 提供目标）。
 */
class NoteEditViewModel(private val noteId: String) : ViewModel() {

    private val repo: NoteRepository = ServiceLocator.noteRepository

    /** 原笔记（加载完成前为 null） */
    private var original: NoteEntity? = null

    private val stateFlow = MutableStateFlow(
        NoteEditUiState(
            noteId = noteId,
            title = "",
            content = "",
            sourceQuestionText = null,
            deleteConfirmVisible = false,
        ),
    )
    val state: StateFlow<NoteEditUiState> = stateFlow.asStateFlow()

    init {
        if (noteId != "new") {
            viewModelScope.launch {
                val note = repo.getNote(noteId.toLongOrNull() ?: -1L)
                if (note != null) {
                    original = note
                    stateFlow.value = stateFlow.value.copy(
                        title = note.title,
                        content = note.content,
                        sourceQuestionText = note.sourceQuestionId?.let { qid ->
                            val bankId = qid.substringBefore(':', "")
                            val bank = ServiceLocator.bankRepository.getBank(bankId)
                            val question = ServiceLocator.database.questionDao().getById(qid)
                            if (question != null) {
                                "${bank?.name ?: ""} · 第 ${question.orderIndex + 1} 题"
                            } else {
                                null
                            }
                        },
                    )
                }
            }
        }
    }

    fun onEvent(event: NoteEditEvent) {
        when (event) {
            is NoteEditEvent.TitleChange -> stateFlow.value = stateFlow.value.copy(title = event.text)
            is NoteEditEvent.ContentChange -> stateFlow.value = stateFlow.value.copy(content = event.text)
            NoteEditEvent.Save -> Unit // 由导航层 scope.launch { vm.save(); pop }（review F-5）
            NoteEditEvent.DeleteClick -> stateFlow.value = stateFlow.value.copy(deleteConfirmVisible = true)
            NoteEditEvent.DeleteCancel -> stateFlow.value = stateFlow.value.copy(deleteConfirmVisible = false)
            NoteEditEvent.DeleteConfirm -> Unit // 同上，导航层等落库再退栈
            else -> Unit // Back / SourceClick 由导航层处理
        }
    }

    /** 保存：suspend 供导航层等待落库后再退栈（review F-5：抢跑取消协程曾静默丢笔记） */
    suspend fun save() {
        val current = stateFlow.value
        if (current.title.isBlank() && current.content.isBlank()) return
        repo.saveNote(
            noteId = if (noteId == "new") 0L else noteId.toLongOrNull() ?: 0L,
            title = current.title.ifBlank { "无标题笔记" },
            content = current.content,
            source = original?.source ?: "手动",
            sourceQuestionId = original?.sourceQuestionId,
            bankName = original?.bankName,
        )
    }

    /** 删除：同 save，等落库再退栈 */
    suspend fun delete() {
        repo.deleteNote(noteId.toLongOrNull() ?: -1L)
        stateFlow.value = stateFlow.value.copy(deleteConfirmVisible = false)
    }

    /** 来源题目跳转目标：返回 bankId（questionId 形如 "bankId:qid"），无来源返回 null */
    fun sourceBankId(): String? {
        val qid = original?.sourceQuestionId ?: return null
        return qid.substringBefore(':', "").ifBlank { null }
    }

    class Factory(private val noteId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NoteEditViewModel(noteId) as T
    }
}
