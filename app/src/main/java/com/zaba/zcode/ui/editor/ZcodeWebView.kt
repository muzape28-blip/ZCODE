package com.zaba.zcode.ui.editor

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

/**
 * ZcodeWebView — custom WebView untuk Ace Editor di Android
 * FIX keyboard tidak muncul — versi hati-hati ( pelajaran ZABACODE )
 *
 * Referensi yang berhasil di ZABACODE (Buildozer WebView) & StackOverflow:
 * - https://stackoverflow.com/questions/3460915/webview-textarea-doesnt-pop-up-the-keyboard (issue #7189)
 *   Fix minimal: requestFocus(View.FOCUS_DOWN) + onTouchListener requestFocus
 *   + overide onCheckIsTextEditor() true SAJA (jangan override onCreateInputConnection dengan BaseInputConnection dummy)
 * - Ace issue #3450: Android butuh full buffer: ace_text-input width 100%
 *
 * Pelajaran ZABACODE: Monaco → Ace migrasi karena Monaco terlalu berat & unusable mobile.
 * ZCODE jangan ulangi dengan hack berlebihan (BaseInputConnection false bikin IME attach ke WebView bukan ke textarea Ace → keyboard hilang)
 *
 * Jadi ZcodeWebView ini MINIMAL: hanya onCheckIsTextEditor true, focusable true.
 * Biarkan WebView super handle InputConnection asli agar textarea Ace dapat IME.
 */
class ZcodeWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onCheckIsTextEditor(): Boolean {
        // Paksa sistem anggap WebView bisa jadi text editor → keyboard boleh muncul
        // Ini fix paling vote di StackOverflow, tanpa side-effect BaseInputConnection
        return true
    }

    // JANGAN override onCreateInputConnection dengan BaseInputConnection dummy
    // Biarkan super handle agar Ace hidden textarea dapat InputConnection asli
}
