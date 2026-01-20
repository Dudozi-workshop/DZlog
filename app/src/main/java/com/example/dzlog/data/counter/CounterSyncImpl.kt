package com.example.dzlog.data.counter

import android.content.Context
import com.example.dzlog.data.mediastore.MediaStoreSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CounterSyncImpl(
    private val saver: MediaStoreSaver
) : CounterSync {

    /**
     * 트리거에서만 호출:
     * - 앱 실행 후 촬영화면 최초 진입 1회
     * - 프로젝트/그룹 변경 시
     * - 설정 저장 시
     * - 앱 재실행 후 촬영화면 재진입 시
     */
    override fun syncNextCounter(
        context: Context,
        relativePath: String,
        counterDigits: Int,
        fnDelim: String,
        onResult: (nextCounter: Int) -> Unit,
        onFail: (String) -> Unit
    ) {
        // UI 스레드 막지 않게 백그라운드
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val max = withContext(Dispatchers.IO) {
                    saver.findMaxCounterInFolder(
                        context = context,
                        relativePath = relativePath,
                        counterDigits = counterDigits,
                        fnDelim = fnDelim
                    )
                }
                onResult(max + 1)
            } catch (e: Exception) {
                onFail(e.message ?: "counter sync failed")
            }
        }
    }
}