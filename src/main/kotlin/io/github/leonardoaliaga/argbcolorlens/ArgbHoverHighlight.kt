package io.github.leonardoaliaga.argbcolorlens

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
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
import com.intellij.openapi.util.TextRange
import java.awt.Color
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

    /** Safe immutable snapshot: no PSI objects escape a read action. */
    private data class HoverTarget(
        val document: Document,
        val range: TextRange,
        val color: Color
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
        val preferences = ArgbHighlightSettings.getInstance().snapshot()
        if (!preferences.enabled) {
            clear()
            return
        }

        // Being on the Swing EDT is NOT equivalent to having PSI read access
        // in IntelliJ Platform 2026.2+. Even pointer.element checks PSI validity.
        // Resolve and snapshot everything from PSI in an explicit read action.
        val target = ReadAction.compute<HoverTarget?, RuntimeException> {
            val token = pointer.element ?: return@compute null
            val expression = ArgbPsi.expressionForLeaf(token) ?: return@compute null
            val sourceColor = ArgbPsi.colorOf(expression) ?: return@compute null
            val file = expression.containingFile ?: return@compute null
            val document = PsiDocumentManager.getInstance(expression.project)
                .getDocument(file) ?: return@compute null
            HoverTarget(document, expression.textRange, sourceColor)
        } ?: return

        val document = target.document

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
        val range = target.range
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

        val chosenColor = if (preferences.useSwatchColor) {
            target.color
        } else {
            Color(preferences.customColorArgb, true)
        }
        val attributes = highlightAttributes(editor, chosenColor)
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
     * Blend the color with the editor theme to keep text readable.
     * RGB drives the highlight even when the swatch's alpha is zero.
     */
    private fun highlightAttributes(editor: Editor, selected: Color): TextAttributes {
        val base = editor.colorsScheme.defaultBackground
        val isDark = (base.red * 299 + base.green * 587 + base.blue * 114) < 128000
        val contrast = if (isDark) Color.WHITE else Color.BLACK
        val background = blend(base, selected, 0.36)
        val outline = blend(selected, contrast, 0.42)
        return TextAttributes(null, background, outline, EffectType.ROUNDED_BOX, Font.PLAIN)
    }

    private fun blend(base: Color, accent: Color, weight: Double): Color {
        fun channel(a: Int, b: Int) = (a + (b - a) * weight).toInt().coerceIn(0, 255)
        return Color(
            channel(base.red, accent.red),
            channel(base.green, accent.green),
            channel(base.blue, accent.blue)
        )
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
