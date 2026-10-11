package com.riccardopinato.inviaalpc.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.ContextCompat
import com.riccardopinato.inviaalpc.AppLanguage
import com.riccardopinato.inviaalpc.AppThemeMode
import com.riccardopinato.inviaalpc.AppLanguageStore
import com.riccardopinato.inviaalpc.AppText
import com.riccardopinato.inviaalpc.BuildConfig
import com.riccardopinato.inviaalpc.R
import com.riccardopinato.inviaalpc.HomeSection
import com.riccardopinato.inviaalpc.TransferViewModel
import com.riccardopinato.inviaalpc.monetization.AdaptiveBannerAd
import com.riccardopinato.inviaalpc.monetization.MonetizationController
import com.riccardopinato.inviaalpc.transfer.SharedItem
import com.riccardopinato.inviaalpc.transfer.TransferDirection
import com.riccardopinato.inviaalpc.transfer.TransferHistoryEntry
import com.riccardopinato.inviaalpc.transfer.TransferOperationSummary
import com.riccardopinato.inviaalpc.transfer.TransferProgress
import com.riccardopinato.inviaalpc.transfer.TransferSession
import com.riccardopinato.inviaalpc.transfer.TransferStatus
import com.riccardopinato.inviaalpc.transfer.TrustedDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LOCAL_NETWORK_PERMISSION =
    "android.permission.ACCESS_LOCAL_NETWORK"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviaAlPcApp(
    viewModel: TransferViewModel
) {
    val session by viewModel.session.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val history by viewModel.history.collectAsState()
    val operationSummary by viewModel.operationSummary.collectAsState()
    val trustedDevices by viewModel.trustedDevices.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val monetizationState by
        MonetizationController.state
            .collectAsState()
    val context = LocalContext.current
    val activity =
        context as? Activity

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

    val localNetworkPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                viewModel.startSendSession()
            } else {
                viewModel.reportLocalNetworkPermissionDenied()
            }
        }

    fun startConnection() {
        val requiresRuntimePermission =
            Build.VERSION.SDK_INT >= 37 &&
                ContextCompat.checkSelfPermission(
                    context,
                    LOCAL_NETWORK_PERMISSION
                ) != PackageManager.PERMISSION_GRANTED

        if (requiresRuntimePermission) {
            localNetworkPermission.launch(
                LOCAL_NETWORK_PERMISSION
            )
        } else {
            viewModel.startSendSession()
        }
    }

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
            language = uiState.language,
            languageOverrideCode = uiState.languageOverrideCode,
            onLanguageChange = viewModel::setLanguageOverride,
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
                                tr("app_name"),
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
                            contentDescription = tr("help"),
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
                                tr("connect")
                            )
                        },
                        label = {
                            Text(tr("connect"))
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
                                tr("recents")
                            )
                        },
                        label = {
                            Text(tr("recents"))
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
                                tr("trusted_pcs")
                            )
                        },
                        label = {
                            Text(tr("pc"))
                        }
                    )

                    NavigationBarItem(
                        selected = uiState.section == HomeSection.SETTINGS,
                        onClick = {
                            viewModel.setSection(HomeSection.SETTINGS)
                        },
                        icon = {
                            AppIcon(
                                R.drawable.ic_settings,
                                tr("settings")
                            )
                        },
                        label = {
                            Text(tr("settings"))
                        }
                    )
                }
            }
        }
    ) { padding ->
        val activeSession = session

        if (activeSession != null) {
            ActiveSessionScreen(
                modifier =
                    Modifier.padding(
                        padding
                    ),
                session =
                    activeSession,
                progress =
                    progress,
                history =
                    history,
                operationSummary =
                    operationSummary,
                remainingSeconds =
                    uiState.remainingSeconds,
                onOpenRecent =
                    viewModel::openHistoryEntry,
                onStop =
                    viewModel::stopSession,
                onCompletedStop = {
                    val summary =
                        operationSummary

                    viewModel.stopSession()

                    if (
                        activity != null &&
                        summary != null
                    ) {
                        MonetizationController
                            .showInterstitialIfEligible(
                                activity,
                                summary
                            )
                    }
                }
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
            ) {
                AdaptiveBannerAd(
                    adsEnabled =
                        monetizationState
                            .canRequestAds &&
                            monetizationState
                                .mobileAdsInitialized,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 4.dp
                            )
                )

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                ) {
                    when (uiState.section) {
                        HomeSection.SEND,
                        HomeSection.RECEIVE -> {
                            SendScreen(
                                modifier =
                                    Modifier.fillMaxSize(),
                                items =
                                    uiState.selectedItems,
                                text =
                                    uiState.sharedText,
                                link =
                                    uiState.sharedLink,
                                preparing =
                                    uiState.preparing,
                                error =
                                    uiState.error,
                                onPickFiles = {
                                    filePicker.launch(
                                        arrayOf("*/*")
                                    )
                                },
                                onPickPhotos = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts
                                                .PickVisualMedia
                                                .ImageOnly
                                        )
                                    )
                                },
                                onRemove =
                                    viewModel::removeItem,
                                onTextChange =
                                    viewModel::setSharedText,
                                onLinkChange =
                                    viewModel::setSharedLink,
                                onDismissError =
                                    viewModel::clearError,
                                onStart = {
                                    startConnection()
                                }
                            )
                        }

                        HomeSection.RECENTS -> {
                            RecentsScreen(
                                modifier =
                                    Modifier.fillMaxSize(),
                                history =
                                    history,
                                error =
                                    uiState.error,
                                onClear =
                                    viewModel::clearHistory,
                                onDismissError =
                                    viewModel::clearError,
                                onOpen =
                                    viewModel::openHistoryEntry,
                                onReuse =
                                    viewModel::reuseHistoryEntry
                            )
                        }

                        HomeSection.DEVICES -> {
                            DevicesScreen(
                                modifier =
                                    Modifier.fillMaxSize(),
                                devices =
                                    trustedDevices,
                                onRemove =
                                    viewModel::removeTrustedDevice,
                                onClear =
                                    viewModel::clearTrustedDevices
                            )
                        }

                        HomeSection.SETTINGS -> {
                            SettingsScreen(
                                modifier =
                                    Modifier.fillMaxSize(),
                                language =
                                    uiState.language,
                                languageOverrideCode =
                                    uiState.languageOverrideCode,
                                themeMode =
                                    uiState.themeMode,
                                privacyOptionsRequired =
                                    monetizationState
                                        .privacyOptionsRequired,
                                productionAdsConfigured =
                                    monetizationState
                                        .productionConfigured,
                                onLanguageChange =
                                    viewModel::setLanguageOverride,
                                onThemeChange =
                                    viewModel::setThemeMode,
                                onPrivacyChoices = {
                                    activity?.let {
                                        MonetizationController
                                            .showPrivacyOptions(
                                                it
                                            )
                                    }
                                }
                            )
                        }
                    }
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
                eyebrow = tr("phone_pc"),
                title = tr("connect_devices"),
                subtitle = tr("connect_subtitle"),
                iconRes = R.drawable.ic_devices,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        item {
            Text(
                tr("phone_content_optional"),
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
                    Text(tr("file"))
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
                    Text(tr("photo"))
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
                        tr("text"),
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
                            Text(tr("text_hint"))
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
                            tr("link"),
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
                        tr("preparing")
                    } else {
                        tr("connect_pc")
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
                tr("selected_count", items.size),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            items.take(20).forEach { item ->
                val removeDescription =
                    tr(
                        "remove",
                        item.displayName
                    )

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
                                    removeDescription
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
                    tr("more_files", items.size - 20),
                    style = MaterialTheme.typography.bodySmall
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
    operationSummary: TransferOperationSummary?,
    remainingSeconds: Long,
    onOpenRecent: (TransferHistoryEntry) -> Unit,
    onStop: () -> Unit,
    onCompletedStop: () -> Unit
) {
    val context = LocalContext.current
    val appNameLabel = tr("app_name")
    val addressCopiedLabel =
        tr("address_copied")

    var confirmStop by remember {
        mutableStateOf(false)
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = {
                confirmStop = false
            },
            title = {
                Text(tr("end_transfer_title"))
            },
            text = {
                Text(
                    tr("end_transfer_body")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmStop = false
                        onStop()
                    }
                ) {
                    Text(tr("end"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmStop = false
                    }
                ) {
                    Text(tr("continue"))
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
                eyebrow = tr("session_local"),
                title = tr("connection_ready"),
                subtitle =
                    when (session.status) {
                        TransferStatus.WAITING ->
                            tr("waiting_pc")
                        TransferStatus.CONNECTED ->
                            tr("pc_connected")
                        TransferStatus.TRANSFERRING ->
                            tr("transferring")
                        TransferStatus.COMPLETED ->
                            tr("completed_open")
                        TransferStatus.ERROR ->
                            tr("network_changed")
                        TransferStatus.EXPIRED ->
                            tr("session_expired")
                        TransferStatus.STOPPED ->
                            tr("session_stopped")
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
                        tr("pin_access"),
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
                        tr(
                            "expires_in",
                            formatCountdown(remainingSeconds)
                        ),
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
                                    appNameLabel,
                                    session.url
                                )
                            )

                            Toast.makeText(
                                context,
                                addressCopiedLabel,
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
                        Text(tr("copy_address"))
                    }
                }
            }
        }

        progress?.let { current ->
            item {
                TransferProgressCard(current)
            }
        }

        if (
            session.status ==
                TransferStatus.COMPLETED &&
            operationSummary != null &&
            operationSummary.completedFiles > 0
        ) {
            item {
                TransferSummaryCard(
                    summary =
                        operationSummary
                )
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
                    Text(tr("open_last_received"))
                }
            }
        }

        item {
            Text(
                tr("direct_local"),
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
                    when (
                        session.status
                    ) {
                        TransferStatus.TRANSFERRING ->
                            confirmStop = true

                        TransferStatus.COMPLETED ->
                            onCompletedStop()

                        else ->
                            onStop()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (
                        session.status ==
                        TransferStatus.COMPLETED
                    ) {
                        tr("done")
                    } else {
                        tr("end_session")
                    }
                )
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
                    tr("progress_send")
                } else {
                    tr("progress_receive")
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

                            " • " +
                                tr(
                                    "about_time",
                                    formatDuration(seconds)
                                )
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
private fun TransferSummaryCard(
    summary: TransferOperationSummary
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .secondaryContainer
            )
    ) {
        Column(
            modifier =
                Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            Text(
                tr("transfer_summary"),
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
            )

            Text(
                tr(
                    "files_count",
                    summary.completedFiles
                ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
            )

            Text(
                tr(
                    "transferred_size",
                    formatBytes(
                        summary.completedBytes
                    )
                ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
            )

            Text(
                tr(
                    "elapsed_time",
                    formatDuration(
                        summary.durationMillis /
                            1000L
                    )
                ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
            )
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
                Text(tr("clear_history_title"))
            },
            text = {
                Text(
                    tr("clear_history_body")
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
                        tr("recents"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        tr("history_note")
                    )
                }

                if (history.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            confirmClear = true
                        }
                    ) {
                        Text(tr("clear"))
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
                        tr("recent_empty"),
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
        } else {
            items(
                items = history,
                key = { it.id }
            ) { entry ->
                val directionLabel =
                    if (
                        entry.direction ==
                        TransferDirection.PHONE_TO_PC
                    ) {
                        tr("sent")
                    } else {
                        tr("received")
                    }

                val historyDateLabel =
                    formatHistoryDate(
                        entry.completedAtMillis
                    )

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
                                        directionLabel
                                    )

                                    entry.sizeBytes?.let {
                                        append(" • ")
                                        append(formatBytes(it))
                                    }

                                    append(" • ")
                                    append(
                                        historyDateLabel
                                    )
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
                                            Text(tr("open"))
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
                                            Text(tr("send_again"))
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
    language: AppLanguage,
    languageOverrideCode: String?,
    onLanguageChange: (AppLanguage?) -> Unit,
    onDismiss: () -> Unit
) {
    var languageExpanded by remember {
        mutableStateOf(false)
    }

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
                    tr("app_name"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    tr("onboarding_intro")
                )

                OnboardingStep(
                    number = "1",
                    title =
                        tr(
                            "onboarding_step1_title"
                        ),
                    text =
                        tr(
                            "onboarding_step1_body"
                        )
                )

                OnboardingStep(
                    number = "2",
                    title =
                        tr(
                            "onboarding_step2_title"
                        ),
                    text =
                        tr(
                            "onboarding_step2_body"
                        )
                )

                OnboardingStep(
                    number = "3",
                    title =
                        tr(
                            "onboarding_step3_title"
                        ),
                    text =
                        tr(
                            "onboarding_step3_body"
                        )
                )

                Text(
                    tr("onboarding_privacy"),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    tr("unsafe_wifi"),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.error
                )

                HorizontalDivider()

                Text(
                    tr("language"),
                    fontWeight = FontWeight.Bold
                )

                Box {
                    OutlinedButton(
                        onClick = {
                            languageExpanded = true
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                languageOverrideCode == null
                            ) {
                                tr("system_language") +
                                    " · " +
                                    language.nativeName
                            } else {
                                language.nativeName
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = {
                            languageExpanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    tr("system_language")
                                )
                            },
                            onClick = {
                                languageExpanded = false
                                onLanguageChange(null)
                            }
                        )

                        AppLanguage.entries.forEach {
                            candidate ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        candidate.nativeName
                                    )
                                },
                                onClick = {
                                    languageExpanded =
                                        false
                                    onLanguageChange(
                                        candidate
                                    )
                                }
                            )
                        }
                    }
                }

                Text(
                    tr(
                        "version",
                        versionName
                    ),
                    style =
                        MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(tr("start"))
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
private fun tr(
    key: String,
    vararg args: Any
): String {
    val context = LocalContext.current
    return AppText.get(
        AppLanguageStore.current(context),
        key,
        *args
    )
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
                    tr(
                        "network_ready",
                        it
                    )
                } ?: tr("network_unavailable"),
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
            TransferStatus.WAITING -> tr("status_waiting")
            TransferStatus.CONNECTED -> tr("status_connected")
            TransferStatus.TRANSFERRING -> tr("status_transferring")
            TransferStatus.COMPLETED -> tr("status_completed")
            TransferStatus.EXPIRED -> tr("status_expired")
            TransferStatus.STOPPED -> tr("status_stopped")
            TransferStatus.ERROR -> tr("status_error")
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

@Composable
private fun formatHistoryDate(
    timestamp: Long
): String {
    val context = LocalContext.current
    val language =
        AppLanguageStore.current(
            context
        )

    return SimpleDateFormat(
        "dd MMM, HH:mm",
        Locale.forLanguageTag(
            language.code
        )
    ).format(Date(timestamp))
}

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
                Text(tr("revoke_all_title"))
            },
            text = {
                Text(
                    tr("revoke_all_body")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    }
                ) {
                    Text(tr("revoke_all"))
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
                        tr("trusted_pcs"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        tr("trusted_note"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (devices.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            confirmClear = true
                        }
                    ) {
                        Text(tr("revoke_all"))
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
                            tr("trusted_empty"),
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            tr("trusted_empty_body")
                        )
                    }
                }
            }
        } else {
            items(
                items = devices,
                key = { it.id }
            ) { device ->
                val lastSeenLabel =
                    tr(
                        "last_seen",
                        formatHistoryDate(
                            device.lastSeenAtMillis
                        )
                    )

                val revokeDescription =
                    tr(
                        "revoke",
                        device.name
                    )

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
                                lastSeenLabel,
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
                                        revokeDescription
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
