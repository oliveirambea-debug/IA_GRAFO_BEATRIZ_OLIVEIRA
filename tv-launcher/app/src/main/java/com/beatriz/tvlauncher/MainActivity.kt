package com.beatriz.tvlauncher

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.beatriz.tvlauncher.ui.LauncherApp
import com.beatriz.tvlauncher.ui.LauncherTheme

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme { LauncherApp(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.resumed = true
        vm.onUserActivity()
        vm.refreshApps()
    }

    override fun onPause() {
        super.onPause()
        vm.resumed = false
    }

    /** Pressing Home while the launcher is already showing goes back to the main screen. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            vm.ambient = false
            vm.screen = Screen.Home
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Any key wakes the ambient screen; that key press is swallowed.
        if (event.action == KeyEvent.ACTION_DOWN && vm.onUserActivity()) return true
        return super.dispatchKeyEvent(event)
    }
}
