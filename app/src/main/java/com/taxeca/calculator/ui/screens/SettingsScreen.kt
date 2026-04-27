package com.taxeca.calculator.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
fun SettingsScreen(languageManager: LanguageManager) {
    val freemiumVm = LocalFreemiumViewModel.current
    val isPremium  by freemiumVm.isPremium.collectAsStateWithLifecycle()
    val isFrench   by languageManager.isFrench.collectAsStateWithLifecycle()
    val context    = LocalContext.current
    val activity   = context as? Activity

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {

        // ── Language ──────────────────────────────────────────────────────────
        _SectionHeader(stringResource(R.string.settings_language))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    languageManager.setLanguage(LanguageManager.LANG_FR)
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
                Icon(Icons.Outlined.Language, null, modifier = Modifier.padding(end = 6.dp))
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
                Icon(Icons.Outlined.Language, null, modifier = Modifier.padding(end = 6.dp))
                Text("English", fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        // ── Premium ───────────────────────────────────────────────────────────
        _SectionHeader("Premium")
        if (isPremium) {
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.Verified, null,
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
            _SettingsTile(
                icon     = Icons.Outlined.Star,
                label    = stringResource(R.string.premium_buy_btn),
                subtitle = stringResource(R.string.settings_premium_subtitle),
                onClick  = { activity?.let { freemiumVm.buyPremium(it) } }
            )
            _SettingsTile(
                icon    = Icons.Outlined.Restore,
                label   = stringResource(R.string.premium_restore_btn),
                onClick = { activity?.let { freemiumVm.restorePurchases(it) } }
            )
        }
        HorizontalDivider()

        // ── Support ───────────────────────────────────────────────────────────
        _SectionHeader(stringResource(R.string.settings_support))
        _SettingsTile(
            icon    = Icons.Outlined.Email,
            label   = stringResource(R.string.settings_contact),
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@calqwise.com")
                    putExtra(Intent.EXTRA_SUBJECT, "TaxeCA Support")
                }
                context.startActivity(intent)
            }
        )
        _SettingsTile(
            icon    = Icons.Outlined.PrivacyTip,
            label   = stringResource(R.string.settings_privacy),
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://calqwise.com/privacy"))
                )
            }
        )
        HorizontalDivider()

        // ── About ─────────────────────────────────────────────────────────────
        _SectionHeader(stringResource(R.string.settings_about))
        _SettingsTile(
            icon     = Icons.Outlined.Apps,
            label    = stringResource(R.string.settings_other_apps),
            subtitle = "calqwise.com",
            onClick  = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://calqwise.com"))
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
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun _SectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun _SettingsTile(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            leadingContent  = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(label) },
            supportingContent = subtitle?.let { { Text(it) } },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
    }
}
