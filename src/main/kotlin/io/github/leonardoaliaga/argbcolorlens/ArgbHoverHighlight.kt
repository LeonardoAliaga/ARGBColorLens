package io.github.leonardoaliaga.argbcolorlens

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.Font
import java.awt.IllegalComponentStateException
import java.awt.MouseInfo
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * Highlights precisely the Java expression behind the currently hovered
 * gutter swatch. Never changes Java source code or keeps raw PSI across edits.
 */
internal object ArgbHoverHighlight {
    private data class Active(
        val editor: Editor,
        val gutter: Component,
        val point: Point,
        val marker: RangeHighlighter
    )

    private var active: Active? = null

    /** Called when the native gutter requests the tooltip for a swatch. */
    fun show(pointer: SmartPsiElementPointer<out PsiElement>) {
        if (!SwingUtilities.isEventDispatchThread()) return

        val token = pointer.element ?: return
        val expression = ArgbPsi.expressionForLeaf(token) ?: return
        val file = expression.containingFile ?: return
        val document = PsiDocumentManager.getInstance(expression.project)
            .getDocument(file) ?: return
        val mouse = try {
            MouseInfo.getPointerInfo()?.location
        } catch (_: Exception) {
            null
        } ?: return

        val hovered = EditorFactory.getInstance().allEditors.firstNotNullOfOrNull { editor ->
            if (editor.document !== document) return@firstNotNullOfOrNull null
            val gutter = editor.gutter as? Component ?: return@firstNotNullOfOrNull null
            if (!gutter.isShowing) return@firstNotNullOfOrNull null
            val relative = Point(mouse)
            try {
                SwingUtilities.convertPointFromScreen(relative, gutter)
            } catch (_: IllegalComponentStateException) {
                return@firstNotNullOfOrNull null
            }
            if (gutter.contains(relative)) Triple(editor, gutter, relative) else null
        } ?: return

        val (editor, gutter, point) = hovered
        val range = expression.textRange
        if (range.isEmpty || range.endOffset > document.textLength) return

        val old = active
        if (old != null && old.editor === editor && old.marker.isValid &&
            old.marker.startOffset == range.startOffset && old.marker.endOffset == range.endOffset) {
            active = old.copy(point = point)
            return
        }
        clear()

        val attrs = TextAttributes(
            null,
            JBColor(0xD8F1FF, 0x284A5A),
            JBColor(0x358ABB, 0x70C8FF),
            EffectType.ROUNDED_BOX,
            Font.PLAIN
        )
        val marker = editor.markupModel.addRangeHighlighter(
            range.startOffset, range.endOffset, HighlighterLayer.SELECTION - 1,
            attrs, HighlighterTargetArea.EXACT_RANGE
        )
        active = Active(editor, gutter, point, marker)
    }

    /** Detect leaving the current swatch without tracking any PSI elements. */
    fun mouseMoved(event: EditorMouseEvent) {
        val item = active ?: return
        val mouse = event.mouseEvent
        if (event.editor !== item.editor || mouse.component !== item.gutter ||
            kotlin.math.abs(mouse.x - item.point.x) > 10 ||
            kotlin.math.abs(mouse.y - item.point.y) > 10) {
            clear()
        }
    }

    fun clear(editor: Editor? = null) {
        val item = active ?: return
        if (editor != null && editor !== item.editor) return
        active = null
        if (item.marker.isValid) {
            item.editor.markupModel.removeHighlighter(item.marker)
        }
    }
}

/** Application-wide editor motion callback provided by the IntelliJ platform. */
class ArgbHoverMouseMotionListener : EditorMouseMotionListener {
    override fun mouseMoved(e: EditorMouseEvent) {
        ArgbHoverHighlight.mouseMoved(e)
    }
}

/** Clear the transient marker when leaving a gutter or releasing an editor. */
class ArgbHoverEditorFactoryListener : EditorFactoryListener {
    private val listeners = mutableMapOf<Editor, MouseAdapter>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val gutter = editor.gutter as? Component ?: return
        val listener = object : MouseAdapter() {
            override fun mouseExited(e: MouseEvent) {
                ArgbHoverHighlight.clear(editor)
            }
        }
        gutter.addMouseListener(listener)
        listeners[editor] = listener
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val listener = listeners.remove(editor)
        if (listener != null) {
            (editor.gutter as? Component)?.removeMouseListener(listener)
        }
        ArgbHoverHighlight.clear(editor)
    }
}
