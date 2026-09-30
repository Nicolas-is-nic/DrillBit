package com.drillbit.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.Banner
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.DBCard
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DBProgressBar
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.DbTopBarInfo
import com.drillbit.ui.components.ExplainCard
import com.drillbit.ui.components.OptionRow
import com.drillbit.ui.components.OptionState
import com.drillbit.ui.components.TagChip
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/**
 * P5 / P6 / P7 / P9 刷题页（单库顺序刷、混合卷、错题重考三模式共用）。
 *
 * 答前：点选项切换选中（主色描边）、主按钮「确认作答」（未选置灰）；答后：判定着色 + 解析常驻 + 「下一题」激活。
 */
@Composable
fun QuizScreen(state: QuizUiState, onEvent: (QuizEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = state.title,
                onBack = { onEvent(QuizEvent.Back) },
                actions = {
                    DbTopBarInfo(
                        text = if (state.finished) {
                            "已完成"
                        } else {
                            "第 ${state.currentIndex} / ${state.totalCount} 题"
                        },
                    )
                },
            )
            DBProgressBar(progress = state.progress, thin = true)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                if (state.finished) {
                    FinishedCard(state = state)
                } else {
                    QuestionContent(state = state, onEvent = onEvent)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                HorizontalDivider(thickness = 1.dp, color = colors.line)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
                ) {
                    if (state.finished) {
                        DBButton(
                            text = "返回",
                            onClick = { onEvent(QuizEvent.Back) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        DBButton(
                            text = "问 AI",
                            onClick = { onEvent(QuizEvent.AskAi) },
                            modifier = Modifier.width(88.dp),
                            type = DBButtonType.GHOST,
                        )
                        Spacer(Modifier.width(10.dp))
                        val awaitingConfirm = state.phase == QuizPhase.ANSWERING
                        DBButton(
                            text = if (awaitingConfirm) "确认作答" else "下一题",
                            onClick = {
                                if (awaitingConfirm) {
                                    onEvent(QuizEvent.ConfirmClick)
                                } else {
                                    onEvent(QuizEvent.Next)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            type = if (state.phase == QuizPhase.ANSWERED ||
                                (awaitingConfirm && state.selectedIndices.isNotEmpty())
                            ) {
                                DBButtonType.PRIMARY
                            } else {
                                DBButtonType.OFF
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 题干、选项、解析与答后提示 */
@Composable
private fun QuestionContent(state: QuizUiState, onEvent: (QuizEvent) -> Unit) {
    val colors = dbColors()
    state.question.sourceBankName?.let { source ->
        TagChip(
            text = "来自：$source",
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    state.answered?.countBannerText?.let { countText ->
        Banner(
            text = countText,
            modifier = Modifier.padding(top = 10.dp),
            type = BannerType.INFO,
        )
    }
    Text(
        text = state.question.stem,
        style = MaterialTheme.typography.titleMedium,
        color = colors.text,
        modifier = Modifier.padding(top = 8.dp),
    )
    Spacer(Modifier.height(12.dp))
    state.question.options.forEachIndexed { index, option ->
        OptionRow(
            label = ('A' + index).toString(),
            text = option,
            state = optionState(state = state, index = index),
            modifier = Modifier.padding(bottom = 8.dp),
            onClick = if (state.phase == QuizPhase.ANSWERING) {
                { onEvent(QuizEvent.OptionClick(index)) }
            } else {
                null
            },
        )
    }
    Spacer(Modifier.height(4.dp))
    state.answered?.let { answered ->
        ExplainCard(title = "解析", body = answered.explanation)
        if (!answered.isCorrect) {
            answered.wrongBannerText?.let { bannerText ->
                Banner(
                    text = bannerText,
                    modifier = Modifier.padding(top = 10.dp),
                    type = BannerType.WARN,
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

/**
 * 选项状态映射：页面只按契约字段决定着色，不判定对错。
 *
 * 答前：multi 已选项 SELECTED，其余 DEFAULT。
 * 答后：正确项 GOOD 优先，误选项 BAD，其余 DISABLED 轻微降噪（不可点）。
 */
private fun optionState(state: QuizUiState, index: Int): OptionState {
    val answered = state.answered
        ?: return if (index in state.selectedIndices) OptionState.SELECTED else OptionState.DEFAULT
    return when {
        index in answered.correctIndices -> OptionState.GOOD
        index in answered.selectedIndices -> OptionState.BAD
        else -> OptionState.DISABLED
    }
}

/**
 * 完成页卡片。
 *
 * 完成页展示本次总题数与答对题数（契约 7.4 的 correctCount）。
 */
@Composable
private fun FinishedCard(state: QuizUiState) {
    val colors = dbColors()
    Spacer(Modifier.height(16.dp))
    DBCard {
        Text(
            text = "本次完成",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "本次共 ${state.totalCount} 题 · 答对 ${state.correctCount} 题",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
    }
}

/** 预览假数据：答前（单库顺序刷） */
private fun previewAnsweringState() = QuizUiState(
    mode = QuizMode.SINGLE,
    title = "大模型基础",
    currentIndex = 12,
    totalCount = 60,
    correctCount = 0,
    progress = 0.2f,
    phase = QuizPhase.ANSWERING,
    question = QuestionUi(
        stem = "在标准 Transformer 中，自注意力机制的计算复杂度随序列长度 n 如何增长（忽略常数因子）？",
        options = listOf("O(n)", "O(n log n)", "O(n²)", "O(n³)"),
        type = QuestionType.SINGLE,
        sourceBankName = null,
    ),
    answered = null,
    selectedIndices = emptyList(),
    finished = false,
)

/** 预览假数据：答后（答错，已入错题集） */
private fun previewAnsweredState() = previewAnsweringState().copy(
    phase = QuizPhase.ANSWERED,
    answered = AnsweredUi(
        selectedIndices = listOf(1),
        correctIndices = listOf(2),
        isCorrect = false,
        explanation = "自注意力要计算 n×n 的注意力分数矩阵 QKᵀ，再与 V 相乘，两项均为 O(n²·d)，" +
            "因此呈平方级增长。这正是长上下文需要稀疏注意力、线性注意力等优化的原因。",
        wrongBannerText = "已加入错题集 · 重考计数 3/3",
        countBannerText = null,
    ),
)

/** 预览假数据：混合卷答对（带来源题库标签） */
private fun previewMixState() = QuizUiState(
    mode = QuizMode.MIX,
    title = "混合卷",
    currentIndex = 7,
    totalCount = 20,
    correctCount = 1,
    progress = 0.35f,
    phase = QuizPhase.ANSWERED,
    question = QuestionUi(
        stem = "ReAct 范式中「行动」与「观察」交替进行，其中观察（Observation）的主要作用是？",
        options = listOf(
            "记录模型的推理轨迹",
            "把工具执行结果回灌给模型",
            "对最终答案做格式校验",
            "触发下一次采样温度调整",
        ),
        type = QuestionType.SINGLE,
        sourceBankName = "Agent 与工具调用",
    ),
    answered = AnsweredUi(
        selectedIndices = listOf(1),
        correctIndices = listOf(1),
        isCorrect = true,
        explanation = "ReAct 循环为「思考 → 行动 → 观察」，观察即把外部工具的返回结果拼回上下文，" +
            "模型据此修正下一步推理，因此它是工具与推理之间的反馈通道。",
        wrongBannerText = null,
        countBannerText = null,
    ),
    selectedIndices = emptyList(),
    finished = false,
)

/** 预览假数据：错题重考（带计数状态条） */
private fun previewRetryState() = QuizUiState(
    mode = QuizMode.RETRY,
    title = "错题重考",
    currentIndex = 3,
    totalCount = 14,
    correctCount = 0,
    progress = 0.21f,
    phase = QuizPhase.ANSWERED,
    question = QuestionUi(
        stem = "LoRA 相比全量微调的主要优势是什么？",
        options = listOf(
            "只训练少量低秩参数，显存与存储开销小",
            "完全不需要反向传播",
            "可以无限扩展上下文长度",
            "训练后不需要推理框架支持",
        ),
        type = QuestionType.SINGLE,
        sourceBankName = null,
    ),
    answered = AnsweredUi(
        selectedIndices = listOf(1),
        correctIndices = listOf(0),
        isCorrect = false,
        explanation = "LoRA 冻结原权重，只训练注入的低秩矩阵，可训练参数通常降到原模型的千分之一量级，" +
            "因此显存占用与权重存储都大幅下降，但前向与反向仍要经过主干网络。",
        wrongBannerText = "已加入错题集 · 重考计数重置为 3/3",
        countBannerText = "本题重考计数 2/3 · 本次答对后变为 1/3",
    ),
    selectedIndices = emptyList(),
    finished = false,
)

/** 预览假数据：本次完成 */
private fun previewFinishedState() = previewAnsweringState().copy(
    finished = true,
    phase = QuizPhase.ANSWERED,
    currentIndex = 20,
    totalCount = 20,
    correctCount = 17,
    progress = 1f,
)

@Preview(name = "刷题 · 答前 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizAnsweringPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewAnsweringState(), onEvent = {})
        }
    }
}

@Preview(name = "刷题 · 答前 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizAnsweringPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewAnsweringState(), onEvent = {})
        }
    }
}

@Preview(name = "刷题 · 答后答错 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizAnsweredPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewAnsweredState(), onEvent = {})
        }
    }
}

@Preview(name = "刷题 · 答后答错 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizAnsweredPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewAnsweredState(), onEvent = {})
        }
    }
}

@Preview(name = "刷题 · 混合卷答对 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizMixPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewMixState(), onEvent = {})
        }
    }
}

@Preview(name = "刷题 · 错题重考 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizRetryPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewRetryState(), onEvent = {})
        }
    }
}

@Preview(name = "刷题 · 本次完成 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizFinishedPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewFinishedState(), onEvent = {})
        }
    }
}
