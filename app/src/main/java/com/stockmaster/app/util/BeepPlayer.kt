package com.stockmaster.app.util

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * 扫码枪/摄像头扫描反馈音效（对应 Web 版 Web Audio API 提示音）。
 *
 * 生命周期：ToneGenerator 持有原生音频资源，需在 Activity onDestroy 释放；
 * 若宿主因配置变更重建，re-init 会在下次 play 时懒加载重建。
 */
object BeepPlayer {

    enum class BeepType { SCAN, SUCCESS, ALERT }

    private var toneGen: ToneGenerator? = null
    private var lastType: BeepType? = null
    private var lastTime: Long = 0

    fun play(type: BeepType) {
        val now = System.currentTimeMillis()
        // 防止同一事件的重复提示
        if (type == lastType && now - lastTime < 300) return
        lastType = type
        lastTime = now

        try {
            if (toneGen == null) {
                toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            }
            when (type) {
                BeepType.SCAN -> toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
                BeepType.SUCCESS -> toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 160)
                BeepType.ALERT -> toneGen?.startTone(ToneGenerator.TONE_SUP_ERROR, 150)
            }
        } catch (e: Exception) {
            // 忽略音频初始化失败
        }
    }

    fun release() {
        try {
            toneGen?.release()
        } catch (e: Exception) {
        }
        toneGen = null
    }
}
