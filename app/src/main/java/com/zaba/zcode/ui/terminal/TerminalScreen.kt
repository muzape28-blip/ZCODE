package com.zaba.zcode.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaba.zcode.core.execution.ExecutionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Composable
fun TerminalScreen(
    filename: String,
    filesDir: File,
    onBack: () -> Unit
) {
    var terminalText by remember { mutableStateOf("ZCODE Terminal — Running $filename\n" + "-".repeat(40) + "\n") }
    var inputVal by remember { mutableStateOf(TextFieldValue("")) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Store reference to interactive session
    var session by remember { mutableStateOf<ExecutionEngine.InteractiveSession?>(null) }

    // Start execution on enter
    LaunchedEffect(filename) {
        val targetFile = File(filesDir, filename)
        if (!targetFile.exists()) {
            terminalText += "Error: File $filename not found!\n"
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val activeSession = ExecutionEngine.startInteractiveSession(targetFile)
                session = activeSession

                val reader = BufferedReader(InputStreamReader(activeSession.stdout, Charsets.UTF_8))
                var charCode: Int
                // Read character by character for real-time prompt streaming
                while (reader.read().also { charCode = it } != -1) {
                    val char = charCode.toChar()
                    withContext(Dispatchers.Main) {
                        terminalText += char
                        // Auto-scroll to bottom
                        scope.launch {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                    }
                }

                val exitCode = activeSession.process.waitFor()
                withContext(Dispatchers.Main) {
                    terminalText += "\n\nProcess finished with exit code $exitCode\n"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    terminalText += "\nExecution Error: ${e.message}\n"
                }
            }
        }
    }

    // Auto-focus on terminal open
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF3A4452)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button on top left (user request)
                    Text(
                        "◀ Back",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                session?.sendCtrlC()
                                onBack()
                            }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Terminal: $filename",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        },
        bottomBar = {
            // Action bar with Ctrl+C trigger (user request)
            Surface(color = Color(0xFF1E1F29)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            session?.sendCtrlC()
                            terminalText += "^C\nProcess Interrupted\n"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Ctrl+C", fontSize = 12.sp, color = Color.White)
                    }

                    Text(
                        "Tap below to type",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF050806)) // Always true-black
                .clickable { focusRequester.requestFocus() }
                .padding(12.dp)
        ) {
            // Main terminal view area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Column {
                    Text(
                        text = terminalText,
                        color = Color(0xFF39FF14), // Classic phosphor green
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp, // Font size 12
                        lineHeight = 16.sp
                    )

                    // Show current active prompt input line inline
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = inputVal.text,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        // Binking block cursor
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(14.dp)
                                .background(Color(0xFF39FF14))
                        )
                    }
                }
            }

            // Fully transparent input handler that binds standard soft keyboard
            TextField(
                value = inputVal,
                onValueChange = { inputVal = it },
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val line = inputVal.text
                        // Print locally
                        terminalText += line + "\n"
                        // Send to session stdin
                        session?.sendInput(line + "\n")
                        // Clear input buffer
                        inputVal = TextFieldValue("")
                        scope.launch {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                    }
                )
            )
        }
    }
}
