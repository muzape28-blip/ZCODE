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
 * FIX lag & blank:
 * - addJavascriptInterface "ZCODE" — TANPA loopback HTTP (file:// murni)
 * - allowFileAccessFromFileURLs + allowUniversalAccessFromFileURLs = true
 *   (FIX blank: Android 11+ butuh ini agar file:///android_asset/... bisa load ace.js)
 * - WebView focusable true (FIX typing: Ace butuh hidden textarea focus)
 * - ZMUX lesson: debounce resize 100ms di MainActivity agar prompt tidak loncat 4-5 baris.
 * - Font editor 12px — di-set di index.html.
 * - FIX blank: LaunchedEffect(fileName) setCode saat ganti file + onPageFinished setCode(initial)
 *   update lambda tidak override typing (hanya setCode saat fileName berubah)
 * - gutter 40px, debounce 100ms note tetap ada (anti-regresi)
 */
@Composable
fun EditorScreen(
    code: String,
    fileName: String?,
    onCodeChange: (String) -> Unit,
    webViewRef: MutableState<WebView?> = remember { mutableStateOf(null) }
) {
    // Track kapan WebView sudah selesai load index.html
    var webViewReady by remember { mutableStateOf(false) }

    // Saat ganti file, dorong code baru ke WebView (kalau sudah ready)
    LaunchedEffect(fileName) {
        if (webViewReady) {
            webViewRef.value?.evaluateJavascript("setCode(${escapeJavaScriptString(code)});", null)
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
                        // FIX blank editor: file:// assets perlu akses file URL
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        // Biar keyboard muncul mulus
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }
                    // FIX typing: WebView harus focusable agar Ace textarea dapat fokus
                    isFocusable = true
                    isFocusableInTouchMode = true

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            webViewReady = true
                            view?.requestFocus()
                            // Initial load = setCode dengan code terkini
                            view?.evaluateJavascript("setCode(${escapeJavaScriptString(code)});", null)
                            // Fokus Ace
                            view?.evaluateJavascript("editor.focus();", null)
                        }
                    }

                    // Tap di WebView → fokus Ace (FIX keyboard muncul tapi char tidak masuk)
                    setOnTouchListener { v, _ ->
                        if (!v.hasFocus()) v.requestFocus()
                        // delay kecil biar focus dulu baru ace focus
                        postDelayed({
                            evaluateJavascript("editor.focus();", null)
                        }, 80)
                        false
                    }

                    addJavascriptInterface(EditorBridge(onCodeChange), "ZCODE")
                    loadUrl("file:///android_asset/editor/index.html")
                    webViewRef.value = this
                }
            },
            update = { webView ->
                // FIX: jangan setCode tiap recompose (bikin kursor loncat & lag)
                // setCode hanya via LaunchedEffect(fileName) + onPageFinished
                // Di sini kita fokus Ace kalau webViewReady
                if (webViewReady) {
                    // tidak override typing
                }
            }
        )
    }
}

/** Escape string ke JS string literal yang aman (baris baru, kutip, backslash, unicode). */
fun escapeJavaScriptString(value: String): String {
    val builder = StringBuilder()
    builder.append("\"")
    for (char in value) {
        when (char) {
            '\\' -> builder.append("\\\\")
            '"' -> builder.append("\\\"")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            '\t' -> builder.append("\\t")
            else -> {
                if (char.code < 32 || char.code > 126) {
                    builder.append(String.format("\\u%04x", char.code))
                } else {
                    builder.append(char)
                }
            }
        }
    }
    builder.append("\"")
    return builder.toString()
}

class EditorBridge(private val onChange: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun onCodeChange(code: String) {
        onChange(code)
    }

    @android.webkit.JavascriptInterface
    fun getCode(): String = ""
}
