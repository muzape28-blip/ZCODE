package com.zaba.zcode.ui.editor

import android.view.View
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
 * EditorScreen — WebView file:// + Ace 1.44.0 bundled (offline-first)
 *
 * FIX SERIUS & HATI-HATI (pelajaran ZABACODE Monaco→Ace):
 * - ZABACODE pernah ada 2 engine native+Monaco, problem keyboard sama, puluhan fix, akhirnya migrasi Ace
 * - ZCODE jangan ulangi: keep hack minimal, jangan over-engineering
 *
 * Yang terbukti work di StackOverflow Android WebView keyboard issue:
 * - ZcodeWebView.onCheckIsTextEditor = true (tanpa BaseInputConnection dummy)
 * - isFocusable true, isFocusableInTouchMode true
 * - requestFocus(View.FOCUS_DOWN) di onPageFinished
 * - setOnTouchListener ACTION_DOWN/UP requestFocus (jangan return true, biarkan WebView terima touch)
 * - allowFileAccessFromFileURLs + allowUniversalAccessFromFileURLs (Android 11+ file://)
 * - Ace text-input width 100% (full buffer) di index.html
 * - Jangan pakai Box clickable wrapper di WorkbenchScreen (intercept touch)
 * - setLayerType SOFTWARE (bukan HARDWARE) agar IME attach stabil di Samsung/Xiaomi
 *
 * Blank fix:
 * - pendingFileCode handling untuk file switch sebelum WebView ready
 * - safeSetCode di JS dengan lastSetCode guard
 * - onWebViewReady() flush pending
 *
 * Gutter 40px, debounce 100ms note tetap ada untuk anti-regresi
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
                ZcodeWebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }
                    // SOFTWARE lebih stabil untuk keyboard di banyak device
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Fix generic WebView keyboard bug #7189
                            view?.requestFocus(View.FOCUS_DOWN)
                            view?.requestFocusFromTouch()
                            view?.evaluateJavascript(
                                "setCode(${escapeJavaScriptString(code)});",
                                null
                            )
                            view?.evaluateJavascript("onWebViewReady();", null)
                            postDelayed({
                                webViewReady = true
                                pendingFileCode?.let { (_, c) ->
                                    evaluateJavascript("setCode(${escapeJavaScriptString(c)});", null)
                                    pendingFileCode = null
                                }
                            }, 150)
                        }
                    }

                    // Fix keyboard not showing — minimal, jangan return true
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN,
                            android.view.MotionEvent.ACTION_UP -> {
                                if (!v.hasFocus()) {
                                    v.requestFocus()
                                    v.requestFocusFromTouch()
                                }
                            }
                        }
                        false // biarkan WebView handle touch untuk Ace
                    }

                    addJavascriptInterface(EditorBridge(onCodeChange), "ZCODE")
                    loadUrl("file:///android_asset/editor/index.html")
                    webViewRef.value = this
                }
            },
            update = { /* no-op, setCode via LaunchedEffect + pushCode */ }
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
