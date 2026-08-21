package com.stockmaster.app.util

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 外接 USB/蓝牙扫码枪全局监听。
 * 扫码枪以极快速度（<120ms/键）连续输入字符并以 Enter 结尾。
 * 文本框聚焦时不拦截，避免干扰正常输入（与 Web 版行为一致）。
 */
object ScannerGun {

    var textInputFocused by mutableStateOf(false)

    private val buffer = StringBuilder()
    private var lastKeyTime = 0L

    /** 最近一次扫描（码 + 时间戳），用于同码防重。 */
    private var lastScanned: Pair<String, Long>? = null

    /** 返回 true 表示事件已被消费。 */
    fun handleKeyEvent(keyEvent: KeyEvent?): Boolean {
        if (textInputFocused) return false
        if (keyEvent == null) return false

        val action = keyEvent.action
        val keyCode = keyEvent.keyCode

        if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            val code = buffer.toString().trim()
            buffer.clear()
            if (code.length >= 3) {
                val now = SystemClock.uptimeMillis()
                val isDuplicate = lastScanned?.let { it.first == code && now - it.second < 1200 } ?: false
                lastScanned = code to now
                if (!isDuplicate) onScanned?.invoke(code)
                return true
            }
            return false
        }

        if (action == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_DEL) {
            val unicode = keyEvent.unicodeChar
            if (unicode != 0) {
                val now = SystemClock.uptimeMillis()
                if (now - lastKeyTime > 120) buffer.clear()
                lastKeyTime = now
                buffer.append(unicode.toChar())
                return true
            }
        }
        return false
    }

    var onScanned: ((String) -> Unit)? = null
}