package com.zaba.zcode.ui.editor

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

/**
 * EditorScreen — WebView file:// + Ace 1.44.0 bundled (offline-first, tanpa CDN).
 *
 * FIX blank & typing (deep crosscheck):
 * - allowFileAccessFromFileURLs + allowUniversalAccessFromFileURLs = true (Android 11+)
 * - isFocusable + isFocusableInTouchMode true + requestFocusFromTouch
 * - webViewReady state + pending code handling (file switch sebelum WebView ready)
 * - LaunchedEffect(fileName, webViewReady) setCode saat file ganti
 * - index.html safeSetCode + pendingSetCode + onWebViewReady()
 * - gutter 40px, debounce 100ms note (anti-regresi)
 * - Font 12px
 */

@Composable
fun EditorScreen(
    code: String,
    fileName: String?,
    onCodeChange: (String) -> Unit,
    webViewRef: MutableState<WebView?> = remember { mutableStateOf(null) }
) {
    var webViewReady by remember { mutableStateOf(false) }
    var pendingFileCode by remember { mutableStateOf<Pair<String?, String>?>(null) }

    // Ketika fileName berubah, simpan pending dan coba set jika ready
    LaunchedEffect(fileName, webViewReady) {
        if (fileName != null) {
            if (webViewReady) {
                webViewRef.value?.evaluateJavascript(
                    "setCode(${escapeJavaScriptString(code)});",
                    null
                )
            } else {
                pendingFileCode = fileName to code
            }
        }
    }

    // Juga kalau code berubah karena file switch tapi fileName sama? (refresh)
    LaunchedEffect(code, fileName) {
        // Hanya set jika fileName tidak berubah tapi code berubah dari luar (beautify, etc)
        // Kita deteksi via flag di WorkbenchScreen pushCode manual, jadi disini tidak auto set untuk typing
        // Namun untuk safety, jika pending ada, tetap set
    }

    LaunchedEffect(webViewReady) {
        if (webViewReady) {
            pendingFileCode?.let { (_, c) ->
                webViewRef.value?.evaluateJavascript(
                    "setCode(${escapeJavaScriptString(c)});",
                    null
                )
                pendingFileCode = null
            }
        }
    }

    Surface(color = Color(0xFF050806), modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        // penting untuk keyboard
                        javaScriptCanOpenWindowsAutomatically = false
                    }
                    isFocusable = true
                    isFocusableInTouchMode = true
                    // hardware layer biar Ace smooth
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.requestFocus()
                            view?.requestFocusFromTouch()
                            // Set initial code
                            view?.evaluateJavascript(
                                "setCode(${escapeJavaScriptString(code)});",
                                null
                            )
                            // Beri tahu JS bahwa WebView ready (agar pending flush)
                            view?.evaluateJavascript("onWebViewReady();", null)
                            // Post ready flag dengan delay kecil biar JS selesai init
                            postDelayed({
                                webViewReady = true
                                pendingFileCode?.let { (_, c) ->
                                    evaluateJavascript("setCode(${escapeJavaScriptString(c)});", null)
                                    pendingFileCode = null
                                }
                            }, 120)
                        }
                    }

                    setOnTouchListener { v, _ ->
                        if (!v.hasFocus()) {
                            v.requestFocus()
                            v.requestFocusFromTouch()
                        }
                        // jangan return true, biar WebView tetap terima touch untuk focus Ace
                        false
                    }

                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            evaluateJavascript("focusEditor();", null)
                        }
                    }

                    addJavascriptInterface(EditorBridge(onCodeChange), "ZCODE")
                    loadUrl("file:///android_asset/editor/index.html")
                    webViewRef.value = this
                }
            },
            update = { webView ->
                // Jangan setCode tiap recompose (bikin lag & cursor loncat)
                // setCode hanya via LaunchedEffect(fileName, webViewReady) + pushCode manual dari Workbench
            }
        )
    }
}

fun escapeJavaScriptString(value: String): String {
    val sb = StringBuilder()
    sb.append("\"")
    for (c in value) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> {
                if (c.code < 32 || c.code > 126) {
                    sb.append(String.format("\\u%04x", c.code))
                } else {
                    sb.append(c)
                }
            }
        }
    }
    sb.append("\"")
    return sb.toString()
}

class EditorBridge(private val onChange: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun onCodeChange(code: String) {
        onChange(code)
    }

    @android.webkit.JavascriptInterface
    fun getCode(): String = ""
}
