package com.ainest.aivideolibrary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ainest.aivideolibrary.util.AiAutoFillUtil
import com.ainest.aivideolibrary.util.PrefsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: com.ainest.aivideolibrary.viewmodel.VideoViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context.applicationContext) }

    var appLockEnabled by remember { mutableStateOf(prefs.appLockEnabled) }
    var useBiometric by remember { mutableStateOf(prefs.useBiometricForLock) }
    var pin by remember { mutableStateOf("") }
    var pinSetMessage by remember { mutableStateOf<String?>(null) }

    var backupEncryption by remember { mutableStateOf(prefs.backupEncryptionEnabled) }

    var aiProvider by remember { mutableStateOf(prefs.aiProvider) }
    var geminiKey by remember { mutableStateOf(prefs.geminiApiKey ?: "") }
    var claudeKey by remember { mutableStateOf(prefs.claudeApiKey ?: "") }
    var template by remember { mutableStateOf(prefs.aiPromptTemplate ?: AiAutoFillUtil.DEFAULT_TEMPLATE) }
    var instructionsMessage by remember { mutableStateOf<String?>(null) }

    var showAuthInline by remember { mutableStateOf(false) }
    var signedInEmail by remember { mutableStateOf(com.ainest.aivideolibrary.util.AuthUtil.currentUser()?.email) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    if (showAuthInline) {
        com.ainest.aivideolibrary.ui.AuthScreen(
            viewModel = viewModel,
            onSignedIn = {
                signedInEmail = com.ainest.aivideolibrary.util.AuthUtil.currentUser()?.email
                showAuthInline = false
            },
            onSkip = { showAuthInline = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Cloud Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (signedInEmail != null) {
                Text("Signed in as $signedInEmail", style = MaterialTheme.typography.bodyMedium)
                Text("Auto-sync every", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                var autoSyncHours by remember { mutableStateOf(prefs.autoSyncIntervalHours) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(12 to "12h", 24 to "24h", 72 to "3d", 168 to "7d")
                    options.forEach { (hours, label) ->
                        FilterChip(
                            selected = autoSyncHours == hours,
                            onClick = { autoSyncHours = hours; prefs.autoSyncIntervalHours = hours },
                            label = { Text(label) }
                        )
                    }
                }
                Button(onClick = {
                    viewModel.syncNow { ok -> syncMessage = if (ok) "Sync complete" else "Sync failed" }
                }) { Text("Sync Now") }
                TextButton(onClick = { showSignOutConfirm = true }) { Text("Sign Out") }
            } else {
                Text(
                    "Not signed in - your library stays fully offline until you sign in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { showAuthInline = true }) { Text("Sign In") }
            }
            syncMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Divider()

            Text("App Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Require unlock to open app")
                Switch(checked = appLockEnabled, onCheckedChange = {
                    appLockEnabled = it
                    prefs.appLockEnabled = it
                })
            }
            if (appLockEnabled) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Use fingerprint/face when available")
                    Switch(checked = useBiometric, onCheckedChange = {
                        useBiometric = it
                        prefs.useBiometricForLock = it
                    })
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } },
                    label = { Text("Set a backup PIN (4-8 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (pin.length in 4..8) {
                            prefs.pinHash = PrefsManager.hashPin(pin)
                            pinSetMessage = "PIN saved"
                            pin = ""
                        } else {
                            pinSetMessage = "PIN must be 4-8 digits"
                        }
                    }
                ) { Text("Save PIN") }
                pinSetMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }

            Divider()

            Text("Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Password-protect backups")
                Switch(checked = backupEncryption, onCheckedChange = {
                    backupEncryption = it
                    prefs.backupEncryptionEnabled = it
                })
            }
            Text(
                "When enabled, you'll set a password each time you export, and need it to restore.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            Text("AI Auto-fill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Generates Title, Hashtags, and Keywords from your Prompt. Gemini is free; Claude is pay-per-use.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = aiProvider == "GEMINI",
                    onClick = { aiProvider = "GEMINI"; prefs.aiProvider = "GEMINI" },
                    label = { Text("Gemini (default)") }
                )
                FilterChip(
                    selected = aiProvider == "CLAUDE",
                    onClick = { aiProvider = "CLAUDE"; prefs.aiProvider = "CLAUDE" },
                    label = { Text("Claude") }
                )
            }
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it; prefs.geminiApiKey = it },
                label = { Text("Gemini API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = claudeKey,
                onValueChange = { claudeKey = it; prefs.claudeApiKey = it },
                label = { Text("Claude API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Keys save automatically as you type.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                label = { Text("Auto-fill instructions (advanced)") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { prefs.aiPromptTemplate = template; instructionsMessage = "Instructions saved" }) { Text("Save Instructions") }
                TextButton(onClick = {
                    template = AiAutoFillUtil.DEFAULT_TEMPLATE
                    prefs.aiPromptTemplate = null
                    instructionsMessage = "Reset to default"
                }) { Text("Reset to Default") }
            }
            instructionsMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (signedInEmail != null) {
                Text(
                    "API keys sync automatically with your account via Sync Now / auto-sync.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Build marker: 2026-07-05-v3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }

    if (showSignOutConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign out?") },
            text = { Text("Your library stays on this device, but stops syncing until you sign in again.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signOut()
                    signedInEmail = null
                    showSignOutConfirm = false
                }) { Text("Sign Out") }
            },
            dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") } }
        )
    }
}
