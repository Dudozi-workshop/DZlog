package com.dudoziworkshop.dzlog.data.repository

import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureStreamKey
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import org.json.JSONArray
import org.json.JSONObject

/** Immutable capture-time values needed to finish a staged photo after process death. */
internal data class CaptureJobSnapshot(
    val request: CaptureRequest,
    val tablePatch: TablePatch,
    val counterStream: CaptureStreamKey? = null,
    /** Immutable catalog identity, distinct from the template's table contents. */
    val templateId: String? = null,
    /** Frozen at shutter reservation; null means ordinary shooting. */
    val projectId: String? = null,
)

/** Private on-disk format; reject unknown schemas rather than silently losing a setting. */
internal object CaptureJobSnapshotCodec {
    fun encode(job: CaptureJobSnapshot): JSONObject {
        val r = job.request
        val wm = r.watermark
        val cells = JSONArray().apply {
            r.resolvedCells.forEach { cell ->
                put(JSONObject().put("id", cell.id).put("type", cell.type.name)
                    .put("text", cell.resolvedText).put("empty", cell.isEmpty)
                    .put("scopeToken", cell.scopeToken ?: JSONObject.NULL))
            }
        }
        val watermarks = JSONArray().apply {
            r.watermarkCells.forEach { put(it.valueText) }
        }
        val updates = JSONObject().apply {
            job.tablePatch.updatesByCellId.forEach { (key, value) -> put(key, value) }
        }
        val progress = JSONObject().apply {
            job.tablePatch.phraseProgressByCellId.forEach { (key, value) ->
                put(key, JSONObject().put("index", value.index)
                    .put("completedShots", value.completedShots))
            }
        }
        val watermark = JSONObject()
            .put("anchor", wm.anchor.name)
            .put("offsetXRatio", wm.offsetXRatio).put("offsetYRatio", wm.offsetYRatio)
            .put("boundsOffsetX10000", wm.boundsOffsetX10000)
            .put("boundsOffsetY10000", wm.boundsOffsetY10000)
            .put("tableHeightRatio", wm.tableHeightRatio)
            .put("tableWidthRatio", wm.tableWidthRatio)
            .put("tableBgAlpha", wm.tableBgAlpha).put("bgStyle", wm.bgStyle)
            .put("valueScale", wm.valueScale)
            .put("textColorMode", wm.textColorMode)
            .put("manualTextColor", wm.manualTextColor)
            .put("textAlign", wm.textAlign)
            .put("gridEnabled", wm.gridEnabled)
            .put("rotationCwDeg", wm.rotationCwDeg)

        return JSONObject()
            .put("version", 1)
            .put("relativePath", r.relativePath)
            .put("group1", r.group1).put("group2", r.group2)
            .put("displayName", r.displayName)
            .put("saveMode", r.saveMode.name)
            .put("captureAspect", r.captureAspect.name)
            .put("photoQualityMode", r.photoQualityMode.name)
            .put("usableTopRatio", r.usableTopRatio.toDouble())
            .put("usableBottomRatio", r.usableBottomRatio.toDouble())
            .put("template", r.tableTemplate.toJsonString())
            .put("templateId", job.templateId ?: JSONObject.NULL)
            .put("projectId", job.projectId ?: JSONObject.NULL)
            .put("resolvedCells", cells)
            .put("watermarkCells", watermarks)
            .put("watermark", watermark)
            .put("counterPatch", updates)
            .put("phrasePatch", progress)
            .put("counterStream", job.counterStream?.let { stream ->
                JSONObject().put("relativePathKey", stream.relativePathKey)
                    .put("prefix", stream.prefix).put("scanPrefix", stream.scanPrefix)
            } ?: JSONObject.NULL)
    }

    fun decode(data: JSONObject): CaptureJobSnapshot {
        require(data.getInt("version") == 1) { "Unsupported capture snapshot" }
        val template = requireNotNull(tableTemplateStateFromJson(data.getString("template"))) {
            "Invalid staged template"
        }
        val rawById = template.cells.associateBy { it.cellId }
        val setsById = template.phraseSets.associateBy { it.id }
        val resolved = data.getJSONArray("resolvedCells").let { arr ->
            List(arr.length()) { i ->
                val cell = arr.getJSONObject(i)
                val id = cell.getString("id")
                val raw = rawById[id]
                ResolvedCell(
                    id = id,
                    type = TableCellDataType.valueOf(cell.getString("type")),
                    raw = raw,
                    resolvedText = cell.getString("text"),
                    isEmpty = cell.getBoolean("empty"),
                    scopeToken = if (cell.isNull("scopeToken")) null else cell.getString("scopeToken"),
                    rotatingPhraseSet = raw?.phraseSetId?.let(setsById::get),
                )
            }
        }
        val wmCells = data.getJSONArray("watermarkCells").let { arr ->
            List(arr.length()) { i -> WatermarkBuilder.WatermarkCell(arr.getString(i)) }
        }
        val wm = data.getJSONObject("watermark").let {
            WatermarkConfig(
                anchor = WatermarkTableAnchor.valueOf(it.getString("anchor")),
                offsetXRatio = it.getInt("offsetXRatio"),
                offsetYRatio = it.getInt("offsetYRatio"),
                boundsOffsetX10000 = it.getInt("boundsOffsetX10000"),
                boundsOffsetY10000 = it.getInt("boundsOffsetY10000"),
                tableHeightRatio = it.getInt("tableHeightRatio"),
                tableWidthRatio = it.getInt("tableWidthRatio"),
                tableBgAlpha = it.getInt("tableBgAlpha"),
                bgStyle = it.getInt("bgStyle"),
                valueScale = it.getInt("valueScale"),
                textColorMode = it.getInt("textColorMode"),
                manualTextColor = it.getInt("manualTextColor"),
                textAlign = it.getInt("textAlign"),
                gridEnabled = it.getBoolean("gridEnabled"),
                rotationCwDeg = it.getInt("rotationCwDeg"),
            )
        }
        val updates = data.getJSONObject("counterPatch").let { obj ->
            obj.keys().asSequence().associateWith(obj::getString)
        }
        val phrases = data.getJSONObject("phrasePatch").let { obj ->
            obj.keys().asSequence().associateWith { key ->
                val progress = obj.getJSONObject(key)
                CellPhraseProgress(progress.getInt("index"), progress.getInt("completedShots"))
            }
        }
        return CaptureJobSnapshot(
            request = CaptureRequest(
                relativePath = data.getString("relativePath"),
                group1 = data.getString("group1"),
                group2 = data.getString("group2"),
                displayName = data.getString("displayName"),
                resolvedCells = resolved,
                watermarkCells = wmCells,
                saveMode = SaveMode.valueOf(data.getString("saveMode")),
                captureAspect = CaptureAspect.valueOf(data.getString("captureAspect")),
                photoQualityMode = PhotoQualityMode.valueOf(data.getString("photoQualityMode")),
                tableTemplate = template,
                usableTopRatio = data.getDouble("usableTopRatio").toFloat(),
                usableBottomRatio = data.getDouble("usableBottomRatio").toFloat(),
                watermark = wm,
            ),
            tablePatch = TablePatch(updates, phrases),
            templateId = data.optString("templateId").takeIf {
                it.isNotBlank() && it != "null"
            },
            projectId = data.optString("projectId").takeIf {
                it.isNotBlank() && it != "null"
            },
            counterStream = data.optJSONObject("counterStream")?.let { stream ->
                CaptureStreamKey(
                    relativePathKey = stream.getString("relativePathKey"),
                    prefix = stream.getString("prefix"),
                    scanPrefix = stream.getString("scanPrefix"),
                )
            },
        )
    }
}
