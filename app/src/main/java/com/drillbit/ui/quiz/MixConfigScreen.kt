package com.drillbit.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.Stepper
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P4 混合抽题配置：勾选多个题库 + 加减号选择题目数量 */
@Composable
fun MixConfigScreen(state: MixConfigUiState, onEvent: (MixConfigEvent) -> Unit) {
    val colors = dbColors()
    val hasSelection = state.banks.any { it.selected }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "混合抽题",
                onBack = { onEvent(MixConfigEvent.Back) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                SectionTitle(text = "选择题库")
                state.banks.forEach { bank ->
                    BankOptionRow(
                        option = bank,
                        onClick = { onEvent(MixConfigEvent.ToggleBank(bank.bankId)) },
                    )
                }
                SectionTitle(text = "题目数量")
                Stepper(
                    value = state.count,
                    onMinus = { onEvent(MixConfigEvent.Minus) },
                    onPlus = { onEvent(MixConfigEvent.Plus) },
                    minValue = state.minCount,
                )
                Text(
                    text = "已到下限 ${state.minCount} 时减号不可再点；步进 ±${state.step}，" +
                        "按题目权重抽取，权重高的题目更容易被抽中",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(top = 9.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
            ) {
                DBButton(
                    text = "开始测试 · ${state.count} 题",
                    onClick = { onEvent(MixConfigEvent.Start) },
                    modifier = Modifier.fillMaxWidth(),
                    type = if (hasSelection) DBButtonType.PRIMARY else DBButtonType.OFF,
                )
            }
        }
    }
}

/** 分组小标题（题目数量 / 选择题库） */
@Composable
private fun SectionTitle(text: String) {
    val colors = dbColors()
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.text2,
        modifier = Modifier.padding(top = 12.dp, bottom = 7.dp),
    )
}

/** 题库勾选行：选中态为主色方块加勾 */
@Composable
private fun BankOptionRow(option: BankOption, onClick: () -> Unit) {
    val colors = dbColors()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (option.selected) colors.primary else colors.card)
                .border(1.dp, if (option.selected) colors.primary else colors.line, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (option.selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = option.name,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${option.questionCount} 题",
            style = MaterialTheme.typography.labelSmall,
            color = colors.text2,
        )
    }
}

/** 预览假数据：勾选两个题库，题数 20 */
private fun previewState() = MixConfigUiState(
    banks = listOf(
        BankOption("bank-llm", "大模型基础", 128, true),
        BankOption("bank-agent", "Agent 与工具调用", 96, true),
        BankOption("bank-tune", "模型微调与部署", 64, false),
    ),
    count = 20,
    minCount = 10,
    step = 10,
)

/** 预览假数据：题数已到下限 10（减号禁用） */
private fun previewMinState() = previewState().copy(count = 10)

/** 预览假数据：未勾选任何题库（开始按钮置灰） */
private fun previewNoSelectionState() = previewState().copy(
    banks = previewState().banks.map { it.copy(selected = false) },
)

@Preview(name = "混合抽题 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun MixConfigPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            MixConfigScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "混合抽题 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun MixConfigPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            MixConfigScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "混合抽题 · 下限 10 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun MixConfigMinPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            MixConfigScreen(state = previewMinState(), onEvent = {})
        }
    }
}

@Preview(name = "混合抽题 · 未选择 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun MixConfigEmptyPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            MixConfigScreen(state = previewNoSelectionState(), onEvent = {})
        }
    }
}
