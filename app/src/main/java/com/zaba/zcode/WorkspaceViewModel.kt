package com.zaba.zcode

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.zaba.zcode.core.editor.Checker
import com.zaba.zcode.core.files.FileManager
import com.zaba.zcode.core.files.Paths
import com.zaba.zcode.core.plugins.PluginHost
import com.zaba.zcode.ui.theme.ZcodeThemeType
import kotlinx.coroutines.*
import java.io.File

class WorkspaceViewModel(private val context: Context) : ViewModel() {
    private val filesDir = Paths.filesDir(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var validationJob: Job? = null

    var themeType by mutableStateOf(ZcodeThemeType.RETRO)
    var openedFiles = mutableStateListOf<String>()
    var activeFile by mutableStateOf<String?>(null)
    var activeCode by mutableStateOf("")
    var syntaxError by mutableStateOf<String?>(null)

    private val fileDrafts = mutableMapOf<String, String>()

    init {
        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }
        loadSavedWorkspace()
    }

    private fun loadSavedWorkspace() {
        val available = FileManager.listFiles(filesDir)
        if (available.isEmpty()) {
            FileManager.saveFile(filesDir, "main.py", "# Welcome to ZCODE\nprint(\"Hello, ZCODE!\")\n")
        }
        val defaultFile = "main.py"
        openedFiles.add(defaultFile)
        activeFile = defaultFile
        val code = FileManager.readFile(filesDir, defaultFile).getOrDefault("")
        activeCode = code
        validateSyntaxDebounced(code)
    }

    fun selectFile(filename: String) {
        activeFile?.let { current ->
            fileDrafts[current] = activeCode
        }

        if (filename !in openedFiles) {
            openedFiles.add(filename)
        }
        activeFile = filename
        val code = fileDrafts[filename] ?: FileManager.readFile(filesDir, filename).getOrDefault("")
        activeCode = code
        validateSyntaxDebounced(code)
    }

    fun updateCode(newCode: String) {
        activeCode = newCode
        activeFile?.let { current ->
            fileDrafts[current] = newCode
            FileManager.saveFile(filesDir, current, newCode)
        }
        validateSyntaxDebounced(newCode)
    }

    fun createNewFile() {
        val existing = FileManager.listFiles(filesDir).map { it["name"] as String }.toSet()
        var index = 1
        var newName = "untitled_$index.py"
        while (existing.contains(newName)) {
            index++
            newName = "untitled_$index.py"
        }
        FileManager.saveFile(filesDir, newName, "# New python script\n")
        selectFile(newName)
    }

    fun closeFile(filename: String) {
        val idx = openedFiles.indexOf(filename)
        if (idx != -1) {
            openedFiles.removeAt(idx)
            fileDrafts.remove(filename)
            if (activeFile == filename) {
                if (openedFiles.isNotEmpty()) {
                    val nextIdx = if (idx < openedFiles.size) idx else openedFiles.size - 1
                    selectFile(openedFiles[nextIdx])
                } else {
                    activeFile = null
                    activeCode = ""
                    syntaxError = null
                }
            }
        }
    }

    fun renameFile(oldName: String, newName: String): Boolean {
        val securedOld = FileManager.secureFilename(oldName) ?: return false
        val securedNew = FileManager.secureFilename(newName) ?: return false
        val oldFile = File(filesDir, securedOld)
        val newFile = File(filesDir, securedNew)
        if (oldFile.exists() && !newFile.exists()) {
            val ok = oldFile.renameTo(newFile)
            if (ok) {
                val idx = openedFiles.indexOf(securedOld)
                if (idx != -1) {
                    openedFiles[idx] = securedNew
                }
                if (activeFile == securedOld) {
                    activeFile = securedNew
                }
                val draft = fileDrafts.remove(securedOld)
                if (draft != null) {
                    fileDrafts[securedNew] = draft
                }
                return true
            }
        }
        return false
    }

    fun deleteFile(filename: String): Boolean {
        val secured = FileManager.secureFilename(filename) ?: return false
        val file = File(filesDir, secured)
        if (file.exists()) {
            val ok = file.delete()
            if (ok) {
                closeFile(secured)
                return true
            }
        }
        return false
    }

    fun getAllFiles(): List<Map<String, Any>> {
        return FileManager.listFiles(filesDir)
    }

    private fun validateSyntaxDebounced(code: String) {
        validationJob?.cancel()
        validationJob = scope.launch {
            delay(800)
            withContext(Dispatchers.Default) {
                val err = Checker.checkSyntax(code)
                withContext(Dispatchers.Main) {
                    syntaxError = err
                }
            }
        }
    }

    fun beautifyActiveFile() {
        val beautified = PluginHost.beautify(activeCode)
        updateCode(beautified)
    }

    fun optimizeActiveImports() {
        val optimized = PluginHost.optimizeImports(activeCode)
        updateCode(optimized)
    }

    fun clearAllDrafts() {
        openedFiles.clear()
        fileDrafts.clear()
        activeFile = null
        activeCode = ""
        syntaxError = null
        FileManager.listFiles(filesDir).forEach { fileMap ->
            val name = fileMap["name"] as String
            File(filesDir, name).delete()
        }
        loadSavedWorkspace()
    }

    fun stop() {
        scope.cancel()
    }
}
