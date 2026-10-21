package com.shohankhan.hisaabos.ui.app

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohankhan.hisaabos.di.AppContainer
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.notifications.Notifications
import com.shohankhan.hisaabos.ui.components.LoadingScreen
import com.shohankhan.hisaabos.ui.lock.LockScreen
import com.shohankhan.hisaabos.ui.lock.RecoveryScreen
import com.shohankhan.hisaabos.ui.onboarding.OnboardingScreen
import com.shohankhan.hisaabos.ui.onboarding.OnboardingViewModel
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.work.ReminderWorker

@Composable
fun HisaabRoot(container: AppContainer) {
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AppViewModel(container.preferences, container.sessionManager) }
        },
    )
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val locked by appViewModel.locked.collectAsStateWithLifecycle()
    val onboardingViewModel: OnboardingViewModel = appViewModel { OnboardingViewModel(it.repository) }
    val context = LocalContext.current
    var showRecovery by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    DisposableEffect(Unit) {
        Notifications.ensureChannels(context)
        ReminderWorker.schedule(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        onDispose { }
    }

    LaunchedEffect(settings.onboardingCompleted) {
        ready = true
    }

    // Screen capture protection is a user setting (default on).
    DisposableEffect(settings.screenProtection) {
        val activity = context as? Activity
        if (activity != null) {
            if (settings.screenProtection) {
                activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose { }
    }

    // Auto-lock after the configured time in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, settings.autoLockSeconds, settings.onboardingCompleted) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (settings.onboardingCompleted) appViewModel.onBackground()
                Lifecycle.Event.ON_START -> if (settings.onboardingCompleted) appViewModel.onForeground(settings.autoLockSeconds)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        when {
            !ready -> LoadingScreen()
            !settings.onboardingCompleted -> {
                OnboardingScreen(
                    onboardingViewModel = onboardingViewModel,
                    appViewModel = appViewModel,
                    onFinished = { },
                )
            }
            locked -> {
                if (showRecovery) {
                    RecoveryScreen(
                        onCancel = { showRecovery = false },
                        onWipeAndRestart = {
                            appViewModel.resetSecurityAfterWipe { showRecovery = false }
                        },
                        onWipe = { container.repository.wipeEverything() },
                    )
                } else {
                    LockScreen(
                        viewModel = appViewModel,
                        settings = settings,
                        onForgotPin = { showRecovery = true },
                    )
                }
            }
            else -> MainScaffold(
                appViewModel = appViewModel,
                settings = settings,
            )
        }
    }
}
