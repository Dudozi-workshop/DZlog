package com.dudoziworkshop.dzlog.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.debug.CounterDebugDump
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class HomeUiState(
    val settings: AppSettings = AppSettings.Default,
    val nextCounterPreview: Int = 1,
    val latestImage: MediaImageItem? = null,
    val latestImageTimeText: String = "-",
)

internal class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var activeTemplateState: TableTemplateState? = null
    private var previewJob: Job? = null
    private var isActive = false

    init {
        viewModelScope.launch {
            AppSettingsStore.flow(appContext).collectLatest { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
                if (isActive) {
                    activeTemplateState?.let { recomputeCounter(it, settings) }
                }
            }
        }
    }

    fun activate(templateState: TableTemplateState) {
        isActive = true
        updateTemplate(templateState)
        refreshLatestImage()
    }

    fun updateTemplate(templateState: TableTemplateState) {
        activeTemplateState = templateState
        if (!isActive) return

        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val unit = decideTickUnitFromTemplate(templateState.cells)
            while (isActive) {
                recomputeCounter(templateState, _uiState.value.settings)
                delay(computeNextDelayMillis(unit))
            }
        }
    }

    fun deactivate() {
        isActive = false
        previewJob?.cancel()
        previewJob = null
    }

    fun refreshLatestImage() {
        if (!isActive) return
        viewModelScope.launch {
            val latest = withContext(Dispatchers.IO) {
                runCatching {
                    DzlogMediaStoreReader(appContext.contentResolver).loadLatestImage()
                }.getOrNull()
            }
            _uiState.value = _uiState.value.copy(
                latestImage = latest,
                latestImageTimeText = formatRecentCaptureTime(latest?.dateAddedSeconds ?: 0L),
            )
        }
    }

    private fun formatRecentCaptureTime(dateAddedSeconds: Long): String {
        if (dateAddedSeconds <= 0L) return "-"
        val captureDate = Date(dateAddedSeconds * 1_000L)
        val todayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isToday = todayKey.format(captureDate) == todayKey.format(Date())
        return if (isToday) {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(captureDate)
            appContext.getString(com.dudoziworkshop.dzlog.R.string.home_today_time, time)
        } else {
            val pattern = appContext.getString(com.dudoziworkshop.dzlog.R.string.home_date_time_pattern)
            SimpleDateFormat(pattern, Locale.getDefault()).format(captureDate)
        }
    }

    private suspend fun recomputeCounter(
        templateState: TableTemplateState,
        settings: AppSettings,
    ) {
        val previewPipeline = buildPreview(
            PreviewInput(
                templateState = templateState,
                captureNow = Date(),
                counterDigits = settings.counterPadding,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                includePathInCounterScope = settings.includePathInCounterScope,
                includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
                saveMode = settings.saveMode,
                scopeNextCounter = HOME_PREVIEW_COUNTER_SEED,
                phraseProgressCursor = HOME_PREVIEW_PHRASE_CURSOR,
            ),
        )
        val counterRequest = CounterRequestResolver.fromHome(
            counterScope = previewPipeline.previewNaming.counterScope,
            saveMode = settings.saveMode,
            scanPrefix = previewPipeline.previewNaming.scanPrefix,
            includePathInScope = settings.includePathInCounterScope,
            includeFilenameInScope = settings.includeFilenameInCounterScope,
        )
        val counterRead = CounterFacade(
            context = appContext,
            counterDigits = settings.counterPadding,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        ).read(counterRequest)

        _uiState.value = _uiState.value.copy(
            nextCounterPreview = counterRead.next.coerceAtLeast(1),
        )

        CounterDebugDump.dump(
            tag = "HomePreview",
            context = appContext,
            scopedStream = counterRead.scopedStream,
            appSettings = settings,
            nextSeed = counterRead.next.coerceAtLeast(1),
            note = null,
        )
    }

    private companion object {
        const val HOME_PREVIEW_COUNTER_SEED = 1
        const val HOME_PREVIEW_PHRASE_CURSOR = 1
    }
}
