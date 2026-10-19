package com.dudoziworkshop.dzlog.data.platform

internal expect fun newTemplateId(): String
internal expect fun backupNowMillis(): Long
internal expect fun backupSha256(payload: String): String
