package io.github.leonardoaliaga.argbcolorlens

import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.openapi.editor.GutterMarkPreprocessor
import com.intellij.openapi.util.TextRange
import com.intellij.util.ui.ColorIcon

/** Immutable source ranges captured when an ARGB swatch is collected. */
internal interface ArgbSwatchRenderer {
    val colorRanges: List<TextRange>
}

/** Prefer our mouse actions when IntelliJ also supplies a swatch for the same color. */
class ArgbGutterMarkPreprocessor : GutterMarkPreprocessor {
    override fun processMarkers(list: List<GutterMark>): List<GutterMark> {
        val colorRanges = list.filterIsInstance<ArgbSwatchRenderer>().flatMap { it.colorRanges }
        if (colorRanges.isEmpty()) return list

        return list.flatMap { mark ->
            val renderer = mark as? LineMarkerInfo.LineMarkerGutterIconRenderer<*>
                ?: return@flatMap listOf(mark)
            val info = renderer.lineMarkerInfo
            val merged = MergeableLineMarkerInfo.getMergedMarkers(info)
            if (merged.isEmpty()) {
                if (isDuplicate(info, colorRanges)) emptyList() else listOf(mark)
            } else {
                // A grouped native icon can also contain colors we do not handle.
                // Keep their original renderers and actions instead of hiding the group.
                val remaining = merged.filterNot { isDuplicate(it, colorRanges) }
                if (remaining.size == merged.size) listOf(mark)
                else remaining.mapNotNull { it.createGutterRenderer() }
            }
        }
    }

    private fun isDuplicate(info: LineMarkerInfo<*>, colorRanges: List<TextRange>): Boolean {
        if (info.icon !is ColorIcon) return false
        return colorRanges.any { range ->
            range.startOffset <= info.startOffset && info.endOffset <= range.endOffset
        }
    }
}
