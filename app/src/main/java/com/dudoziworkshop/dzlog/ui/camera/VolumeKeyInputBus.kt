package com.dudoziworkshop.dzlog.ui.camera

import android.view.KeyEvent
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicReference

enum class VolumeKeyPress {
    UP,
    DOWN
}

/**
 * 하드웨어 음량키 이벤트를 Camera 화면으로 전달하는 단일 버스.
 *
 * 정책:
 * - Camera에서 음량키 기능이 ON일 때만 이벤트를 소비한다.
 * - OFF일 때는 시스템 기본 음량 동작을 그대로 유지한다.
 */
object VolumeKeyInputBus {
    private val currentAction = AtomicReference(VolumeKeyAction.NONE)
    private val _events = MutableSharedFlow<VolumeKeyPress>(extraBufferCapacity = 8)
    val events: SharedFlow<VolumeKeyPress> = _events.asSharedFlow()

    fun setVolumeKeyAction(action: VolumeKeyAction) {
        currentAction.set(action)
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val action = currentAction.get()
        if (action == VolumeKeyAction.NONE) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false

        // 정책 보정: CAPTURE는 롱프레스 반복을 무시하고, ZOOM만 반복 입력을 허용한다.
        if (action == VolumeKeyAction.CAPTURE && event.repeatCount > 0) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                _events.tryEmit(VolumeKeyPress.UP)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                _events.tryEmit(VolumeKeyPress.DOWN)
                true
            }
            else -> false
        }
    }
}
