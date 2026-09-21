package com.riccardopinato.inviaalpc.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.inviaalpc.HomeSection
import com.riccardopinato.inviaalpc.TransferViewModel
import com.riccardopinato.inviaalpc.transfer.SharedItem
import com.riccardopinato.inviaalpc.transfer.TransferDirection
import com.riccardopinato.inviaalpc.transfer.TransferHistoryEntry
import com.riccardopinato.inviaalpc.transfer.TransferProgress
import com.riccardopinato.inviaalpc.transfer.TransferSession
import com.riccardopinato.inviaalpc.transfer.TransferStatus
import com.riccardopinato.inviaalpc.transfer.TrustedDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviaAlPcApp(
    viewModel: TransferViewModel
) {
    val session by viewModel.session.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val history by viewModel.history.collectAsState()
    val trustedDevices by viewModel.trustedDevices.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val filePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            viewModel.addUris(uris)
        }

    val photoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(50)
        ) { uris ->
            viewModel.addUris(uris)
        }

    val notificationPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

    LaunchedEffect(Unit) {
        viewModel.refreshNetwork()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Invia al PC",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            if (uiState.localIp != null) {
                                "Rete locale pronta"
                            } else {
                                "Wi-Fi non disponibile"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (uiState.localIp != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (session == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = uiState.section == HomeSection.SEND,
                        onClick = {
                            viewModel.setSection(HomeSection.SEND)
                        },
                        icon = {
                            Icon(Icons.Default.Send, null)
                        },
                        label = {
                            Text("Invia")
                        }
                    )

                    NavigationBarItem(
                        selected = uiState.section == HomeSection.RECEIVE,
                        onClick = {
                            viewModel.setSection(HomeSection.RECEIVE)
                        },
                        icon = {
                            Icon(Icons.Default.SwapVert, null)
                        },
                        label = {
                            Text("Ricevi")
                        }
                    )

                    NavigationBarItem(
                        selected = uiState.section == HomeSection.RECENTS,
                        onClick = {
                            viewModel.setSection(HomeSection.RECENTS)
                        },
                        icon = {
                            Icon(Icons.Default.History, null)
                        },
                        label = {
                            Text("Recenti")
                        }
                    )

                    NavigationBarItem(
                        selected = uiState.section == HomeSection.DEVICES,
                        onClick = {
                            viewModel.setSection(HomeSection.DEVICES)
                        },
                        icon = {
                            Icon(Icons.Default.Computer, null)
                        },
                        label = {
                            Text("PC")
                        }
                    )
                }
            }
        }
    ) { padding ->
        val activeSession = session

        if (activeSession != null) {
            ActiveSessionScreen(
                modifier = Modifier.padding(padding),
                session = activeSession,
                progress = progress,
                history = history,
                remainingSeconds = uiState.remainingSeconds,
                onOpenRecent = viewModel::openHistoryEntry,
                onStop = viewModel::stopSession
            )
        } else {
            when (uiState.section) {
                HomeSection.SEND -> {
                    SendScreen(
                        modifier = Modifier.padding(padding),
                        items = uiState.selectedItems,
                        text = uiState.sharedText,
                        link = uiState.sharedLink,
                        preparing = uiState.preparing,
                        error = uiState.error,
                        onPickFiles = {
                            filePicker.launch(arrayOf("*/*"))
                        },
                        onPickPhotos = {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onRemove = viewModel::removeItem,
                        onTextChange = viewModel::setSharedText,
                        onLinkChange = viewModel::setSharedLink,
                        onStart = viewModel::startSendSession
                    )
                }

                HomeSection.RECEIVE -> {
                    ReceiveScreen(
                        modifier = Modifier.padding(padding),
                        networkAvailable = uiState.localIp != null,
                        preparing = uiState.preparing,
                        error = uiState.error,
                        onRefresh = viewModel::refreshNetwork,
                        onStart = viewModel::startReceiveSession
                    )
                }

                HomeSection.RECENTS -> {
                    RecentsScreen(
                        modifier = Modifier.padding(padding),
                        history = history,
                        onClear = viewModel::clearHistory,
                        onOpen = viewModel::openHistoryEntry,
                        onReuse = viewModel::reuseHistoryEntry
                    )
                }

                HomeSection.DEVICES -> {
                    DevicesScreen(
                        modifier = Modifier.padding(padding),
                        devices = trustedDevices,
                        onRemove = viewModel::removeTrustedDevice,
                        onClear = viewModel::clearTrustedDevices
                    )
                }
            }
        }
    }
}

@Composable
private fun SendScreen(
    modifier: Modifier,
    items: List<SharedItem>,
    text: String,
    link: String,
    preparing: Boolean,
    error: String?,
    onPickFiles: () -> Unit,
    onPickPhotos: () -> Unit,
    onRemove: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onLinkChange: (String) -> Unit,
    onStart: () -> Unit
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Cosa vuoi inviare?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Il PC dovrà solo aprire il QR nel browser.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onPickFiles,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                ) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("File")
                }

                FilledTonalButton(
                    onClick = onPickPhotos,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Foto")
                }
            }
        }

        if (items.isNotEmpty()) {
            item {
                SelectedFilesCard(
                    items = items,
                    onRemove = onRemove
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        "Testo",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8,
                        placeholder = {
                            Text("Scrivi o incolla del testo...")
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Link",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = link,
                        onValueChange = onLinkChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Uri
                            ),
                        placeholder = {
                            Text("https://...")
                        }
                    )
                }
            }
        }

        error?.let {
            item {
                ErrorCard(it)
            }
        }

        item {
            Button(
                onClick = onStart,
                enabled = !preparing,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.QrCode2, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (preparing) {
                        "Preparazione..."
                    } else {
                        "Crea QR"
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectedFilesCard(
    items: List<SharedItem>,
    onRemove: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                items.size.toString() + " elementi selezionati",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            items.take(20).forEach { item ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            item.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        item.sizeBytes?.let {
                            Text(
                                formatBytes(it),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onRemove(item.id)
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Rimuovi"
                        )
                    }
                }
            }

            if (items.size > 20) {
                Text(
                    "+ " + (items.size - 20) + " altri file",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ReceiveScreen(
    modifier: Modifier,
    networkAvailable: Boolean,
    preparing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onStart: () -> Unit
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Ricevi dal PC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Apri la pagina locale sul computer e trascina i file.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Text(
                        if (networkAvailable) {
                            "Rete locale pronta"
                        } else {
                            "Wi-Fi non disponibile"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        if (networkAvailable) {
                            "I file ricevuti saranno salvati in Download/Invia al PC."
                        } else {
                            "Collega telefono e computer alla stessa rete Wi-Fi."
                        }
                    )

                    if (!networkAvailable) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onRefresh) {
                            Text("Controlla di nuovo")
                        }
                    }
                }
            }
        }

        error?.let {
            item {
                ErrorCard(it)
            }
        }

        item {
            Button(
                onClick = onStart,
                enabled = networkAvailable && !preparing,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
            ) {
                Icon(Icons.Default.QrCode2, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (preparing) {
                        "Preparazione..."
                    } else {
                        "Avvia ricezione"
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveSessionScreen(
    modifier: Modifier,
    session: TransferSession,
    progress: TransferProgress?,
    history: List<TransferHistoryEntry>,
    remainingSeconds: Long,
    onOpenRecent: (TransferHistoryEntry) -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Sessione attiva",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            SessionStatusChip(session.status)

            Text(
                when (session.status) {
                    TransferStatus.WAITING ->
                        "In attesa del PC. Scansiona il QR o copia l'indirizzo."
                    TransferStatus.CONNECTED ->
                        "PC collegato e autenticato."
                    TransferStatus.TRANSFERRING ->
                        "Trasferimento in corso."
                    TransferStatus.COMPLETED ->
                        "Trasferimento completato. La sessione resta aperta."
                    TransferStatus.ERROR ->
                        "La rete è cambiata o la sessione non è più raggiungibile."
                    TransferStatus.EXPIRED ->
                        "La sessione è scaduta."
                    TransferStatus.STOPPED ->
                        "Sessione terminata."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QrCodeView(
                        text = session.url,
                        modifier = Modifier.size(250.dp)
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "PIN",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        session.pin,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        "Scade tra " +
                            formatCountdown(remainingSeconds)
                    )

                    Spacer(Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = {
                            val clipboard =
                                context.getSystemService(
                                    Context.CLIPBOARD_SERVICE
                                ) as ClipboardManager

                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Invia al PC",
                                    session.url
                                )
                            )

                            Toast.makeText(
                                context,
                                "Indirizzo copiato",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copia indirizzo")
                    }
                }
            }
        }

        progress?.let { current ->
            item {
                TransferProgressCard(current)
            }
        }

        val latestReceived =
            history.firstOrNull {
                it.direction == TransferDirection.PC_TO_PHONE &&
                    !it.contentUri.isNullOrBlank()
            }

        if (
            session.status == TransferStatus.COMPLETED &&
            latestReceived != null
        ) {
            item {
                Button(
                    onClick = {
                        onOpenRecent(latestReceived)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apri ultimo file ricevuto")
                }
            }
        }

        item {
            Text(
                session.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            FilledTonalButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Termina sessione")
            }
        }
    }
}

@Composable
private fun TransferProgressCard(
    progress: TransferProgress
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                if (progress.direction == TransferDirection.PHONE_TO_PC) {
                    "Invio al PC"
                } else {
                    "Ricezione dal PC"
                },
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                progress.fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            progress.percentage?.let { percent ->
                LinearProgressIndicator(
                    progress = {
                        percent / 100f
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    percent.toString() +
                        "% • " +
                        formatBytes(progress.bytesPerSecond) +
                        "/s"
                )
            }
        }
    }
}

@Composable
private fun RecentsScreen(
    modifier: Modifier,
    history: List<TransferHistoryEntry>,
    onClear: () -> Unit,
    onOpen: (TransferHistoryEntry) -> Unit,
    onReuse: (TransferHistoryEntry) -> Unit
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Recenti",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Viene salvata solo la cronologia, non una copia dei file."
                    )
                }

                if (history.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Cancella")
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Nessun trasferimento effettuato.",
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
        } else {
            items(
                items = history,
                key = { it.id }
            ) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (
                                entry.direction ==
                                TransferDirection.PHONE_TO_PC
                            ) {
                                "↑"
                            } else {
                                "↓"
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                entry.fileName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                buildString {
                                    append(
                                        if (
                                            entry.direction ==
                                            TransferDirection.PHONE_TO_PC
                                        ) {
                                            "Inviato"
                                        } else {
                                            "Ricevuto"
                                        }
                                    )

                                    entry.sizeBytes?.let {
                                        append(" • ")
                                        append(formatBytes(it))
                                    }

                                    append(" • ")
                                    append(formatHistoryDate(entry.completedAtMillis))
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!entry.contentUri.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (
                                        entry.direction ==
                                        TransferDirection.PC_TO_PHONE
                                    ) {
                                        TextButton(
                                            onClick = {
                                                onOpen(entry)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Apri")
                                        }
                                    } else {
                                        TextButton(
                                            onClick = {
                                                onReuse(entry)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Invia di nuovo")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStatusChip(
    status: TransferStatus
) {
    val label =
        when (status) {
            TransferStatus.WAITING -> "In attesa"
            TransferStatus.CONNECTED -> "PC collegato"
            TransferStatus.TRANSFERRING -> "Trasferimento"
            TransferStatus.COMPLETED -> "Completato"
            TransferStatus.EXPIRED -> "Scaduta"
            TransferStatus.STOPPED -> "Terminata"
            TransferStatus.ERROR -> "Errore rete"
        }

    AssistChip(
        onClick = {},
        label = {
            Text(label)
        },
        leadingIcon = {
            if (status == TransferStatus.COMPLETED) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

private fun formatHistoryDate(
    timestamp: Long
): String =
    SimpleDateFormat(
        "dd MMM, HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))

@Composable
private fun DevicesScreen(
    modifier: Modifier,
    devices: List<TrustedDevice>,
    onRemove: (TrustedDevice) -> Unit,
    onClear: () -> Unit
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "PC fidati",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "I PC ricordati possono riconnettersi senza PIN quando il browser conserva il token locale.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (devices.isNotEmpty()) {
                    TextButton(
                        onClick = onClear
                    ) {
                        Text("Revoca tutti")
                    }
                }
            }
        }

        if (devices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp)
                    ) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Nessun PC fidato",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Collega un PC con il PIN, poi usa “Ricorda questo PC” dalla pagina web."
                        )
                    }
                }
            }
        } else {
            items(
                items = devices,
                key = { it.id }
            ) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                device.name,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                "Ultimo accesso • " +
                                    formatHistoryDate(device.lastSeenAtMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                onRemove(device)
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Revoca PC"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(text: String) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.errorContainer
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            color =
                MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

private fun formatCountdown(seconds: Long): String =
    "%02d:%02d".format(
        Locale.US,
        seconds / 60L,
        seconds % 60L
    )

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return bytes.toString() + " B"

    val kb = bytes / 1024.0
    if (kb < 1024.0) {
        return "%.1f KB".format(Locale.US, kb)
    }

    val mb = kb / 1024.0
    if (mb < 1024.0) {
        return "%.1f MB".format(Locale.US, mb)
    }

    return "%.2f GB".format(
        Locale.US,
        mb / 1024.0
    )
}
