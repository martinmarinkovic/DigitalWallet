package com.threemdroid.digitalwallet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.threemdroid.digitalwallet.app.DigitalWalletApp
import com.threemdroid.digitalwallet.app.AppThemeViewModel
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.data.reminder.ExpirationReminderIntents
import com.threemdroid.digitalwallet.ui.theme.DigitalWalletTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val pendingReminderCardId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingReminderCardId.value = ExpirationReminderIntents.consumeReminderCardId(intent)
        enableEdgeToEdge()
        setContent {
            val appThemeViewModel: AppThemeViewModel = hiltViewModel()
            val appThemeState by appThemeViewModel.uiState.collectAsStateWithLifecycle()
            val reminderCardId by pendingReminderCardId.collectAsStateWithLifecycle()
            val systemDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (appThemeState.themeMode) {
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DigitalWalletTheme(darkTheme = useDarkTheme) {
                DigitalWalletApp(
                    reminderCardId = reminderCardId,
                    onReminderCardIdHandled = {
                        pendingReminderCardId.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingReminderCardId.value = ExpirationReminderIntents.consumeReminderCardId(intent)
    }
}
