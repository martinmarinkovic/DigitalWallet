package com.threemdroid.digitalwallet.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.threemdroid.digitalwallet.R

object SettingsLegalRoutes {
    const val privacyPolicy = "settings/privacy-policy"
    const val terms = "settings/terms"
}

fun NavGraphBuilder.settingsPrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = SettingsLegalRoutes.privacyPolicy) {
        LegalDocumentScreen(
            title = stringResource(id = R.string.settings_privacy_policy_title),
            sections = listOf(
                LegalSectionUiModel(
                    heading = stringResource(id = R.string.privacy_policy_section_data_title),
                    body = stringResource(id = R.string.privacy_policy_section_data_body)
                ),
                LegalSectionUiModel(
                    heading = stringResource(id = R.string.privacy_policy_section_permissions_title),
                    body = stringResource(id = R.string.privacy_policy_section_permissions_body)
                ),
                LegalSectionUiModel(
                    heading = stringResource(id = R.string.privacy_policy_section_backup_title),
                    body = stringResource(id = R.string.privacy_policy_section_backup_body)
                ),
                LegalSectionUiModel(
                    heading = stringResource(id = R.string.privacy_policy_section_retention_title),
                    body = stringResource(id = R.string.privacy_policy_section_retention_body)
                )
            ),
            onNavigateBack = onNavigateBack
        )
    }
}

fun NavGraphBuilder.settingsTermsScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = SettingsLegalRoutes.terms) {
        LegalDocumentScreen(
            title = stringResource(id = R.string.settings_terms_title),
            sections = listOf(
                LegalSectionUiModel(
                    heading = stringResource(id = R.string.terms_section_scope_title),
                    body = stringResource(id = R.string.terms_section_scope_body)
                ),
                LegalSectionUiModel(
                    heading = stringResource(id = R.string.terms_section_responsibility_title),
                    body = stringResource(id = R.string.terms_section_responsibility_body)
                ),
                LegalSectionUiModel(
                    heading = stringResource(id = R.string.terms_section_backup_title),
                    body = stringResource(id = R.string.terms_section_backup_body)
                ),
                LegalSectionUiModel(
                    heading = stringResource(id = R.string.terms_section_availability_title),
                    body = stringResource(id = R.string.terms_section_availability_body)
                )
            ),
            onNavigateBack = onNavigateBack
        )
    }
}

private data class LegalSectionUiModel(
    val heading: String,
    val body: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LegalDocumentScreen(
    title: String,
    sections: List<LegalSectionUiModel>,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sections.size) { index ->
                val section = sections[index]
                Text(
                    text = section.heading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = section.body,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (index < sections.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}
