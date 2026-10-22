package io.github.leonardoaliaga.argbcolorlens

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.ui.ColorChooserService
import com.intellij.ui.ColorPickerListener
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.util.function.Supplier
import javax.swing.Icon

/**
 * A single, clickable color swatch in Java gutters.
 * Left-click edits only the expression; right-click edits its source field.
 * No ElementColorProvider is registered alongside this provider.
 */
class ArgbLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Mark only leaf nodes to avoid flickering in IntelliJ's two-pass inspection.
        if (element !is PsiJavaToken) return null

        val expression = ArgbPsi.expressionForLeaf(element) ?: return null

        val color = ArgbPsi.colorOf(expression) ?: return null
        val hex = "0x%08X".format(color.rgb)
        val definition = expression is PsiReferenceExpression
        // Preserve the detailed color/source context only when source highlighting
        // is disabled. While highlighted, the expression itself already identifies
        // the swatch, so a compact tooltip avoids covering the editor line.
        val expandedTooltip = if (definition) {
            ArgbMessages.message("swatch.reference", hex, expression.text)
        } else {
            ArgbMessages.message("swatch.literal", hex)
        }
        val compactTooltip = ArgbMessages.message(
            if (definition) "swatch.reference.compact" else "swatch.literal.compact"
        )

        val anchor = SmartPointerManager.createPointer(element)
        // Compute these while the daemon holds read access. The gutter filter
        // must not resolve PSI on the UI thread when it removes native duplicates.
        val coveredColorRanges = buildList {
            add(expression.textRange)
            val variable = expression.parent as? PsiVariable
            if (variable != null && variable.initializer == expression) {
                variable.nameIdentifier?.textRange?.let(::add)
            }
        }

        return object : LineMarkerInfo<PsiElement>(
            element, element.textRange, ArgbSwatchIcon(color.rgb),
            null, null, GutterIconRenderer.Alignment.CENTER,
            Supplier { "ARGB Color Lens: $hex" }
        ) {
            override fun createGutterRenderer(): GutterIconRenderer =
                object : LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this), ArgbSwatchRenderer {
                    override val colorRanges: List<TextRange> = coveredColorRanges

                    override fun getTooltipText(): String {
                        ArgbHoverHighlight.show(anchor)
                        return if (ArgbHighlightSettings.getInstance().snapshot().enabled) {
                            compactTooltip
                        } else {
                            expandedTooltip
                        }
                    }

                    override fun isNavigateAction(): Boolean = true

                    override fun getClickAction(): AnAction =
                        EditColorAction(anchor, false)

                    override fun getRightButtonClickAction(): AnAction =
                        EditColorAction(anchor, true)
                }
        }
    }
}

/**
 * Holds a smart pointer to the gutter's leaf token, not a live PsiExpression.
 * The PSI is resolved again after the picker closes, so edits are atomic.
 */
private class EditColorAction(
    private val pointer: SmartPsiElementPointer<out PsiElement>,
    private val editDefinition: Boolean
) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val leaf = pointer.element ?: return
        val expression = ArgbPsi.expressionForLeaf(leaf) ?: return
        val originalColor = ArgbPsi.colorOf(expression) ?: return
        val project = e.project ?: return

        val parent: Component = e.getData(CommonDataKeys.EDITOR)?.contentComponent
            ?: (e.inputEvent?.source as? Component)
            ?: return

        val title = ArgbMessages.message(
            if (editDefinition && expression is PsiReferenceExpression)
                "dialog.edit.definition" else "dialog.edit.usage"
        )

        // IntelliJ treats an EMPTY listener list as the "Copy color" mode.
        // A non-empty listener list enables "Choose" (confirm the selection).
        // No changes are applied during live previews; only after confirmation.
        val selectionMode = object : ColorPickerListener {
            override fun colorChanged(color: Color) = Unit
            override fun closed(color: Color?) = Unit
        }

        val selected = ColorChooserService.getInstance().showDialog(
            project, parent, title, originalColor, true, listOf(selectionMode), false
        ) ?: return

        // Never use the expression captured before showing the modal dialog.
        val current = pointer.element ?: return
        val currentExpression = ArgbPsi.expressionForLeaf(current) ?: return

        if (editDefinition && currentExpression is PsiReferenceExpression) {
            val field = ArgbPsi.sourceField(currentExpression) ?: return
            if (!field.isWritable) {
                Messages.showWarningDialog(project, ArgbMessages.message("warning.readonly.definition"), "ARGB Color Lens")
                return
            }
            ArgbPsi.replaceOriginal(field, selected)
        } else {
            if (!currentExpression.isWritable) {
                Messages.showWarningDialog(project, ArgbMessages.message("warning.readonly.file"), "ARGB Color Lens")
                return
            }
            ArgbPsi.replaceColor(currentExpression, selected)
        }
    }


}

/** Transparency checkerboard + an ARGB overlay with a distinct border. */
internal class ArgbSwatchIcon(private val argb: Int) : Icon {
    override fun getIconWidth() = 12
    override fun getIconHeight() = 12

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val saved = g.color
        try {
            g.color = Color.WHITE
            g.fillRect(x, y, 12, 12)
            g.color = Color(0xDDDDDD)
            g.fillRect(x, y, 6, 6)
            g.fillRect(x + 6, y + 6, 6, 6)
            g.color = Color(argb, true)
            g.fillRect(x, y, 12, 12)
            g.color = Color(0x666666)
            g.drawRect(x, y, 11, 11)
        } finally {
            g.color = saved
        }
    }

    override fun equals(other: Any?) = other is ArgbSwatchIcon && argb == other.argb
    override fun hashCode() = argb
}
