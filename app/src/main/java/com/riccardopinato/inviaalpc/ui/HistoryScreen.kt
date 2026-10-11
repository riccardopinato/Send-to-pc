package com.riccardopinato.inviaalpc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.inviaalpc.R
import com.riccardopinato.inviaalpc.transfer.TransferContentKind
import com.riccardopinato.inviaalpc.transfer.TransferDirection
import com.riccardopinato.inviaalpc.transfer.TransferHistoryEntry

@Composable
internal fun RecentsScreen(
    modifier: Modifier,
    history: List<TransferHistoryEntry>,
    error: String?,
    onClear: () -> Unit,
    onDismissError: () -> Unit,
    onOpen: (TransferHistoryEntry) -> Unit,
    onReuse: (TransferHistoryEntry) -> Unit,
    onCopy: (TransferHistoryEntry) -> Unit,
    onShare: (TransferHistoryEntry) -> Unit
) {
    var confirmClear by remember {
        mutableStateOf(false)
    }

    val sessionGroups =
        remember(history) {
            history
                .groupBy {
                    it.sessionId
                        ?: it.id
                }
                .values
                .toList()
        }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = {
                confirmClear = false
            },
            title = {
                Text(
                    tr(
                        "clear_history_title"
                    )
                )
            },
            text = {
                Text(
                    tr(
                        "clear_history_body"
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    }
                ) {
                    Text(tr("clear"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                    }
                ) {
                    Text(tr("cancel"))
                }
            }
        )
    }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = 18.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            ),
        contentPadding =
            PaddingValues(
                bottom = 24.dp
            )
    ) {
        item {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconBadge(
                    iconRes =
                        R.drawable.ic_history,
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer,
                    contentColor =
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                )

                Spacer(
                    Modifier.width(
                        12.dp
                    )
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        tr("recents"),
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        tr("history_note")
                    )
                }

                if (
                    history.isNotEmpty()
                ) {
                    TextButton(
                        onClick = {
                            confirmClear =
                                true
                        }
                    ) {
                        Text(
                            tr("clear")
                        )
                    }
                }
            }
        }

        error?.let {
            item {
                ErrorCard(
                    text = it,
                    onDismiss =
                        onDismissError
                )
            }
        }

        if (history.isEmpty()) {
            item {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {
                    Text(
                        tr(
                            "recent_empty"
                        ),
                        modifier =
                            Modifier.padding(
                                22.dp
                            )
                    )
                }
            }
        } else {
            items(
                items =
                    sessionGroups,
                key = { group ->
                    group.first().sessionId
                        ?: group.first().id
                }
            ) { group ->
                val totalBytes =
                    group
                        .mapNotNull {
                            it.sizeBytes
                        }
                        .sum()

                val latestDate =
                    group.maxOf {
                        it.completedAtMillis
                    }

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {
                        Text(
                            tr(
                                "session_group",
                                group.size,
                                formatBytes(
                                    totalBytes
                                )
                            ),
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            formatHistoryDate(
                                latestDate
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        group.forEachIndexed {
                                index,
                                entry ->
                            if (index > 0) {
                                HorizontalDivider()
                            }

                            HistoryEntryRow(
                                entry =
                                    entry,
                                onOpen =
                                    onOpen,
                                onReuse =
                                    onReuse,
                                onCopy =
                                    onCopy,
                                onShare =
                                    onShare
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HistoryEntryRow(
    entry: TransferHistoryEntry,
    onOpen: (TransferHistoryEntry) -> Unit,
    onReuse: (TransferHistoryEntry) -> Unit,
    onCopy: (TransferHistoryEntry) -> Unit,
    onShare: (TransferHistoryEntry) -> Unit
) {
    val directionLabel =
        if (
            entry.direction ==
                TransferDirection
                    .PHONE_TO_PC
        ) {
            tr("sent")
        } else {
            tr("received")
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 2.dp
                ),
        verticalAlignment =
            Alignment.Top
    ) {
        DirectionBadge(
            direction =
                entry.direction
        )

        Spacer(
            Modifier.width(12.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                when (entry.kind) {
                    TransferContentKind.FILE ->
                        entry.fileName

                    TransferContentKind.TEXT ->
                        tr("text_received")

                    TransferContentKind.LINK ->
                        tr("link_received")
                },
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                buildString {
                    append(
                        directionLabel
                    )

                    entry.sizeBytes
                        ?.let {
                            append(" • ")
                            append(
                                formatBytes(
                                    it
                                )
                            )
                        }
                },
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            entry.textContent
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    Text(
                        it,
                        modifier =
                            Modifier.padding(
                                top = 4.dp
                            ),
                        maxLines = 2,
                        overflow =
                            TextOverflow
                                .Ellipsis,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

            Row(
                modifier =
                    Modifier.padding(
                        top = 4.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    )
            ) {
                when (entry.kind) {
                    TransferContentKind.FILE -> {
                        if (
                            entry.direction ==
                                TransferDirection
                                    .PC_TO_PHONE &&
                            !entry.contentUri
                                .isNullOrBlank()
                        ) {
                            TextButton(
                                onClick = {
                                    onOpen(
                                        entry
                                    )
                                }
                            ) {
                                Text(
                                    tr("open")
                                )
                            }
                        } else if (
                            entry.direction ==
                                TransferDirection
                                    .PHONE_TO_PC &&
                            !entry.contentUri
                                .isNullOrBlank()
                        ) {
                            TextButton(
                                onClick = {
                                    onReuse(
                                        entry
                                    )
                                }
                            ) {
                                Text(
                                    tr(
                                        "send_again"
                                    )
                                )
                            }
                        }
                    }

                    TransferContentKind.TEXT -> {
                        TextButton(
                            onClick = {
                                onCopy(entry)
                            }
                        ) {
                            Text(
                                tr("copy")
                            )
                        }
                    }

                    TransferContentKind.LINK -> {
                        TextButton(
                            onClick = {
                                onOpen(entry)
                            }
                        ) {
                            Text(
                                tr(
                                    "open_link"
                                )
                            )
                        }
                    }
                }

                val canShare =
                    when (entry.kind) {
                        TransferContentKind.FILE ->
                            !entry.contentUri
                                .isNullOrBlank()

                        TransferContentKind.TEXT,
                        TransferContentKind.LINK ->
                            !entry.textContent
                                .isNullOrBlank()
                    }

                if (canShare) {
                    TextButton(
                        onClick = {
                            onShare(entry)
                        }
                    ) {
                        Text(
                            tr("share")
                        )
                    }
                }
            }
        }
    }
}

