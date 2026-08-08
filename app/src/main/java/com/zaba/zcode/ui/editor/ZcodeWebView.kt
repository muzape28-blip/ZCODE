package com.zaba.zcode.ui.editor

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView

/**
 * ZcodeWebView — custom WebView untuk Ace Editor di Android
 * FIX keyboard tidak muncul saat tap (issue generic WebView + Ace)
 * Referensi: StackOverflow Android WebView keyboard not showing, Ace issue #3450
 * - onCheckIsTextEditor() true → paksa sistem anggap WebView adalah text editor
 * - onCreateInputConnection → return BaseInputConnection agar IME bisa attach
 * - focusable true
 *
 * Kombinasi ini yang membuat textarea Ace bisa munculkan keyboard di semua device
 * termasuk Samsung, Xiaomi, Android 11-14.
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
        return true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        // Jika outAttrs diset, IME akan muncul
        // Pakai BaseInputConnection agar tidak crash saat Ace hidden textarea
        // outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE
        // outAttrs.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        // Return base connection, tapi tetap coba super untuk kompat
        return try {
            val superConn = super.onCreateInputConnection(outAttrs)
            superConn ?: BaseInputConnection(this, false)
        } catch (e: Exception) {
            BaseInputConnection(this, false)
        }
    }
}
