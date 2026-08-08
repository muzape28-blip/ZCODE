package com.zaba.zcode

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaba.zcode.core.editor.Checker
import com.zaba.zcode.core.execution.ExecutionEngine
import com.zaba.zcode.core.files.FileManager
import com.zaba.zcode.core.files.Paths
import com.zaba.zcode.core.plugins.PluginHost
import com.zaba.zcode.ui.theme.ZcodeThemeType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * WorkspaceViewModel — pusat state workspace ZCODE (satu sumber kebenaran, DRY).
 * - CRUD file via FileManager (filesDir internal, anti traversal, 512KB guard)
 * - Persistensi: isi file tersimpan async di IO (FIX lag global: jangan save sync di Main)
 * - Daftar file di-cache (FIX lag drawer: jangan listFiles() di composition tiap frame)
 * - Diagnostik sintaksis real-time (debounce 800ms, cancel job lama → tanpa race)
 * - Guard anti double-trigger long-press close → re-open (400ms)
 */
@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val filesDir: File = Paths.filesDir(appContext)
    private val prefs = appContext.getSharedPreferences("zcode_workspace", Context.MODE_PRIVATE)

    private var validationJob: Job? = null
    private var saveJob: Job? = null

    var themeType by mutableStateOf(ZcodeThemeType.RETRO)

    val openedFiles = mutableStateListOf<String>()
    var activeFile by mutableStateOf<String?>(null)
    var activeCode by mutableStateOf("")
    var syntaxError by mutableStateOf<String?>(null)

    // FIX lag drawer: cache list file, refresh hanya saat ada perubahan file
    var filesInfoCache by mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set

    private val fileDrafts = mutableMapOf<String, String>()
    private var lastClosed: Pair<String, Long>? = null

    init {
        if (!filesDir.exists()) filesDir.mkdirs()
        ExecutionEngine.workspaceDirPath = filesDir.absolutePath
        loadSavedWorkspace()
        // sync pertama biar drawer tidak kosong flicker
        filesInfoCache = FileManager.listFiles(filesDir)
        refreshFiles()
    }

    // ------------------------------------------------------------------
    // File list cache (anti disk I/O di composition)
    // ------------------------------------------------------------------

    fun refreshFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = FileManager.listFiles(filesDir)
            withContext(Dispatchers.Main) { filesInfoCache = list }
        }
    }

    // Sync version untuk situasi yang butuh langsung (createNewFile butuh set existing)
    private fun refreshFilesSync() {
        filesInfoCache = FileManager.listFiles(filesDir)
    }

    // ------------------------------------------------------------------
    // Persistensi workspace (tab terbuka + file aktif)
    // ------------------------------------------------------------------

    private fun persistWorkspaceState() {
        try {
            val json = JSONObject()
            json.put("opened", JSONArray(openedFiles))
            json.put("active", activeFile ?: "")
            prefs.edit().putString("workspace", json.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun loadSavedWorkspace() {
        val available = FileManager.listFiles(filesDir)
        if (available.isEmpty()) {
            // save sync untuk bootstrap pertama (file kecil)
            FileManager.saveFile(filesDir, "main.py", "# Welcome to ZCODE\nprint(\"Hello, ZCODE!\")\n")
        }

        val saved = try {
            prefs.getString("workspace", null)?.let { JSONObject(it) }
        } catch (_: Exception) {
            null
        }

        val savedTabs = saved?.optJSONArray("opened")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } ?: emptyList()

        if (savedTabs.isNotEmpty()) {
            val validTabs = savedTabs.filter { File(filesDir, it).exists() }
            openedFiles.addAll(validTabs)
            activeFile = saved?.optString("active").takeIf { it in validTabs } ?: validTabs.firstOrNull()
        }

        if (openedFiles.isEmpty()) {
            openedFiles.add("main.py")
            activeFile = "main.py"
        }

        activeCode = activeFile?.let { FileManager.readFile(filesDir, it).getOrDefault("") } ?: ""
        activeFile?.let { fileDrafts[it] = activeCode }
        validateSyntaxDebounced(activeCode)
        persistWorkspaceState()
    }

    // ------------------------------------------------------------------
    // Navigasi file
    // ------------------------------------------------------------------

    fun selectFile(filename: String) {
        val now = System.currentTimeMillis()
        val lc = lastClosed
        if (lc != null && lc.first == filename && now - lc.second < 400) return

        if (filename !in openedFiles) openedFiles.add(filename)
        activeFile = filename
        // read di IO? Untuk latency kecil, masih Main tapi file kecil 512KB. Jika mau super smooth, bisa IO.
        // Kita tetap Main untuk select cepat, tapi file list sudah cache.
        val code = fileDrafts[filename] ?: FileManager.readFile(filesDir, filename).getOrDefault("")
        activeCode = code
        fileDrafts[filename] = code
        validateSyntaxDebounced(code)
        persistWorkspaceState()
    }

    fun updateCode(newCode: String) {
        if (newCode == activeCode) return
        activeCode = newCode
        val current = activeFile ?: return
        fileDrafts[current] = newCode

        // FIX lag global: save async di IO + debounce 350ms (mirip VS Code autoSave)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            withContext(Dispatchers.IO) {
                FileManager.saveFile(filesDir, current, newCode)
            }
            // setelah save, refresh cache size (opsional, jarang)
            refreshFiles()
        }

        validateSyntaxDebounced(newCode)
    }

    fun createNewFile() {
        // butuh list existing sync agar tidak duplicate name
        val existing = FileManager.listFiles(filesDir).map { it["name"] as String }.toSet()
        var index = 1
        var newName = "untitled_$index.py"
        while (existing.contains(newName)) {
            index++
            newName = "untitled_$index.py"
        }
        // save sync kecil untuk new file agar langsung ada
        FileManager.saveFile(filesDir, newName, "# New python script\n")
        refreshFilesSync()
        selectFile(newName)
    }

    fun closeFile(filename: String) {
        val idx = openedFiles.indexOf(filename)
        if (idx == -1) return
        openedFiles.removeAt(idx)
        fileDrafts.remove(filename)
        lastClosed = filename to System.currentTimeMillis()
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
        persistWorkspaceState()
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
                if (idx != -1) openedFiles[idx] = securedNew
                if (activeFile == securedOld) activeFile = securedNew
                val draft = fileDrafts.remove(securedOld)
                if (draft != null) fileDrafts[securedNew] = draft
                refreshFilesSync()
                persistWorkspaceState()
                return true
            }
        }
        return false
    }

    fun deleteFile(filename: String): Boolean {
        val secured = FileManager.secureFilename(filename) ?: return false
        val file = File(filesDir, secured)
        return if (file.exists() && file.delete()) {
            // closeFile juga persist
            closeFile(secured)
            refreshFilesSync()
            true
        } else false
    }

    // Dipakai UI: cache, bukan scan disk tiap recompose
    fun getAllFiles(): List<Map<String, Any>> = filesInfoCache

    // ------------------------------------------------------------------
    // Diagnostik sintaksis real-time (Fase 2)
    // ------------------------------------------------------------------

    private fun validateSyntaxDebounced(code: String) {
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            delay(800)
            withContext(Dispatchers.Default) {
                val err = Checker.checkSyntax(code)
                withContext(Dispatchers.Main) { syntaxError = err }
            }
        }
    }

    // ------------------------------------------------------------------
    // Plugin transform (Fase 2)
    // ------------------------------------------------------------------

    fun beautifyActiveFile() {
        val beautified = PluginHost.beautify(activeCode)
        // beautify langsung tanpa debounce save agar terasa instan, tapi save async
        activeCode = beautified
        activeFile?.let { fileDrafts[it] = beautified }
        viewModelScope.launch(Dispatchers.IO) {
            activeFile?.let { FileManager.saveFile(filesDir, it, beautified) }
        }
        validateSyntaxDebounced(beautified)
        refreshFiles()
    }

    fun optimizeActiveImports() {
        val optimized = PluginHost.optimizeImports(activeCode)
        activeCode = optimized
        activeFile?.let { fileDrafts[it] = optimized }
        viewModelScope.launch(Dispatchers.IO) {
            activeFile?.let { FileManager.saveFile(filesDir, it, optimized) }
        }
        validateSyntaxDebounced(optimized)
    }

    fun clearAllDrafts() {
        viewModelScope.launch(Dispatchers.IO) {
            openedFiles.toList().forEach { FileManager.deleteFileIfExists(filesDir, it) }
            withContext(Dispatchers.Main) {
                openedFiles.clear()
                fileDrafts.clear()
                activeFile = null
                activeCode = ""
                syntaxError = null
                prefs.edit().clear().apply()
                // recreate main.py sync
                FileManager.saveFile(filesDir, "main.py", "# Welcome to ZCODE\nprint(\"Hello, ZCODE!\")\n")
                loadSavedWorkspace()
                refreshFilesSync()
            }
        }
    }
}
