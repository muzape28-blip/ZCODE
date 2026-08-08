package com.zaba.zcode

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zaba.zcode.ui.theme.ZcodeTheme
import com.zaba.zcode.ui.workbench.WorkbenchScreen
import com.zaba.zcode.ui.terminal.TerminalScreen
import com.zaba.zcode.ui.settings.PipScreen
import com.zaba.zcode.ui.settings.AboutScreen
import com.zaba.zcode.core.files.Paths
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ZMUX lesson: debounce resize 100ms to avoid prompt jump 4-5 lines
    private val resizeHandler = Handler(Looper.getMainLooper())
    private val resizeRunnable = Runnable { /* terminalView.updateSize() will be called via Compose */ }
    private val resizeDebounceMs = 100L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm = WorkspaceViewModel(applicationContext)

        setContent {
            ZcodeTheme(themeType = vm.themeType) {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "editor") {
                    composable("editor") {
                        WorkbenchScreen(
                            vm = vm,
                            onRun = { filename ->
                                nav.navigate("output/$filename")
                            },
                            onNavigateToPip = { nav.navigate("pip") },
                            onNavigateToAbout = { nav.navigate("about") }
                        )
                    }
                    composable("output/{filename}") { backStackEntry ->
                        val filename = backStackEntry.arguments?.getString("filename") ?: "main.py"
                        val filesDir = Paths.filesDir(applicationContext)
                        TerminalScreen(
                            filename = filename,
                            filesDir = filesDir,
                            onBack = { nav.navigateUp() }
                        )
                    }
                    composable("pip") {
                        PipScreen(
                            onBack = { nav.navigateUp() }
                        )
                    }
                    composable("about") {
                        AboutScreen(
                            onBack = { nav.navigateUp() }
                        )
                    }
                }
            }
        }
    }

    // Called from Compose side when layout changes
    fun debouncedUpdateSize() {
        resizeHandler.removeCallbacks(resizeRunnable)
        resizeHandler.postDelayed(resizeRunnable, resizeDebounceMs)
    }
}
