package com.taxeca.calculator.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.taxeca.calculator.ui.viewmodel.SettingsViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.BuildConfig
import com.taxeca.calculator.R
import com.taxeca.calculator.data.repository.LanguageManager
import com.taxeca.calculator.ui.navigation.LocalFreemiumViewModel

@Composable
fun SettingsScreen(
    languageManager: LanguageManager,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val freemiumVm = LocalFreemiumViewModel.current
    val isPremium  by freemiumVm.isPremium.collectAsStateWithLifecycle()
    val isFrench   by languageManager.isFrench.collectAsStateWithLifecycle()
    val context    = LocalContext.current
    val activity   = context as? android.app.Activity

    LaunchedEffect(Unit) { viewModel.logScreenView() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {

        // ── Language ──────────────────────────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_language))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    languageManager.setLanguage(LanguageManager.LANG_FR)
                    // Recreate activity to apply new locale via attachBaseContext
                    activity?.recreate()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isFrench) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.surface,
                    contentColor   = if (isFrench) MaterialTheme.colorScheme.onPrimary
                                     else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Outlined.Language, contentDescription = stringResource(R.string.desc_language_icon), modifier = Modifier.padding(end = 6.dp))
                Text("Français", fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = {
                    languageManager.setLanguage(LanguageManager.LANG_EN)
                    activity?.recreate()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (!isFrench) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.surface,
                    contentColor   = if (!isFrench) MaterialTheme.colorScheme.onPrimary
                                     else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Outlined.Language, contentDescription = stringResource(R.string.desc_language_icon), modifier = Modifier.padding(end = 6.dp))
                Text("English", fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        // ── Premium ───────────────────────────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_premium_section))
        if (isPremium) {
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.Verified,
                        contentDescription = stringResource(R.string.desc_verified_icon),
                        tint = MaterialTheme.colorScheme.primary)
                },
                headlineContent = {
                    Text(stringResource(R.string.premium_active_label),
                        fontWeight = FontWeight.Medium)
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_premium_subtitle))
                }
            )
        } else {
            SettingsTile(
                icon     = Icons.Outlined.Star,
                label    = stringResource(R.string.premium_buy_btn),
                subtitle = stringResource(R.string.settings_premium_subtitle),
                onClick  = { activity?.let { freemiumVm.buyPremium(it) } }
            )
            SettingsTile(
                icon    = Icons.Outlined.Restore,
                label   = stringResource(R.string.premium_restore_btn),
                onClick = { activity?.let { freemiumVm.restorePurchases(it) } }
            )
        }
        HorizontalDivider()

        // ── Support ───────────────────────────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_support))
        SettingsTile(
            icon    = Icons.Outlined.Email,
            label   = stringResource(R.string.settings_contact),
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(context.getString(R.string.url_support_email))
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.support_email_subject))
                }
                context.startActivity(intent)
            }
        )
        SettingsTile(
            icon    = Icons.Outlined.PrivacyTip,
            label   = stringResource(R.string.settings_privacy),
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_privacy_policy)))
                )
            }
        )
        HorizontalDivider()

        // ── About ─────────────────────────────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_about))
        SettingsTile(
            icon     = Icons.Outlined.Apps,
            label    = stringResource(R.string.settings_other_apps),
            subtitle = stringResource(R.string.settings_website_display),
            onClick  = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_website)))
                )
            }
        )
        ListItem(
            headlineContent = {
                Text(
                    text  = "TaxeCA v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Text(
            text     = stringResource(R.string.settings_disclaimer),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    } // end Box
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val iconDesc = when (icon) {
        Icons.Outlined.Email -> stringResource(R.string.desc_email_icon)
        Icons.Outlined.PrivacyTip -> stringResource(R.string.desc_privacy_icon)
        Icons.Outlined.Apps -> stringResource(R.string.desc_apps_icon)
        Icons.Outlined.Restore -> stringResource(R.string.desc_restore_icon)
        Icons.Outlined.Star -> stringResource(R.string.desc_star_icon)
        else -> label
    }
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            leadingContent  = { Icon(icon, contentDescription = iconDesc, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(label) },
            supportingContent = subtitle?.let { { Text(it) } },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.desc_arrow_right_icon),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
    }
}
