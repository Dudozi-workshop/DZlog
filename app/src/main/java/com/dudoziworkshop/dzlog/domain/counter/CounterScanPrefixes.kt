package com.dudoziworkshop.dzlog.domain.counter

import java.net.URLDecoder
import java.net.URLEncoder

/** Finite slot alternatives; no wildcard and no Cartesian expansion. */
object CounterScanPrefixes {
    private const val MARKER = "|rp-scan-v1|" // '|' cannot occur in a sanitized filename.

    fun encode(slotChoices: List<List<String>>): String = MARKER + slotChoices.joinToString("|") { choices ->
        choices.distinct().joinToString(",") { URLEncoder.encode(it, "UTF-8") }
    }

    fun isEncoded(value: String): Boolean = value.startsWith(MARKER)

    fun matches(prefix: String, scanPrefix: String, delimiter: String): Boolean {
        if (!isEncoded(scanPrefix)) return prefix == scanPrefix
        val slots = runCatching {
            scanPrefix.removePrefix(MARKER).split('|').map { group ->
                group.split(',').map { URLDecoder.decode(it, "UTF-8") }
            }
        }.getOrNull() ?: return false
        // States are offsets in the actual prefix, bounded by its length.
        // Empty slots are omitted exactly as they are during filename generation.
        var offsets = setOf(0)
        for (choices in slots) {
            offsets = buildSet {
                for (offset in offsets) for (choice in choices) {
                    if (choice.isEmpty()) {
                        add(offset)
                    } else {
                        val token = if (offset == 0) choice else delimiter + choice
                        if (prefix.startsWith(token, offset)) add(offset + token.length)
                    }
                }
            }
            if (offsets.isEmpty()) return false
        }
        return prefix.length in offsets || (0 in offsets && prefix == "DZlog")
    }
}
