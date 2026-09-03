package com.brave.jsabmusic.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView

/**
 * Hardened WebView engineered for:
 * 1. Flawless IME Keyboard & Touch Focus (Phone OTP & Password Sign-In support).
 * 2. Chromium C++ Visibility Decoupling (Screen-Off background audio preservation).
 */
class BackgroundWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        // Informs Chromium that the view is VISIBLE so media decoding never pauses
        super.onWindowVisibilityChanged(View.VISIBLE)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        // Keeps Chromium audio thread running
        super.onVisibilityChanged(changedView, View.VISIBLE)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Ensure tapping input fields claims focus for Samsung Keyboard / Gboard
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!hasFocus()) {
                requestFocus()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        // Critical for IME keyboard binding during Phone/Email Sign-In
        return super.onCreateInputConnection(outAttrs)
    }
}
