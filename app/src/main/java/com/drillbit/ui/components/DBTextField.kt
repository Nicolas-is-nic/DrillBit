package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drillbit.ui.theme.dbColors

/**
 * 输入框：标签 + 卡片底色输入区（圆角 10dp）。
 *
 * mono = true 时用等宽字体（接口地址、密钥、模型名）；
 * password = true 时默认掩码显示，并提供「显示 / 隐藏」切换。
 */
@Composable
fun DBTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    password: Boolean = false,
    singleLine: Boolean = true,
    minHeight: Dp = 0.dp,
    maxHeight: Dp = 0.dp,
    placeholder: String = "",
) {
    val colors = dbColors()
    var visible by remember { mutableStateOf(false) }
    val baseStyle = MaterialTheme.typography.bodyMedium
    val textStyle = if (mono) {
        baseStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    } else {
        baseStyle
    }
    val shape = RoundedCornerShape(10.dp)

    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.text2,
            )
            Spacer(Modifier.height(5.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.card2)
                .border(1.dp, colors.line, shape)
                .defaultMinSize(minHeight = minHeight)
                // 多行长文本限高：超出后输入区内部自行滚动，不再把弹窗撑爆
                .then(if (maxHeight > 0.dp) Modifier.heightIn(max = maxHeight) else Modifier)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotBlank()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = colors.text2,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = singleLine,
                    textStyle = textStyle.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                    ),
                    visualTransformation = if (password && !visible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    // BasicTextField 无内置滚动：多行限高时手动加，否则长文本下半段不可见（review M-1）
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!singleLine && maxHeight > 0.dp) {
                                Modifier.verticalScroll(rememberScrollState())
                            } else {
                                Modifier
                            },
                        ),
                )
            }
            if (password) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (visible) "隐藏" else "显示",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    modifier = Modifier.clickable { visible = !visible },
                )
            }
        }
    }
}
