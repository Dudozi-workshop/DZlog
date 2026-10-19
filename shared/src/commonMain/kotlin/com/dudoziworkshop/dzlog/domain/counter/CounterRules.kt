package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.CellKey
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.naming.sanitizeFilePart
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell

/** Pure policy only. The platform supplies the set of counters read from real media. */
object CounterRules {
    fun computeCounterStreamRelativePathKey(
        baseRelativePath: String,
        hasG2Group: Boolean,
        group2Value: String
    ): String {
        if (!hasG2Group) return baseRelativePath
        if (group2Value.isNotBlank()) return baseRelativePath
        // Virtual stream key: distinguish empty-enabled G2 from true G1-only.
        return "$baseRelativePath|g2=enabled_empty"
    }

    fun computeCounterPrefix(
        resolvedCells: List<ResolvedCell>,
        fnDelim: String,
        fileNameSlots: List<CellKey?>,
        includeFilenameInScope: Boolean,
        scopeOptions: CounterScopeOptions = CounterScopeOptions(),
    ): String {
        val delim = fnDelim.ifBlank { "_" }
        val normalizedSlots = fileNameSlots.take(FILE_NAME_SLOT_COUNT) + List((FILE_NAME_SLOT_COUNT - fileNameSlots.size).coerceAtLeast(0)) { null }

        val slotParts = if (includeFilenameInScope) {
            normalizedSlots
                .asSequence()
                .mapNotNull { slot -> slot?.let { key -> resolvedCells.firstOrNull { rc -> rc.id == key } } }
                .mapNotNull { rc ->
                    val token: String? = when (rc.type) {
                        TableCellDataType.COUNTER -> null
                        TableCellDataType.DATE,
                        TableCellDataType.TIME -> null
                        TableCellDataType.ROTATING_TEXT -> {
                            // 정책: ROTATING_TEXT 분리는 phraseScopeValues(rp_...)를 단일 기준으로 사용한다.
                            // 중복 분리를 막기 위해 slot 토큰에서는 제외한다.
                            null
                        }
                        else -> rc.resolvedText
                    }

                    token
                        ?.let { sanitizeFilePart(it) }
                        ?.takeIf { it.isNotBlank() }
                }
                .toList()
        } else {
            emptyList()
        }

        val dateParts = scopeOptions.dateScopeValues
            .asSequence()
            .mapNotNull { sanitizeFilePart(it).takeIf { part -> part.isNotBlank() } }
            .map { "d_$it" }
            .toList()

        val timeParts = scopeOptions.timeScopeValues
            .asSequence()
            .mapNotNull { sanitizeFilePart(it).takeIf { part -> part.isNotBlank() } }
            .map { "t_$it" }
            .toList()

        val phraseParts = scopeOptions.phraseScopeValues
            .asSequence()
            .mapNotNull { sanitizeFilePart(it).takeIf { part -> part.isNotBlank() } }
            .toList()

        val filenameDraftParts = scopeOptions.filenameDraftScopeValues
            .asSequence()
            .mapNotNull { sanitizeFilePart(it).takeIf { part -> part.isNotBlank() } }
            .map { "fd_$it" }
            .toList()

        // 핵심 정책:
        // - filename scope ON  -> slot + phrase/date/time
        // - filename scope OFF -> 파일명 기반 스코프 토큰은 전부 무시
        val scopedParts = buildList {
            addAll(filenameDraftParts)
            addAll(phraseParts)
            addAll(dateParts)
            addAll(timeParts)
        }

        val parts = buildList {
            if (includeFilenameInScope) {
                addAll(slotParts)
                addAll(scopedParts)
            } else {
                add("name=off")
            }
        }

        val base = if (parts.isEmpty()) "DZlog" else parts.joinToString(delim)
        // Ordinary filename scope OFF must not erase the phrase's explicit policy.
        val rotatingPolicy = scopeOptions.rotatingPolicyScopeValues.joinToString(",") {
            encodeCounterToken(it)
        }
        return if (!includeFilenameInScope && rotatingPolicy.isNotBlank()) {
            "$base|rotating=$rotatingPolicy"
        } else base
    }

    fun computeCounterStreamPrefix(
        resolvedCells: List<ResolvedCell>,
        fnDelim: String,
        fileNameSlots: List<CellKey?>,
        includeFilenameInScope: Boolean,
        scopeOptions: CounterScopeOptions = CounterScopeOptions(),
    ): String {
        val basePrefix = computeCounterPrefix(
            resolvedCells = resolvedCells,
            fnDelim = fnDelim,
            fileNameSlots = fileNameSlots,
            includeFilenameInScope = includeFilenameInScope,
            scopeOptions = scopeOptions,
        )
        val g2Enabled = resolvedCells.any { it.raw?.groupLevel == GroupLevel.G2 }
        val tag = if (g2Enabled) "g2=1" else "g2=0"
        return "$basePrefix|$tag"
    }

    fun nextFromUsed(existingCounters: Set<Int>): Int {
        return (existingCounters.maxOrNull() ?: 0) + 1
    }
}
