package com.example.dzlog.domain.preview

import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TimeSystem {
        H24,
        H12
    }

fun resolvePreviewCellText(
        cell: TableCellState,
        now: Date,
        nextCounter: Int,
        counterDigits: Int,
        dateFormat: String,
        timeFormat: String,
        timeSystem: TimeSystem
    ): String {
        return when (cell.dataType) {
                TableCellDataType.TEXT,
                TableCellDataType.NUMBER -> cell.valueText

                TableCellDataType.DATE -> {
                        // 예: yyyy.MM.dd, yy.MM.dd
                        SimpleDateFormat(dateFormat, Locale.getDefault()).format(now)
                    }

                TableCellDataType.TIME -> {
                        // 12/24시간제는 포맷 문자열에 영향을 주므로, 12시간제면 HH를 hh로 보정(최소 구현)
                        val fmt = if (timeSystem == TimeSystem.H12) {
                                timeFormat.replace("HH", "hh")
                            } else {
                                timeFormat.replace("hh", "HH")
                            }
                        SimpleDateFormat(fmt, Locale.getDefault()).format(now)
                    }

                TableCellDataType.COUNTER -> {
                       nextCounter.toString().padStart(counterDigits, '0')
                    }
            }
    }
