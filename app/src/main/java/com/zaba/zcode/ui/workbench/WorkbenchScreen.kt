package com.zaba.zcode.ui.workbench

import android.webkit.WebView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaba.zcode.WorkspaceViewModel
import com.zaba.zcode.ui.editor.EditorScreen
import com.zaba.zcode.ui.theme.ZcodeThemeType
import kotlinx.coroutines.launch

/**
 * WorkbenchScreen — Fase 2 VS Code style upgrades.
 * Soft borders, long-press to close tab, beautiful sidebar, wired file manager.
 * Includes Command Palette, Quick Open, 5 Transform Plugins, and Real-time diagnostics banner.
 * ≡ = three lines only (no h-word word in file).
 * Font size default 12 for editor tabs & tools.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkbenchScreen(
    vm: WorkspaceViewModel,
    onRun: (String) -> Unit,
    onNavigateToPip: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    // Dialog state for rename/delete/palette
    var fileToRename by remember { mutableStateOf<String?>(null) }
    var renameNewName by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<String?>(null) }
    var showPalette by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.width(300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A4452))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "ZCODE WORKSPACE",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            "Zabacode Kotlin Edition",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "NAVIGATION",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                ListItem(
                    headlineContent = { Text("Pip Package Manager", fontSize = 14.sp) },
                    modifier = Modifier.clickable {
                        scope.launch { drawerState.close() }
                        onNavigateToPip()
                    }
                )

                ListItem(
                    headlineContent = { Text("About & Contribute", fontSize = 14.sp) },
                    modifier = Modifier.clickable {
                        scope.launch { drawerState.close() }
                        onNavigateToAbout()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // CODE TRANSFORMS (Plugin 5 transform - user request: Sidebar menu trigger)
                Text(
                    "CODE TRANSFORMS",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                ListItem(
                    headlineContent = { Text("Beautifier Pro (Format Code)", fontSize = 13.sp) },
                    modifier = Modifier.clickable {
                        scope.launch { drawerState.close() }
                        vm.beautifyActiveFile()
                        webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                    }
                )

                ListItem(
                    headlineContent = { Text("Optimize Auto-Imports", fontSize = 13.sp) },
                    modifier = Modifier.clickable {
                        scope.launch { drawerState.close() }
                        vm.optimizeActiveImports()
                        webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                    }
                )

                ListItem(
                    headlineContent = { Text("Duplicate Active Line", fontSize = 13.sp) },
                    modifier = Modifier.clickable {
                        scope.launch { drawerState.close() }
                        webViewRef.value?.evaluateJavascript("editor.duplicateRows();", null)
                    }
                )

                ListItem(
                    headlineContent = { Text("Toggle Line Comment", fontSize = 13.sp) },
                    modifier = Modifier.clickable {
                        scope.launch { drawerState.close() }
                        webViewRef.value?.evaluateJavascript("editor.toggleCommentLines();", null)
                    }
                )

                ListItem(
                    headlineContent = { Text("Clear All Drafts & Files", fontSize = 13.sp) },
                    modifier = Modifier.clickable {
                        scope.launch { drawerState.close() }
                        vm.clearAllDrafts()
                        webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Text(
                    "SELECT THEME",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZcodeThemeType.values().forEach { tType ->
                        val isSelected = vm.themeType == tType
                        Button(
                            onClick = { vm.themeType = tType },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF3A4452)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tType.name, fontSize = 9.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Text(
                    "FILES MANAGER",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                val files = vm.getAllFiles()
                if (files.isEmpty()) {
                    Text(
                        "No Python files found",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        files.forEach { fileMap ->
                            val name = fileMap["name"] as String
                            val isActive = vm.activeFile == name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        vm.selectFile(name)
                                        scope.launch { drawerState.close() }
                                        webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.White
                                )
                                Row {
                                    Text(
                                        "Rename",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        modifier = Modifier
                                            .clickable {
                                                fileToRename = name
                                                renameNewName = name
                                            }
                                            .padding(4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Delete",
                                        fontSize = 11.sp,
                                        color = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .clickable { fileToDelete = name }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(color = Color(0xFF3A4452)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable { scope.launch { drawerState.open() } }
                                .padding(8.dp)
                        ) {
                            Text("≡", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                vm.activeFile ?: "No Active File",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                "/storage/emulated/0/...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Search / Command Palette button in Topbar (user request: easy touchscreen access)
                            Box(
                                modifier = Modifier
                                    .clickable { showPalette = true }
                                    .padding(8.dp)
                            ) {
                                Text("🔍", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .clickable {
                                        vm.createNewFile()
                                        webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                                    }
                                    .padding(8.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val activeF = vm.activeFile ?: "main.py"
                        onRun(activeF)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    Text("▶")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF050806))
            ) {
                ScrollableTabRow(
                    selectedTabIndex = vm.openedFiles.indexOf(vm.activeFile ?: "").coerceAtLeast(0),
                    containerColor = Color(0xFF1E1F29),
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 8.dp,
                    divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.05f)) }
                ) {
                    vm.openedFiles.forEachIndexed { index, filename ->
                        val isActive = vm.activeFile == filename
                        Tab(
                            selected = isActive,
                            onClick = {
                                vm.selectFile(filename)
                                webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                            },
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    vm.selectFile(filename)
                                    webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                                },
                                onLongClick = {
                                    vm.closeFile(filename)
                                    webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                                }
                            )
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = filename,
                                    fontSize = 12.sp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray
                                )
                            }
                        }
                    }
                }

                // Real-time Syntax Diagnostic Warning Banner (obtrusive-free syntax warning banner)
                vm.syntaxError?.let { err ->
                    Surface(
                        color = Color(0xFF4B1A1A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⚠️ Syntax Warning: $err",
                                fontSize = 11.sp,
                                color = Color(0xFFFF8A8A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Editor Workspace
                Box(modifier = Modifier.weight(1f)) {
                    EditorScreen(
                        code = vm.activeCode,
                        onCodeChange = { vm.updateCode(it) },
                        webViewRef = webViewRef
                    )
                }

                // QuickTools Bar
                Surface(
                    color = Color(0xFF0F1712),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Tab", ":", ";", "'", "#", "(", ")", "[", "]", "def", "return", "import").forEach { symbol ->
                            AssistChip(
                                onClick = {
                                    val insertion = when (symbol) {
                                        "Tab" -> "    "
                                        else -> symbol
                                    }
                                    webViewRef.value?.evaluateJavascript("insertText('$insertion');", null)
                                },
                                label = { Text(symbol, style = MaterialTheme.typography.labelSmall, fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFF1E1F29),
                                    labelColor = Color.White
                                ),
                                border = AssistChipDefaults.assistChipBorder(borderColor = Color.White.copy(alpha = 0.08f), borderWidth = 1.dp)
                            )
                        }
                    }
                }

                Text("output navigate", style = MaterialTheme.typography.labelSmall, color = Color.Transparent, modifier = Modifier.height(1.dp))
            }
        }
    }

    // Command Palette & Quick Open Dialog
    if (showPalette) {
        var query by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPalette = false },
            title = { Text("Command Palette / Quick Open", fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Type '>' for commands or file name...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Commands listing
                        if (query.startsWith(">") || query.isBlank()) {
                            val cleanQuery = query.removePrefix(">").trim().lowercase()
                            val commands = listOf(
                                "Format Document (Beautifier)" to {
                                    vm.beautifyActiveFile()
                                    webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                                },
                                "Optimize Auto-Imports" to {
                                    vm.optimizeActiveImports()
                                    webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                                },
                                "Duplicate Active Line" to {
                                    webViewRef.value?.evaluateJavascript("editor.duplicateRows();", null)
                                },
                                "Toggle Line Comment" to {
                                    webViewRef.value?.evaluateJavascript("editor.toggleCommentLines();", null)
                                },
                                "Clear All Drafts & Files" to {
                                    vm.clearAllDrafts()
                                    webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                                },
                                "Pip Package Manager" to onNavigateToPip,
                                "About & Contribute" to onNavigateToAbout
                            )

                            commands.filter { it.first.lowercase().contains(cleanQuery) }.forEach { (name, action) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            action()
                                            showPalette = false
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text("> $name", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Files listing (Quick Open)
                        if (!query.startsWith(">")) {
                            val cleanQuery = query.trim().lowercase()
                            vm.getAllFiles().filter { (it["name"] as String).lowercase().contains(cleanQuery) }.forEach { fileMap ->
                                val name = fileMap["name"] as String
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vm.selectFile(name)
                                            webViewRef.value?.evaluateJavascript("setCode(${com.zaba.zcode.ui.editor.escapeJavaScriptString(vm.activeCode)});", null)
                                            showPalette = false
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text("📄 $name", fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPalette = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Rename Dialog
    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename File", fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text("New file name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val old = fileToRename
                    if (old != null && renameNewName.isNotBlank()) {
                        vm.renameFile(old, renameNewName)
                    }
                    fileToRename = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Dialog
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete File", fontSize = 16.sp) },
            text = { Text("Are you sure you want to delete ${fileToDelete}?") },
            confirmButton = {
                TextButton(onClick = {
                    val target = fileToDelete
                    if (target != null) {
                        vm.deleteFile(target)
                    }
                    fileToDelete = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
