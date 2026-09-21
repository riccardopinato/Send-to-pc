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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopinato.inviaalpc.BuildConfig
import com.riccardopinato.inviaalpc.R
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
    }

    LaunchedEffect(session?.id) {
        if (
            session != null &&
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    if (uiState.showOnboarding) {
        FirstRunDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = viewModel::dismissOnboarding
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    resId = R.drawable.ic_logo,
                                    contentDescription = null,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                "Invia al PC",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )

                            NetworkStatusLine(
                                localIp = uiState.localIp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::showOnboarding
                    ) {
                        AppIcon(
                            resId = R.drawable.ic_help,
                            contentDescription = "Guida",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (session == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.section == HomeSection.SEND,
                        onClick = {
                            viewModel.setSection(HomeSection.SEND)
                        },
                        icon = {
                            AppIcon(
                                R.drawable.ic_send,
                                "Invia"
                            )
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
                            AppIcon(
                                R.drawable.ic_receive,
                                "Ricevi"
                            )
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
                            AppIcon(
                                R.drawable.ic_history,
                                "Recenti"
                            )
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
                            AppIcon(
                                R.drawable.ic_devices,
                                "PC fidati"
                            )
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
                        onDismissError = viewModel::clearError,
                        onStart = viewModel::startSendSession
                    )
                }

                HomeSection.RECEIVE -> {
                    ReceiveScreen(
                        modifier = Modifier.padding(padding),
                        localIp = uiState.localIp,
                        preparing = uiState.preparing,
                        error = uiState.error,
                        onRefresh = viewModel::refreshNetwork,
                        onDismissError = viewModel::clearError,
                        onStart = viewModel::startReceiveSession
                    )
                }

                HomeSection.RECENTS -> {
                    RecentsScreen(
                        modifier = Modifier.padding(padding),
                        history = history,
                        error = uiState.error,
                        onClear = viewModel::clearHistory,
                        onDismissError = viewModel::clearError,
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
    onDismissError: () -> Unit,
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
            PageHero(
                eyebrow = "TELEFONO → PC",
                title = "Invia in pochi secondi",
                subtitle = "Scegli i contenuti, crea il QR e aprilo dal browser del PC.",
                iconRes = R.drawable.ic_send,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        item {
            Text(
                "Aggiungi contenuti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                    AppIcon(
                        R.drawable.ic_file,
                        null,
                        Modifier.size(22.dp)
                    )
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
                    AppIcon(
                        R.drawable.ic_photo,
                        null,
                        Modifier.size(22.dp)
                    )
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
                        AppIcon(
                            R.drawable.ic_link,
                            null,
                            Modifier.size(20.dp)
                        )
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
                ErrorCard(
                    text = it,
                    onDismiss = onDismissError
                )
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
                AppIcon(
                    R.drawable.ic_qr,
                    null,
                    Modifier.size(22.dp)
                )
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(11.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                R.drawable.ic_file,
                                null,
                                Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

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
                        },
                        modifier =
                            Modifier.semantics {
                                contentDescription =
                                    "Rimuovi " +
                                        item.displayName
                            }
                    ) {
                        AppIcon(
                            R.drawable.ic_close,
                            null,
                            Modifier.size(19.dp)
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
    localIp: String?,
    preparing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onStart: () -> Unit
) {
    val networkAvailable =
        localIp != null
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            PageHero(
                eyebrow = "PC → TELEFONO",
                title = "Ricevi senza cavi",
                subtitle = "Apri la pagina locale dal computer e trascina i file.",
                iconRes = R.drawable.ic_receive,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (networkAvailable) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                )
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color =
                                if (networkAvailable) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    resId =
                                        if (networkAvailable) {
                                            R.drawable.ic_check
                                        } else {
                                            R.drawable.ic_close
                                        },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            if (networkAvailable) {
                            "Rete locale pronta"
                        } else {
                            "Wi-Fi non disponibile"
                        },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        if (networkAvailable) {
                            "Telefono raggiungibile su " +
                                localIp +
                                ". I file ricevuti saranno salvati in Download/Invia al PC."
                        } else {
                            "Collega telefono e computer alla stessa Wi-Fi. Se usi una VPN, un hotspot o una rete ospiti, prova a disattivarli."
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
                ErrorCard(
                    text = it,
                    onDismiss = onDismissError
                )
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
                AppIcon(
                    R.drawable.ic_qr,
                    null,
                    Modifier.size(22.dp)
                )
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
    var confirmStop by remember {
        mutableStateOf(false)
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = {
                confirmStop = false
            },
            title = {
                Text("Interrompere il trasferimento?")
            },
            text = {
                Text(
                    "Il file in corso resterà riprendibile quando possibile, ma la sessione corrente verrà chiusa."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmStop = false
                        onStop()
                    }
                ) {
                    Text("Termina")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmStop = false
                    }
                ) {
                    Text("Continua")
                }
            }
        )
    }

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
            PageHero(
                eyebrow = "SESSIONE LOCALE",
                title = "Connessione pronta",
                subtitle =
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
                iconRes = R.drawable.ic_qr,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(10.dp))

            SessionStatusChip(session.status)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        QrCodeView(
                            text = session.url,
                            modifier =
                                Modifier
                                    .padding(14.dp)
                                    .size(224.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "PIN DI ACCESSO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            session.pin,
                            modifier =
                                Modifier.padding(
                                    horizontal = 24.dp,
                                    vertical = 10.dp
                                ),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Scade tra " +
                            formatCountdown(remainingSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        AppIcon(
                            R.drawable.ic_copy,
                            null,
                            Modifier.size(20.dp)
                        )
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
                    AppIcon(
                        R.drawable.ic_open,
                        null,
                        Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Apri ultimo file ricevuto")
                }
            }
        }

        item {
            Text(
                "Connessione diretta sulla rete locale. Nessun file viene caricato su server esterni.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                onClick = {
                    if (
                        session.status ==
                        TransferStatus.TRANSFERRING
                    ) {
                        confirmStop = true
                    } else {
                        onStop()
                    }
                },
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

                val totalText =
                    progress.totalBytes?.let {
                        " • " +
                            formatBytes(progress.transferredBytes) +
                            " / " +
                            formatBytes(it)
                    }.orEmpty()

                val etaText =
                    progress.totalBytes
                        ?.takeIf {
                            progress.bytesPerSecond > 0L &&
                                progress.transferredBytes < it
                        }
                        ?.let {
                            val seconds =
                                (it - progress.transferredBytes) /
                                    progress.bytesPerSecond

                            " • circa " +
                                formatDuration(seconds)
                        }
                        .orEmpty()

                Text(
                    percent.toString() +
                        "% • " +
                        formatBytes(progress.bytesPerSecond) +
                        "/s" +
                        totalText +
                        etaText
                )
            }
        }
    }
}

@Composable
private fun RecentsScreen(
    modifier: Modifier,
    history: List<TransferHistoryEntry>,
    error: String?,
    onClear: () -> Unit,
    onDismissError: () -> Unit,
    onOpen: (TransferHistoryEntry) -> Unit,
    onReuse: (TransferHistoryEntry) -> Unit
) {
    var confirmClear by remember {
        mutableStateOf(false)
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = {
                confirmClear = false
            },
            title = {
                Text("Cancellare la cronologia?")
            },
            text = {
                Text(
                    "Verrà rimossa solo la cronologia locale. I file trasferiti non saranno eliminati."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    }
                ) {
                    Text("Cancella")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

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
                IconBadge(
                    iconRes = R.drawable.ic_history,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.width(12.dp))

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
                    TextButton(
                        onClick = {
                            confirmClear = true
                        }
                    ) {
                        Text("Cancella")
                    }
                }
            }
        }

        error?.let {
            item {
                ErrorCard(
                    text = it,
                    onDismiss = onDismissError
                )
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
                        DirectionBadge(
                            direction = entry.direction
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
                                            AppIcon(
                                                R.drawable.ic_open,
                                                null,
                                                Modifier.size(18.dp)
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
                                            AppIcon(
                                                R.drawable.ic_refresh,
                                                null,
                                                Modifier.size(18.dp)
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
private fun FirstRunDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            R.drawable.ic_logo,
                            null,
                            Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    "Invia al PC",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Trasferisci file, foto, testo e link direttamente tra telefono e PC."
                )

                OnboardingStep(
                    number = "1",
                    title = "Stessa rete",
                    text = "Telefono e PC devono essere collegati alla stessa Wi-Fi locale."
                )

                OnboardingStep(
                    number = "2",
                    title = "Crea la sessione",
                    text = "Scegli cosa inviare oppure avvia Ricevi, poi mostra il QR."
                )

                OnboardingStep(
                    number = "3",
                    title = "Apri dal PC",
                    text = "Scansiona il QR o apri l'indirizzo nel browser. Il PIN protegge la sessione."
                )

                Text(
                    "Nessun account, nessun cloud. I file restano sulla rete locale.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "Versione " + versionName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Inizia")
            }
        }
    )
}

@Composable
private fun OnboardingStep(
    number: String,
    title: String,
    text: String
) {
    Row(
        horizontalArrangement =
            Arrangement.spacedBy(12.dp),
        verticalAlignment =
            Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color =
                MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                number,
                modifier =
                    Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                fontWeight = FontWeight.Black,
                color =
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column {
            Text(
                title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppIcon(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier
    )
}

@Composable
private fun NetworkStatusLine(
    localIp: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(7.dp),
            shape = CircleShape,
            color =
                if (localIp != null) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.error
                }
        ) {}

        Spacer(Modifier.width(6.dp))

        Text(
            text =
                localIp?.let {
                    "Rete locale • " + it
                } ?: "Wi-Fi locale non disponibile",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PageHero(
    eyebrow: String,
    title: String,
    subtitle: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = contentColor.copy(alpha = 0.10f),
                contentColor = contentColor
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        resId = iconRes,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                eyebrow,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = contentColor.copy(alpha = 0.72f)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun IconBadge(
    iconRes: Int,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                resId = iconRes,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DirectionBadge(
    direction: TransferDirection
) {
    val sending =
        direction == TransferDirection.PHONE_TO_PC

    IconBadge(
        iconRes =
            if (sending) {
                R.drawable.ic_send
            } else {
                R.drawable.ic_receive
            },
        containerColor =
            if (sending) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        contentColor =
            if (sending) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
    )
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
                AppIcon(
                    R.drawable.ic_check,
                    null,
                    Modifier.size(18.dp)
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
    var confirmClear by remember {
        mutableStateOf(false)
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = {
                confirmClear = false
            },
            title = {
                Text("Revocare tutti i PC?")
            },
            text = {
                Text(
                    "Tutti i PC fidati dovranno usare nuovamente il PIN per essere associati."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    }
                ) {
                    Text("Revoca tutti")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

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
                IconBadge(
                    iconRes = R.drawable.ic_devices,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(Modifier.width(12.dp))

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
                        onClick = {
                            confirmClear = true
                        }
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
                        IconBadge(
                            iconRes = R.drawable.ic_devices,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                        IconBadge(
                            iconRes = R.drawable.ic_devices,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                            },
                            modifier =
                                Modifier.semantics {
                                    contentDescription =
                                        "Revoca " +
                                            device.name
                                }
                        ) {
                            AppIcon(
                                R.drawable.ic_close,
                                null,
                                Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    text: String,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.errorContainer
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme.onErrorContainer
            )

            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Chiudi")
                }
            }
        }
    }
}

private fun formatCountdown(seconds: Long): String =
    "%02d:%02d".format(
        Locale.US,
        seconds / 60L,
        seconds % 60L
    )

private fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)

    return when {
        safe < 60L ->
            safe.toString() + " s"

        safe < 3600L ->
            (safe / 60L).toString() +
                " min " +
                (safe % 60L).toString() +
                " s"

        else ->
            (safe / 3600L).toString() +
                " h " +
                ((safe % 3600L) / 60L).toString() +
                " min"
    }
}

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
