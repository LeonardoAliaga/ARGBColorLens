package io.github.leonardoaliaga.argbcolorlens

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
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
import kotlin.math.abs

/**
 * Temporary expression highlight for a hovered ARGB gutter icon.
 *
 * The tooltip callback may run off the Swing EDT. We therefore schedule the
 * PSI lookup and editor markup operations on the EDT, where the gutter can
 * also be checked against the current mouse location.
 */
internal object ArgbHoverHighlight {
    private data class Active(
        val editor: Editor,
        val gutter: Component,
        val hoverPoint: Point,
        val highlighter: RangeHighlighter
    )

    private var active: Active? = null

    fun show(pointer: SmartPsiElementPointer<out PsiElement>) {
        if (SwingUtilities.isEventDispatchThread()) {
            showOnEdt(pointer)
        } else {
            SwingUtilities.invokeLater { showOnEdt(pointer) }
        }
    }

    private fun showOnEdt(pointer: SmartPsiElementPointer<out PsiElement>) {
        val token = pointer.element ?: return
        val expression = ArgbPsi.expressionForLeaf(token) ?: return
        val file = expression.containingFile ?: return
        val document = PsiDocumentManager.getInstance(expression.project).getDocument(file) ?: return

        // Do not highlight something in a different editor or an inactive gutter.
        val pointerLocation = try {
            MouseInfo.getPointerInfo()?.location
        } catch (_: Exception) {
            null
        } ?: return

        val hovered = EditorFactory.getInstance().allEditors.firstNotNullOfOrNull { editor ->
            if (editor.document !== document) return@firstNotNullOfOrNull null
            val gutter = editor.gutter as? Component ?: return@firstNotNullOfOrNull null
            if (!gutter.isShowing) return@firstNotNullOfOrNull null

            val localPoint = Point(pointerLocation)
            try {
                SwingUtilities.convertPointFromScreen(localPoint, gutter)
            } catch (_: IllegalComponentStateException) {
                return@firstNotNullOfOrNull null
            }
            if (gutter.contains(localPoint)) Triple(editor, gutter, localPoint) else null
        } ?: return

        val (editor, gutter, point) = hovered
        val range = expression.textRange
        if (range.isEmpty || range.endOffset > document.textLength) return

        val previous = active
        if (previous != null &&
            previous.editor === editor &&
            previous.highlighter.isValid &&
            previous.highlighter.startOffset == range.startOffset &&
            previous.highlighter.endOffset == range.endOffset) {
            active = previous.copy(hoverPoint = point)
            return
        }

        clear()

        val attributes = TextAttributes(
            null,
            JBColor(0xB9E7FA, 0x365568),
            JBColor(0x178BB9, 0x81D8FF),
            EffectType.ROUNDED_BOX,
            Font.PLAIN
        )
        val highlighter = editor.markupModel.addRangeHighlighter(
            range.startOffset,
            range.endOffset,
            HighlighterLayer.SELECTION - 1,
            attributes,
            HighlighterTargetArea.EXACT_RANGE
        )
        active = Active(editor, gutter, point, highlighter)
    }

    /**
     * A gutter Swing mouse listener receives actual gutter mouse events.
     * EditorMouseMotionListener alone does not reliably see these events.
     */
    fun onGutterMouseMoved(editor: Editor, event: MouseEvent) {
        val item = active ?: return
        if (item.editor !== editor ||
            event.component !== item.gutter ||
            abs(event.x - item.hoverPoint.x) > 9 ||
            abs(event.y - item.hoverPoint.y) > 9) {
            clear()
        }
    }

    fun clear(editor: Editor? = null) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { clear(editor) }
            return
        }

        val item = active ?: return
        if (editor != null && item.editor !== editor) return
        active = null
        if (item.highlighter.isValid) {
            item.editor.markupModel.removeHighlighter(item.highlighter)
        }
    }
}

/** The gutter is a Swing component with its own mouse events. */
class ArgbHoverEditorFactoryListener : EditorFactoryListener {
    private val listeners = mutableMapOf<Editor, MouseAdapter>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val gutter = editor.gutter as? Component ?: return
        val listener = object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                ArgbHoverHighlight.onGutterMouseMoved(editor, e)
            }

            override fun mouseExited(e: MouseEvent) {
                ArgbHoverHighlight.clear(editor)
            }
        }
        gutter.addMouseMotionListener(listener)
        gutter.addMouseListener(listener)
        listeners[editor] = listener
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val gutter = editor.gutter as? Component
        val listener = listeners.remove(editor)
        if (listener != null) {
            gutter?.removeMouseMotionListener(listener)
            gutter?.removeMouseListener(listener)
        }
        ArgbHoverHighlight.clear(editor)
    }
}
