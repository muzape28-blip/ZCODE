package com.zaba.zcode.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipScreen(
    onBack: () -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf("ZCODE Pip Installer Layer — Chaquopy 3.11\n" + "-".repeat(45) + "\nType package name above and tap Install.\n") }
    var isInstalling by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

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
                    Text(
                        "◀ Back",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Pip Package Manager",
                        color = Color.White,
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
            // Package name input
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
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (packageName.isNotBlank() && !isInstalling) {
                                isInstalling = true
                                logText += "\n> pip install $packageName\n"
                                // Trigger installation
                            }
                        }
                    ),
                    textStyle = TextStyle(fontSize = 14.sp)
                )

                Button(
                    onClick = {
                        if (packageName.isNotBlank() && !isInstalling) {
                            isInstalling = true
                            logText += "\n> pip install $packageName\n"

                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    try {
                                        // Run real pip command or simulated secure offline pip with output streaming
                                        // On real device, Chaquopy runs pip. Here we can invoke python3 -m pip install
                                        // to prove execution and fetch traceback in real-time.
                                        val pb = ProcessBuilder("python3", "-m", "pip", "install", packageName)
                                        pb.redirectErrorStream(true)
                                        val process = pb.start()

                                        val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
                                        var line: String?
                                        while (reader.readLine().also { line = it } != null) {
                                            val l = line
                                            withContext(Dispatchers.Main) {
                                                logText += l + "\n"
                                                scope.launch {
                                                    scrollState.scrollTo(scrollState.maxValue)
                                                }
                                            }
                                        }

                                        val exitCode = process.waitFor()
                                        withContext(Dispatchers.Main) {
                                            if (exitCode == 0) {
                                                logText += "\n✅ Package '$packageName' installed successfully!\n"
                                            } else {
                                                logText += "\n❌ Installation failed (exit code $exitCode).\n"
                                            }
                                            isInstalling = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            logText += "\n❌ Error: ${e.message}\n"
                                            isInstalling = false
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = packageName.isNotBlank() && !isInstalling,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(size = 18.dp, color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Install", fontSize = 12.sp, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Output Log Terminal
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
                    .background(Color(0xFF050806), shape = RoundedCornerShape(8.dp)) // Always True-Black
                    .padding(12.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = logText,
                    color = Color(0xFF39FF14), // Phosphor green
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, // Font size 12
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// Dummy text style wrapper to guarantee import resolution
private val TextStyle = androidx.compose.ui.text.TextStyle
