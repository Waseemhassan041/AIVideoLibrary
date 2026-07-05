package com.ainest.aivideolibrary.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.ainest.aivideolibrary.data.RECYCLE_BIN_RETENTION_DAYS

@Composable
fun ConfirmDeleteDialog(
    count: Int = 1,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count == 1) "Delete video?" else "Delete $count videos?") },
        text = {
            Text(
                "This moves the entr${if (count == 1) "y" else "ies"} to Recycle Bin for $RECYCLE_BIN_RETENTION_DAYS days, " +
                    "after which they're removed automatically. The original video file on your device is never touched."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmPermanentDeleteDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete permanently?") },
        text = { Text("\"$title\" will be permanently removed from your library. This can't be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete Forever") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
