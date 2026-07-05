package com.ainest.aivideolibrary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.ainest.aivideolibrary.util.BiometricUtil
import com.ainest.aivideolibrary.util.PrefsManager

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context.applicationContext) }
    val activity = context as? FragmentActivity

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun tryBiometric() {
        if (activity != null && prefs.useBiometricForLock) {
            BiometricUtil.confirm(
                activity = activity,
                title = "Unlock AI Video Library",
                subtitle = "Use your fingerprint or face to continue",
                onSuccess = onUnlocked,
                onUnavailable = { /* fall back to PIN field below */ },
                onCancelled = { /* stay on lock screen, user can use PIN */ }
            )
        }
    }

    LaunchedEffect(Unit) { tryBiometric() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
        Text("Locked", style = MaterialTheme.typography.titleLarge)
        Text(
            "Enter your PIN to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            isError = error != null,
            supportingText = { error?.let { Text(it) } }
        )
        Button(
            onClick = {
                val hash = PrefsManager.hashPin(pin)
                if (hash == prefs.pinHash) {
                    onUnlocked()
                } else {
                    error = "Incorrect PIN"
                }
            },
            modifier = Modifier.padding(top = 12.dp)
        ) { Text("Unlock") }

        if (activity != null && prefs.useBiometricForLock) {
            TextButton(onClick = { tryBiometric() }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Try fingerprint again")
            }
        }
    }
}
