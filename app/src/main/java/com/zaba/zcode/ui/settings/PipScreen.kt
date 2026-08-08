package com.zaba.zcode.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaba.zcode.core.execution.ExecutionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PipScreen — Settings → Pip: instal package Python via pip.
 * FIX:
 * - pip ModuleNotFoundError di Chaquopy: sekarang pip dibundle via build.gradle pip { install "pip" }
 * - log buffering 50ms (jangan recompose per line)
 * - cap log MAX_OUTPUT_CHARS
 */

@Composable
fun PipScreen(
    context: android.content.Context,
    onBack: () -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf(initialLog()) }
    var isInstalling by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // buffer untuk batch log (FIX lag per line)
    val logBuffer = remember { StringBuilder() }
    var flushJob by remember { mutableStateOf<Job?>(null) }

    fun flushLog() {
        if (logBuffer.isEmpty()) return
        val chunk = logBuffer.toString()
        logBuffer.clear()
        val combined = logText + chunk
        logText = if (combined.length > ExecutionEngine.MAX_OUTPUT_CHARS)
            combined.takeLast(ExecutionEngine.MAX_OUTPUT_CHARS) else combined
        scope.launch {
            delay(30)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun appendLogBuffered(text: String) {
        logBuffer.append(text)
        if (flushJob?.isActive != true) {
            flushJob = scope.launch {
                delay(60)
                flushLog()
            }
        }
    }

    fun appendLogImmediate(text: String) {
        logBuffer.append(text)
        flushLog()
    }

    fun install(pkg: String) {
        val trimmed = pkg.trim()
        if (trimmed.isBlank() || isInstalling) return
        if (!ExecutionEngine.isSafePackageName(trimmed)) {
            appendLogImmediate("\n❌ Package name tidak valid: '$trimmed'\n")
            return
        }
        isInstalling = true
        appendLogImmediate("\n> pip install $trimmed\n")

        val ok = ExecutionEngine.startPipStream(
            context = context,
            packageName = trimmed,
            onLog = { line -> appendLogBuffered(line) },
            onDone = { success, exitCode ->
                scope.launch {
                    // flush sisa
                    delay(80)
                    flushLog()
                    if (success) {
                        appendLogImmediate("\n✅ Package '$trimmed' installed successfully!\n")
                    } else {
                        appendLogImmediate("\n❌ Installation failed (exit code $exitCode).\n")
                        if (exitCode != 0) {
                            appendLogImmediate("Tip: Coba nama lain atau cek koneksi.\n")
                        }
                    }
                    isInstalling = false
                }
            }
        )
        if (!ok) {
            appendLogImmediate("\n❌ Gagal memulai pip. Cek nama package & coba lagi.\n")
            isInstalling = false
        }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "◀ Back",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Pip Package Manager",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name", fontSize = 12.sp) },
                    placeholder = { Text("e.g. requests", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { install(packageName) }),
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                Button(
                    onClick = { install(packageName) },
                    enabled = packageName.isNotBlank() && !isInstalling,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isInstalling) {
                        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        Text("Install", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "INSTALLATION LOG:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF050806), shape = RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = logText,
                    color = Color(0xFF39FF14),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun initialLog(): String =
    "ZCODE Pip Installer Layer — Chaquopy 3.11\n" +
        "-".repeat(45) + "\n" +
        "Ketik nama package (requests, numpy, dll) lalu tap Install.\n" +
        "Log streaming realtime, max ${com.zaba.zcode.core.execution.ExecutionEngine.MAX_OUTPUT_CHARS / 1024}KB.\n\n"
