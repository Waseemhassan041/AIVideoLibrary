package com.ainest.aivideolibrary

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ainest.aivideolibrary.ui.AddEditVideoScreen
import com.ainest.aivideolibrary.ui.HomeScreen
import com.ainest.aivideolibrary.ui.LockScreen
import com.ainest.aivideolibrary.ui.RecycleBinScreen
import com.ainest.aivideolibrary.ui.SettingsScreen
import com.ainest.aivideolibrary.ui.theme.AIVideoLibraryTheme
import com.ainest.aivideolibrary.util.PrefsManager
import com.ainest.aivideolibrary.viewmodel.VideoViewModel

// FragmentActivity (not plain ComponentActivity) is required so BiometricPrompt
// (used for the app lock screen and fingerprint-confirmed bulk delete) can attach.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context.applicationContext) }
    var darkOverride by remember { mutableStateOf(prefs.darkModeOverride) }

    val isDarkTheme = darkOverride ?: true

    var isUnlocked by remember { mutableStateOf(!prefs.appLockEnabled) }
    var multiAddQueue by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var authResolved by remember { mutableStateOf(prefs.hasSkippedSignIn || com.ainest.aivideolibrary.util.AuthUtil.currentUser() != null) }

    AIVideoLibraryTheme(darkTheme = isDarkTheme) {
        if (!authResolved) {
            val viewModel: VideoViewModel = viewModel()
            com.ainest.aivideolibrary.ui.AuthScreen(
                viewModel = viewModel,
                onSignedIn = { authResolved = true },
                onSkip = {
                    prefs.hasSkippedSignIn = true
                    authResolved = true
                }
            )
            return@AIVideoLibraryTheme
        }

        if (!isUnlocked) {
            LockScreen(onUnlocked = { isUnlocked = true })
            return@AIVideoLibraryTheme
        }

        val navController = rememberNavController()
        val viewModel: VideoViewModel = viewModel()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = {
                        val newValue = !isDarkTheme
                        darkOverride = newValue
                        prefs.darkModeOverride = newValue
                    },
                    onOpenVideo = { id -> navController.navigate("editor/$id") },
                    onAddSingleVideo = { navController.navigate("editor/-1") },
                    onAddMultipleVideos = { uris ->
                        multiAddQueue = uris
                        navController.navigate("editor/-2")
                    },
                    onOpenRecycleBin = { navController.navigate("recycle_bin") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable(
                route = "editor/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: -1L
                AddEditVideoScreen(
                    viewModel = viewModel,
                    videoId = videoId,
                    multiAddQueue = if (videoId == -2L) multiAddQueue else emptyList(),
                    onDone = {
                        multiAddQueue = emptyList()
                        navController.popBackStack()
                    }
                )
            }
            composable("recycle_bin") {
                RecycleBinScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
