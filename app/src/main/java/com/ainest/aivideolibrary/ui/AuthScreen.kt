package com.ainest.aivideolibrary.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.ainest.aivideolibrary.util.AuthUtil
import com.ainest.aivideolibrary.util.CloudSyncUtil
import com.ainest.aivideolibrary.util.PrefsManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: com.ainest.aivideolibrary.viewmodel.VideoViewModel,
    onSignedIn: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleAvailable = remember { AuthUtil.isGoogleSignInAvailable(context) }

    fun syncThenContinue() {
        viewModel.syncNow { onSignedIn() }
    }

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val account = try { task.result } catch (e: Exception) { null }
        val idToken = account?.idToken
        if (idToken != null) {
            loading = true
            scope.launch {
                val res = AuthUtil.handleGoogleSignInResult(idToken)
                loading = false
                res.onSuccess { syncThenContinue() }.onFailure { error = it.message ?: "Google sign-in failed" }
            }
        } else {
            error = "Google sign-in was cancelled or failed"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 12.dp).size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("AI Video Library", style = MaterialTheme.typography.titleLarge)
        Text(
            "Sign in to sync your library metadata and thumbnails across devices - or skip and use it fully offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (googleAvailable) {
            Button(
                onClick = { googleLauncher.launch(AuthUtil.googleSignInIntent(context)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                enabled = !loading
            ) { Text("Continue with Google") }
        }

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
        } else {
            OutlinedButton(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        error = "Enter email and password"
                        return@OutlinedButton
                    }
                    loading = true
                    error = null
                    scope.launch {
                        val res = if (isSignUp) AuthUtil.signUpWithEmail(email, password) else AuthUtil.signInWithEmail(email, password)
                        loading = false
                        res.onSuccess { syncThenContinue() }.onFailure { error = it.message ?: "Failed" }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isSignUp) "Sign Up" else "Sign In") }
        }

        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(if (isSignUp) "Already have an account? Sign In" else "New here? Create an account")
        }

        TextButton(onClick = onSkip, modifier = Modifier.padding(top = 12.dp)) {
            Text("Continue without signing in")
        }
    }
}
