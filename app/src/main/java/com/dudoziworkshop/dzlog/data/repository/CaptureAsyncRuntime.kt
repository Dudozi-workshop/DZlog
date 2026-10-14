package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.model.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-lifetime capture worker. It must not reference a Composable or Activity.
 * The saved shot snapshot owns its counter stream even if the camera changes folders.
 */
internal object CaptureAsyncRuntime {
    @Volatile private var service: CaptureAsyncCaptureService? = null

    fun get(context: Context, repository: DzlogRepositoryImpl): CaptureAsyncCaptureService =
        service ?: synchronized(this) {
            service ?: CaptureAsyncCaptureService(
                context = context.applicationContext,
                repository = repository,
                applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                commitProgress = { staged, entry: LogEntry ->
                    val recorded = requireNotNull(staged.snapshot.counterStream) {
                        "Counter stream missing from durable capture snapshot"
                    }
                    CaptureCounterPolicy.commit(
                        context = context.applicationContext,
                        key = recorded,
                        usedCounter = staged.reservation.counter,
                        mediaStoreId = entry.mediaStoreId,
                    )
                },
            ).also { service = it }
        }
}
