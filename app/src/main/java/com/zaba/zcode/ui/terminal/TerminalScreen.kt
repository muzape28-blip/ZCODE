package com.zaba.zcode.ui.terminal

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaba.zcode.core.execution.ExecutionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * TerminalScreen — layer Terminal full-screen (pindah layer).
 * FIX V3 - keyboard tidak muncul (referensi StackOverflow + Compose docs):
 * - Pakai BasicTextField (lebih ringan dari TextField Material) + focusRequester + LocalSoftwareKeyboardController
 * - LaunchedEffect + delay(100) + awaitFrame pattern + keyboardController.show()
 * - TextField tidak boleh alpha 0f + 0.sp (IME anggap gone) → pakai alpha 0.01f + textStyle 12.sp transparent
 * - Parent Column clickable requestFocus + show keyboard, bukan cuma requestFocus
 * - Buffer 50ms debounce untuk lag per-char
 */

@Composable
fun TerminalScreen(
    filename: String,
    filesDir: File,
    context: Context,
    onBack: () -> Unit
) {
    var terminalText by remember { mutableStateOf("ZCODE Terminal — Running $filename\n" + "-".repeat(40) + "\n") }
    var inputVal by remember { mutableStateOf(TextFieldValue("")) }
    var session by remember { mutableStateOf<ExecutionEngine.InteractiveSession?>(null) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val outputBuffer = remember { StringBuilder() }
    var flushJob by remember { mutableStateOf<Job?>(null) }

    fun flushBuffer() {
        if (outputBuffer.isEmpty()) return
        val chunk = outputBuffer.toString()
        outputBuffer.clear()
        val combined = terminalText + chunk
        terminalText = if (combined.length > ExecutionEngine.MAX_OUTPUT_CHARS) {
            "\n…[output truncated: >${ExecutionEngine.MAX_OUTPUT_CHARS} chars]…\n" +
                combined.takeLast(ExecutionEngine.MAX_OUTPUT_CHARS)
        } else combined
        scope.launch {
            delay(20)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun appendBuffered(text: String) {
        outputBuffer.append(text)
        if (flushJob?.isActive != true) {
            flushJob = scope.launch {
                delay(50)
                flushBuffer()
            }
        }
    }

    fun appendImmediate(text: String) {
        outputBuffer.append(text)
        flushBuffer()
    }

    LaunchedEffect(filename) {
        val targetFile = File(filesDir, filename)
        if (!targetFile.exists()) {
            appendImmediate("\nError: File $filename not found!\n")
            return@LaunchedEffect
        }
        appendImmediate("\n[backend: ${ExecutionEngine.describeBackend()}]\n")
        val activeSession = ExecutionEngine.startInteractiveSession(
            context = context,
            file = targetFile,
            onOutput = { chunk -> appendBuffered(chunk) },
            onExit = { code ->
                scope.launch {
                    delay(60)
                    flushBuffer()
                    withContext(Dispatchers.Main) {
                        val msg = "\n\nProcess finished with exit code $code\n"
                        val combined = terminalText + msg
                        terminalText = if (combined.length > ExecutionEngine.MAX_OUTPUT_CHARS)
                            combined.takeLast(ExecutionEngine.MAX_OUTPUT_CHARS) else combined
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            }
        )
        session = activeSession
        withContext(Dispatchers.IO) { activeSession.waitForExit() }
    }

    // FIX keyboard: request focus + show dengan delay + awaitFrame pattern (Compose docs)
    LaunchedEffect(Unit) {
        delay(300)
        try {
            focusRequester.requestFocus()
            delay(100)
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    DisposableEffect(Unit) {
        onDispose { session?.sendKill() }
    }

    val blinkTransition = rememberInfiniteTransition(label = "cursor")
    val blinkAlpha by blinkTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "cursorAlpha"
    )

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
                            .clickable {
                                session?.sendCtrlC()
                                onBack()
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Terminal: $filename",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF1E1F29)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            session?.sendCtrlC()
                            appendImmediate("^C\nProcess Interrupted\n")
                            // setelah Ctrl+C, fokus balik
                            scope.launch {
                                delay(100)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Ctrl+C", fontSize = 12.sp, color = Color.White)
                    }
                    Text(
                        "Tap terminal untuk ketik • Enter kirim",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            scope.launch {
                                focusRequester.requestFocus()
                                delay(50)
                                keyboardController?.show()
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF050806))
                // Klik di area kosong = fokus + show keyboard
                .clickable {
                    scope.launch {
                        try {
                            focusRequester.requestFocus()
                            delay(80)
                            keyboardController?.show()
                        } catch (_: Exception) {}
                    }
                }
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    // Klik di output juga fokus
                    .clickable {
                        scope.launch {
                            focusRequester.requestFocus()
                            delay(50)
                            keyboardController?.show()
                        }
                    }
            ) {
                Column {
                    Text(
                        text = terminalText,
                        color = Color(0xFF39FF14),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = inputVal.text,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(14.dp)
                                .alpha(blinkAlpha)
                                .background(Color(0xFF39FF14))
                        )
                    }
                }
            }

            // FIX: BasicTextField, bukan TextField Material, alpha 0.01f (jangan 0f) + height 24dp biar IME anggap visible
            // Ini adalah pattern yang work di StackOverflow untuk Compose + WebView fokus issue
            BasicTextField(
                value = inputVal,
                onValueChange = { inputVal = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .alpha(0.01f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = Color.Transparent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val line = inputVal.text
                        appendImmediate(line + "\n")
                        session?.sendInput(line + "\n")
                        inputVal = TextFieldValue("")
                        scope.launch {
                            delay(50)
                            focusRequester.requestFocus()
                            delay(50)
                            keyboardController?.show()
                        }
                    }
                )
            )
        }
    }
}
