package com.stockmaster.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmaster.app.ui.theme.BorderLight
import com.stockmaster.app.ui.theme.GreenPrimary
import com.stockmaster.app.ui.theme.TextMuted
import com.stockmaster.app.ui.theme.TextPrimary
import com.stockmaster.app.ui.theme.TextSecondary
import com.stockmaster.app.util.ScannerGun

/**
 * 统一风格输入框（圆角 + 边框），并上报焦点给扫码枪监听器。
 */
@Composable
fun SMTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    height: Dp = 40.dp,
    fontSize: TextUnit = 13.sp,
    bold: Boolean = false,
    mono: Boolean = false,
    textColor: Color = TextPrimary,
    textAlign: TextAlign = TextAlign.Start,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    borderColor: Color = BorderLight.copy(alpha = 0.6f)
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)
    val focusModifier = if (focused) {
        Modifier
            .background(GreenPrimary.copy(alpha = 0.08f), RoundedCornerShape(13.dp))
            .padding(2.dp)
    } else Modifier

    Box(
        modifier = modifier
            .height(height)
            .then(focusModifier)
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) GreenPrimary else borderColor,
                shape = shape
            )
            .padding(horizontal = 12.dp)
            .onFocusChanged { state ->
                focused = state.isFocused
                ScannerGun.textInputFocused = state.isFocused
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Box(modifier = Modifier.padding(end = 8.dp), contentAlignment = Alignment.Center) { leading() }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = fontSize,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    textAlign = textAlign
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                singleLine = singleLine,
                minLines = minLines,
                cursorBrush = SolidColor(GreenPrimary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = when (textAlign) {
                            TextAlign.Center -> Alignment.Center
                            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
                            else -> Alignment.CenterStart
                        }
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TextMuted,
                                fontSize = fontSize,
                                textAlign = textAlign,
                                maxLines = if (singleLine) 1 else Int.MAX_VALUE
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (trailing != null) {
                Box(modifier = Modifier.padding(start = 8.dp), contentAlignment = Alignment.Center) { trailing() }
            }
        }
    }
}

@Composable
fun SMNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    height: Dp = 40.dp,
    fontSize: TextUnit = 13.sp,
    bold: Boolean = false,
    mono: Boolean = true,
    textColor: Color = TextPrimary,
    textAlign: TextAlign = TextAlign.Start,
    decimal: Boolean = false,
    borderColor: Color = BorderLight.copy(alpha = 0.6f)
) {
    SMTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        height = height,
        fontSize = fontSize,
        bold = bold,
        mono = mono,
        textColor = textColor,
        textAlign = textAlign,
        keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        imeAction = ImeAction.Done,
        borderColor = borderColor
    )
}

@Composable
fun SMTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minHeight: Dp = 80.dp,
    fontSize: TextUnit = 12.sp
) {
    var focused by remember { mutableStateOf(false) }
    val areaShape = RoundedCornerShape(12.dp)
    val areaFocusModifier = if (focused) {
        Modifier
            .background(GreenPrimary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(2.dp)
    } else Modifier

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .then(areaFocusModifier)
            .clip(areaShape)
            .background(Color.White)
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) GreenPrimary else BorderLight.copy(alpha = 0.6f),
                shape = areaShape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .onFocusChanged { state ->
                focused = state.isFocused
                ScannerGun.textInputFocused = state.isFocused
            }
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = fontSize,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(GreenPrimary),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextMuted,
                            fontSize = fontSize
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun FieldLabel(text: String, color: Color = TextSecondary) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}