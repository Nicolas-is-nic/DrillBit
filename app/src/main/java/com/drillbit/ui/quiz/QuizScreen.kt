package com.drillbit.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
import com.drillbit.ui.components.ScrimModal
import com.drillbit.ui.components.Segmented
import com.drillbit.ui.components.TagChip
import com.drillbit.ui.components.TagChipType
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors
import java.io.File

/**
 * P5 / P6 / P7 / P9 刷题页（单库顺序刷、混合卷、错题重考、收藏四模式共用）。
 *
 * 选择题：答前点选项切换选中、主按钮「确认作答」；答后判定着色 + 解析常驻 + 「下一题」。
 * recall 回忆卡（2026-10-08）：答前默想（题干+题图+弱提示），「查看思路」揭示；
 * 揭示后自评双钮「记住了/没记住」，没记住出横幅后「下一题」。题图点按进全屏双指缩放。
 */
@Composable
fun QuizScreen(state: QuizUiState, onEvent: (QuizEvent) -> Unit) {
    val colors = dbColors()
    // 全屏题图查看路径（recall 专用，null=不展示）
    var fullscreenImage by remember { mutableStateOf<String?>(null) }
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
                    QuestionContent(
                        state = state,
                        onEvent = onEvent,
                        onImageClick = { fullscreenImage = it },
                    )
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
                    } else if (state.question.type == QuestionType.RECALL) {
                        RecallBottomBar(state = state, onEvent = onEvent)
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

        fullscreenImage?.let { path ->
            ImageFullscreenOverlay(path = path, onDismiss = { fullscreenImage = null })
        }
    }

    if (state.confirmDelete) {
        DeleteConfirmDialog(onEvent = onEvent)
    }
}

/** recall 底部三态：默想（问AI+查看思路）/ 待自评（记住了+没记住）/ 已自评没记住（下一题） */
@Composable
private fun RowScope.RecallBottomBar(state: QuizUiState, onEvent: (QuizEvent) -> Unit) {
    val awaitingSelfEval = state.phase == QuizPhase.ANSWERED &&
        state.answered?.wrongBannerText == null &&
        state.answered?.countBannerText == null
    when {
        state.phase == QuizPhase.ANSWERING -> {
            DBButton(
                text = "问 AI",
                onClick = { onEvent(QuizEvent.AskAi) },
                modifier = Modifier.width(88.dp),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(10.dp))
            DBButton(
                text = "查看思路",
                onClick = { onEvent(QuizEvent.ConfirmClick) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.PRIMARY,
            )
        }
        awaitingSelfEval -> {
            DBButton(
                text = "记住了",
                onClick = { onEvent(QuizEvent.RememberedClick) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.OK,
            )
            Spacer(Modifier.width(10.dp))
            DBButton(
                text = "没记住",
                onClick = { onEvent(QuizEvent.ForgotClick) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.DANGER,
            )
        }
        else -> {
            DBButton(
                text = "下一题",
                onClick = { onEvent(QuizEvent.Next) },
                modifier = Modifier.fillMaxWidth(),
                type = DBButtonType.PRIMARY,
            )
        }
    }
}

/** 删除二次确认弹窗（F1）：本地删题不入回收站，同步重建后过滤仍生效 */
@Composable
private fun DeleteConfirmDialog(onEvent: (QuizEvent) -> Unit) {
    val colors = dbColors()
    ScrimModal {
        Text(
            text = "删除本题",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "本地删除，不再出现在刷题、混合抽题与错题集；题库同步更新后也不会恢复。",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DBButton(
                text = "取消",
                onClick = { onEvent(QuizEvent.DeleteCancel) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "删除",
                onClick = { onEvent(QuizEvent.DeleteConfirm) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.WARN,
            )
        }
    }
}

/** 题干、选项、解析与答后提示（选择题）；recall 题分流到 RecallContent */
@Composable
private fun QuestionContent(
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit,
    onImageClick: (String) -> Unit,
) {
    if (state.question.type == QuestionType.RECALL) {
        RecallContent(state = state, onEvent = onEvent, onImageClick = onImageClick)
        return
    }
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
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = state.question.stem,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
        )
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = if (state.question.isFavorite) "取消收藏" else "收藏本题",
            tint = if (state.question.isFavorite) colors.primary else colors.off,
            modifier = Modifier
                .padding(start = 8.dp, top = 10.dp)
                .size(24.dp)
                .clickable { onEvent(QuizEvent.FavoriteClick) },
        )
    }
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
 * recall 题内容：默想态（题干+题图+弱提示+停顿提示）；揭示态（+思路卡+代码卡+问AI链）。
 * 自评「没记住」后的横幅复用选择题的 WARN banner。
 */
@Composable
private fun RecallContent(
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit,
    onImageClick: (String) -> Unit,
) {
    val colors = dbColors()
    state.question.sourceBankName?.let { source ->
        TagChip(
            text = "来自：$source",
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    // 重考计数横幅（自评后出现）：与选择题同位，题干上方
    state.answered?.countBannerText?.let { countText ->
        Banner(
            text = countText,
            modifier = Modifier.padding(top = 10.dp),
            type = BannerType.INFO,
        )
    }
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = state.question.stem,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
        )
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = if (state.question.isFavorite) "取消收藏" else "收藏本题",
            tint = if (state.question.isFavorite) colors.primary else colors.off,
            modifier = Modifier
                .padding(start = 8.dp, top = 10.dp)
                .size(24.dp)
                .clickable { onEvent(QuizEvent.FavoriteClick) },
        )
    }
    Spacer(Modifier.height(10.dp))
    // 题图：默想与揭示两阶段均常驻（复习思路时可回看）
    QuestionImages(images = state.question.images, onImageClick = onImageClick)
    // 弱提示标签：难度类（Hard/Medium/Easy）用警示色，其余普通 chip
    if (state.question.tags.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.question.tags.forEach { tag ->
                val isDifficulty = tag.equals("Hard", ignoreCase = true) ||
                    tag.equals("Medium", ignoreCase = true) ||
                    tag.equals("Easy", ignoreCase = true)
                TagChip(text = tag, type = if (isDifficulty) TagChipType.WARN else TagChipType.NORMAL)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (state.phase == QuizPhase.ANSWERING) {
        Text(
            text = "先在心里完整过一遍：用什么结构、每一步做什么、如何收尾。想清楚后再点下方按钮对照。",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
            modifier = Modifier.padding(top = 4.dp),
        )
    } else {
        state.question.recall?.let { recall ->
            Spacer(Modifier.height(6.dp))
            RecallIdeaCard(recall = recall)
            RecallCodeCard(pseudocode = recall.pseudocode, code = recall.code)
            // 问 AI 入口：揭示态底栏为自评双钮，问答入口移到卡内文字链
            Text(
                text = "哪一步没想通？问 AI",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable { onEvent(QuizEvent.AskAi) },
            )
        }
        // 自评「没记住」后的错题横幅（isCorrect 恒 true，须在此独立渲染）
        state.answered?.wrongBannerText?.let { bannerText ->
            Banner(
                text = bannerText,
                modifier = Modifier.padding(top = 10.dp),
                type = BannerType.WARN,
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}

/** 题图列表：白色圆角容器（双主题可读），缺失文件显示占位；点击进全屏 */
@Composable
private fun QuestionImages(images: List<String>, onImageClick: (String) -> Unit) {
    if (images.isEmpty()) return
    val colors = dbColors()
    val shape = RoundedCornerShape(8.dp)
    images.forEachIndexed { index, path ->
        val file = File(path)
        if (file.exists()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(shape)
                    .background(Color.White)
                    .border(1.dp, colors.line, shape)
                    .clickable { onImageClick(path) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = "图 ${index + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Text(
                    text = "图 ${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(72.dp)
                    .clip(shape)
                    .background(colors.card2)
                    .border(1.dp, colors.line, shape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "题图未下载（题库版本更新后重新同步可恢复）",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.text2,
                )
            }
        }
    }
}

/** 全屏题图：双指缩放平移，单击遮罩或图片关闭 */
@Composable
private fun ImageFullscreenOverlay(path: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        offset += panChange
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dbColors().scrim)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = "题图全屏",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                )
                .transformable(transformState),
        )
        Text(
            text = "双指缩放 · 单击关闭",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
        )
    }
}

/** recall 思路卡：策略句 + 编号步骤 + 复杂度 chips（视觉同 ExplainCard 家族） */
@Composable
private fun RecallIdeaCard(recall: RecallUi) {
    val colors = dbColors()
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Text(
            text = "思 路",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = colors.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = recall.strategy,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
        )
        Spacer(Modifier.height(8.dp))
        recall.steps.forEachIndexed { index, step ->
            Row(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    modifier = Modifier
                        .padding(end = 8.dp, top = 2.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(colors.chip)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 21.sp),
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (recall.timeCx.isNotBlank() || recall.spaceCx.isNotBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                if (recall.timeCx.isNotBlank()) TagChip(text = "时间 ${recall.timeCx}")
                if (recall.spaceCx.isNotBlank()) TagChip(text = "空间 ${recall.spaceCx}")
            }
        }
    }
}

/** recall 代码卡：默认折叠；双版本时 segmented「伪代码 | Python」默认伪代码 */
@Composable
private fun RecallCodeCard(pseudocode: String?, code: String?) {
    if (pseudocode == null && code == null) return
    val colors = dbColors()
    var expanded by remember { mutableStateOf(false) }
    var segIndex by remember { mutableStateOf(0) }
    val bothPresent = pseudocode != null && code != null
    val showing = if (bothPresent && segIndex == 1) code else pseudocode ?: code
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(shape)
            .background(colors.card2)
            .border(1.dp, colors.line, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 13.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "参考代码",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.text2,
            )
            Text(
                text = if (expanded) "收起" else "展开",
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
            )
        }
        if (expanded) {
            if (bothPresent) {
                Segmented(
                    options = listOf("伪代码", "Python"),
                    selected = segIndex,
                    onSelect = { segIndex = it },
                    modifier = Modifier.padding(horizontal = 13.dp),
                )
            }
            Text(
                text = showing.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = colors.text,
                modifier = Modifier
                    .padding(horizontal = 13.dp, vertical = 10.dp)
                    .horizontalScroll(rememberScrollState()),
            )
        }
    }
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
        isFavorite = false,
        sourceBankName = null,
    ),
    answered = null,
    selectedIndices = emptyList(),
    confirmDelete = false,
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

/** 预览假数据：recall 默想态（弱提示标签，无题图文件时渲染占位） */
private fun previewRecallAnsweringState() = QuizUiState(
    mode = QuizMode.SINGLE,
    title = "算法回忆卡 · 单调栈",
    currentIndex = 3,
    totalCount = 20,
    correctCount = 0,
    progress = 0.15f,
    phase = QuizPhase.ANSWERING,
    question = QuestionUi(
        stem = "LC 84 · 柱状图中最大的矩形：给定 n 个非负整数表示柱状图各柱宽度为 1、高度为 heights[i]，求能勾勒出的矩形最大面积。",
        options = emptyList(),
        type = QuestionType.RECALL,
        isFavorite = false,
        sourceBankName = null,
        tags = listOf("题眼：单调栈", "Hard"),
        images = listOf("/data/mock/lc84.png"),
        recall = null,
    ),
    answered = null,
    selectedIndices = emptyList(),
    confirmDelete = false,
    finished = false,
)

/** 预览假数据：recall 揭示态（思路卡 + 代码卡 + 自评双钮） */
private fun previewRecallAnsweredState() = previewRecallAnsweringState().copy(
    phase = QuizPhase.ANSWERED,
    question = previewRecallAnsweringState().question.copy(
        recall = RecallUi(
            strategy = "单调栈（栈内高度递增）",
            steps = listOf(
                "栈存下标，底到顶对应高度严格递增",
                "当前柱比栈顶矮则弹栈顶结算：高为弹出柱高，宽为 i 减新栈顶再减 1（栈空则宽为 i）",
                "末尾加高度 0 的哨兵柱，强制清空栈收尾",
            ),
            timeCx = "O(n)",
            spaceCx = "O(n)",
            pseudocode = "stack = 空栈\nans = 0\nh 末尾追加高度 0\nfor 每根柱 i:\n    while 栈非空 且 h[栈顶] >= h[i]: 结算\n    i 入栈",
            code = "def largestRectangleArea(h):\n    stack, ans = [], 0\n    h.append(0)\n    for i, cur in enumerate(h):\n        ...",
        ),
    ),
    answered = AnsweredUi(
        selectedIndices = emptyList(),
        correctIndices = emptyList(),
        isCorrect = true,
        explanation = "",
        wrongBannerText = null,
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
        isFavorite = false,
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
    confirmDelete = false,
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
        isFavorite = false,
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
    confirmDelete = false,
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

@Preview(name = "recall · 默想 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizRecallAnsweringPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewRecallAnsweringState(), onEvent = {})
        }
    }
}

@Preview(name = "recall · 揭示 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun QuizRecallAnsweredPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            QuizScreen(state = previewRecallAnsweredState(), onEvent = {})
        }
    }
}
