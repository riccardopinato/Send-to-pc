package com.riccardopinato.inviaalpc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.inviaalpc.AppLanguage
import com.riccardopinato.inviaalpc.AppThemeMode
import com.riccardopinato.inviaalpc.BuildConfig
import com.riccardopinato.inviaalpc.R

@Composable
internal fun SettingsScreen(
    modifier: Modifier,
    language: AppLanguage,
    languageOverrideCode: String?,
    themeMode: AppThemeMode,
    localIp: String?,
    privacyOptionsRequired: Boolean,
    productionAdsConfigured: Boolean,
    onLanguageChange: (AppLanguage?) -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onPrivacyChoices: () -> Unit
) {
    var languageExpanded by remember {
        mutableStateOf(false)
    }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp),
        contentPadding =
            PaddingValues(bottom = 24.dp)
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
                        R.drawable.ic_settings,
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
                    Modifier.width(12.dp)
                )

                Text(
                    tr("settings"),
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        item {
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(18.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        tr("language"),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Box {
                        OutlinedButton(
                            onClick = {
                                languageExpanded =
                                    true
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {
                            Text(
                                if (
                                    languageOverrideCode ==
                                    null
                                ) {
                                    tr(
                                        "system_language"
                                    ) +
                                        " · " +
                                        language.nativeName
                                } else {
                                    language.nativeName
                                }
                            )
                        }

                        DropdownMenu(
                            expanded =
                                languageExpanded,
                            onDismissRequest = {
                                languageExpanded =
                                    false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        tr(
                                            "system_language"
                                        )
                                    )
                                },
                                onClick = {
                                    languageExpanded =
                                        false
                                    onLanguageChange(
                                        null
                                    )
                                }
                            )

                            AppLanguage.entries
                                .forEach {
                                    candidate ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                candidate
                                                    .nativeName
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
                }
            }
        }

        item {
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(18.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        tr("theme"),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement
                                .spacedBy(8.dp)
                    ) {
                        listOf(
                            AppThemeMode.SYSTEM to
                                tr("theme_system"),
                            AppThemeMode.LIGHT to
                                tr("theme_light"),
                            AppThemeMode.DARK to
                                tr("theme_dark")
                        ).forEach {
                            (mode, label) ->
                            FilterChip(
                                selected =
                                    themeMode ==
                                        mode,
                                onClick = {
                                    onThemeChange(
                                        mode
                                    )
                                },
                                label = {
                                    Text(label)
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(18.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tr("network_help_title"),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        localIp?.let {
                            tr(
                                "network_ready",
                                it
                            )
                        } ?: tr(
                            "network_unavailable"
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            if (localIp != null) {
                                MaterialTheme
                                    .colorScheme
                                    .secondary
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .error
                            }
                    )

                    Text(
                        tr("network_help_body"),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        "1. " +
                            tr(
                                "network_help_step1"
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Text(
                        "2. " +
                            tr(
                                "network_help_step2"
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Text(
                        "3. " +
                            tr(
                                "network_help_step3"
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Text(
                        tr(
                            "network_help_hotspot"
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
                }
            }
        }

        item {
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(18.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tr("ads_title"),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        tr("ads_description"),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        if (
                            productionAdsConfigured
                        ) {
                            tr(
                                "ads_production_mode"
                            )
                        } else {
                            tr(
                                "ads_test_mode"
                            )
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

                    if (
                        privacyOptionsRequired
                    ) {
                        FilledTonalButton(
                            onClick =
                                onPrivacyChoices
                        ) {
                            Text(
                                tr(
                                    "privacy_choices"
                                )
                            )
                        }
                    } else {
                        Text(
                            tr(
                                "privacy_not_required"
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            }
        }

        item {
            Text(
                tr(
                    "version",
                    BuildConfig.VERSION_NAME
                ),
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

