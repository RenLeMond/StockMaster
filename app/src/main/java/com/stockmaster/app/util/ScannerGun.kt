package com.stockmaster.app.util

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * 外接 USB/蓝牙扫码枪全局监听。
 *
 * 扫码枪以极快速度（通常 10~50ms/键）连续输入字符并以 Enter 结尾。
 * 捕获策略：
 * - 长空闲后的首个字符「乐观进入捕获态」（扫码枪触发前一般是长时间无输入）；
 * - 键间隔 ≤[MAX_GAP_MS] 持续续接捕获，超时则整段丢弃并退出捕获态；
 * - 退出捕获态后到下次重新进入前的零散按键全部放行，外接键盘正常打字不再被吞；
 * - 缓冲设上限，卡键/重复风暴时丢弃整段，防止无限累积。
 *
 * 文本框聚焦时不拦截。焦点采用引用计数而非单一布尔：
 * 避免「A 失焦回调晚于 B 聚焦回调」的复位竞态，以及携带焦点的组合被移除、
 * clear 事件未派发导致扫码枪全局卡死的问题。
 */
object ScannerGun {

    private const val MAX_GAP_MS = 100L
    private const val MIN_CODE_LEN = 3
    private const val MAX_BUFFER = 64

    private val focusHolders =
        Collections.newSetFromMap(ConcurrentHashMap<Any, Boolean>())

    var textInputFocused by mutableStateOf(false)
        private set

    fun acquireFocus(holder: Any) {
        focusHolders.add(holder)
        textInputFocused = focusHolders.isNotEmpty()
    }

    fun releaseFocus(holder: Any) {
        focusHolders.remove(holder)
        textInputFocused = focusHolders.isNotEmpty()
    }

    private val buffer = StringBuilder()
    private var lastKeyTime = 0L
    private var capturing = false

    /** 最近一次扫描（码 + 时间戳），用于同码防重。 */
    private var lastScanned: Pair<String, Long>? = null

    /** 返回 true 表示事件已被消费。 */
    fun handleKeyEvent(keyEvent: KeyEvent?): Boolean {
        if (textInputFocused) return false
        if (keyEvent == null || keyEvent.action != KeyEvent.ACTION_DOWN) return false
        val keyCode = keyEvent.keyCode

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            val code = buffer.toString().trim()
            buffer.clear()
            capturing = false
            if (code.length >= MIN_CODE_LEN) {
                val now = SystemClock.uptimeMillis()
                val isDuplicate = lastScanned?.let { it.first == code && now - it.second < 1200 } ?: false
                lastScanned = code to now
                if (!isDuplicate) onScanned?.invoke(code)
                return true
            }
            // 过短缓冲不是有效条码：放行 Enter，不做任何拦截
            return false
        }

        if (keyCode != KeyEvent.KEYCODE_DEL) {
            val unicode = keyEvent.unicodeChar
            if (unicode != 0) {
                val now = SystemClock.uptimeMillis()
                if (now - lastKeyTime > MAX_GAP_MS) {
                    // 新序列起点：乐观进入捕获态
                    buffer.clear()
                    capturing = true
                } else if (!capturing) {
                    // 不在捕获态的零散按键放行给系统
                    return false
                }
                if (buffer.length >= MAX_BUFFER) {
                    // 卡键/key-repeat 保护：丢弃整段
                    buffer.clear()
                    return true
                }
                lastKeyTime = now
                buffer.append(unicode.toChar())
                return true
            }
        }
        return false
    }

    var onScanned: ((String) -> Unit)? = null
}
