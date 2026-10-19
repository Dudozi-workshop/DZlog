package com.dudoziworkshop.dzlog.data.backup

import org.json.JSONArray

/** Existing Android test boundary; all archive validation lives in shared. */
internal object LocalTemplateBackupValidator {
    fun validate(array: JSONArray) = SharedTemplateBackupValidator.validateJson(array.toString())
}
