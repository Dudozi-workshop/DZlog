package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@Composable
fun DDZTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(12.dp),
        textStyle = typography.Body,
        label = label?.let {
            {
                Text(
                    text = it,
                    style = typography.Secondary,
                )
            }
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    style = typography.Body,
                    color = colors.TextSecondary,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.TextPrimary,
            unfocusedTextColor = colors.TextPrimary,
            disabledTextColor = colors.TextDisabled,
            focusedBorderColor = colors.Primary,
            unfocusedBorderColor = colors.Border,
            disabledBorderColor = colors.Border,
            focusedLabelColor = colors.PrimaryDark,
            unfocusedLabelColor = colors.TextSecondary,
            cursorColor = colors.PrimaryDark,
            focusedContainerColor = colors.Surface,
            unfocusedContainerColor = colors.Surface,
            disabledContainerColor = colors.SurfaceSoft,
        ),
    )
}
