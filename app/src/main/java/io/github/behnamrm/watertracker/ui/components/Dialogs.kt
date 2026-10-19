package io.github.behnamrm.watertracker.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.behnamrm.watertracker.R

/** Dialog asking for an amount in millilitres. */
@Composable
fun AmountDialog(
    title: String,
    confirmLabel: String,
    initialMl: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    range: IntRange = 1..5000,
) {
    var text by rememberSaveable { mutableStateOf(initialMl?.toString().orEmpty()) }
    val value = text.toIntOrNull()
    val valid = value != null && value in range
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.amount_ml)) },
                suffix = { Text("ml") },
                singleLine = true,
                isError = text.isNotEmpty() && !valid,
                supportingText = if (text.isNotEmpty() && !valid) {
                    { Text(stringResource(R.string.amount_invalid, range.first, range.last)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
