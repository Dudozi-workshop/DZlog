package com.dudoziworkshop.dzlog.domain.time

/** Display text may follow the capture locale; counter scope tokens always use en-US. */
data class CaptureCellTime(val display: String, val scopeToken: String)

fun resolveCaptureDate(time: CaptureTime, requestedPattern: String): CaptureCellTime {
    val pattern = when (requestedPattern) {
        "yyyyMMdd", "yyMMdd", "MMdd" -> requestedPattern
        else -> "yyyyMMdd"
    }
    return CaptureCellTime(
        formatCaptureTime(pattern, time),
        formatCaptureTime("yyyyMMdd", time.copy(localeTag = "en-US")),
    )
}

/** TIME cells deliberately ignore display settings and seconds, preserving minute scopes. */
fun resolveCaptureMinute(time: CaptureTime): CaptureCellTime = CaptureCellTime(
    formatCaptureTime("HHmm", time),
    formatCaptureTime("HHmm", time.copy(localeTag = "en-US")),
)
