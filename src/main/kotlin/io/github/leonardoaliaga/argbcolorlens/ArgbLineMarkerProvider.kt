package io.github.leonardoaliaga.argbcolorlens

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.ui.ColorChooserService
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

        val expression = when {
            element is PsiIdentifier -> {
                val reference = element.parent as? PsiReferenceExpression
                reference?.takeIf {
                    it.referenceNameElement == element && ArgbPsi.sourceField(it) != null
                }
            }
            else -> element.parent as? PsiLiteralExpression
        } ?: return null

        val color = ArgbPsi.colorOf(expression) ?: return null
        val hex = "0x%08X".format(color.rgb)
        val definition = expression is PsiReferenceExpression
        val tooltip = if (definition)
            "$hex — Izquierdo: este uso; derecho: constante original"
        else
            "$hex — Izquierdo o derecho: editar literal"

        val anchor = SmartPointerManager.createPointer(element)

        return object : LineMarkerInfo<PsiElement>(
            element, element.textRange, ArgbSwatchIcon(color.rgb),
            null, null, GutterIconRenderer.Alignment.CENTER,
            Supplier { "ARGB Color Lens: $hex" }
        ) {
            override fun createGutterRenderer(): GutterIconRenderer =
                object : LineMarkerGutterIconRenderer<PsiElement>(this) {
                    override fun getTooltipText(): String = tooltip

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
    private val pointer: SmartPsiElementPointer<PsiElement>,
    private val editDefinition: Boolean
) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val leaf = pointer.element ?: return
        val expression = expressionAt(leaf) ?: return
        val originalColor = ArgbPsi.colorOf(expression) ?: return
        val project = e.project ?: return

        val parent: Component? = e.getData(CommonDataKeys.EDITOR)?.contentComponent
            ?: e.inputEvent?.source as? Component

        val title = if (editDefinition && expression is PsiReferenceExpression)
            "ARGB Color Lens: editar constante original"
        else
            "ARGB Color Lens: editar este uso"

        val selected = ColorChooserService.getInstance().showDialog(
            project, parent, title, originalColor, true, emptyList(), false
        ) ?: return

        // Never use the expression captured before showing the modal dialog.
        val current = pointer.element ?: return
        val currentExpression = expressionAt(current) ?: return

        if (editDefinition && currentExpression is PsiReferenceExpression) {
            val field = ArgbPsi.sourceField(currentExpression) ?: return
            if (!field.isWritable) {
                Messages.showWarningDialog(project, "La constante es de solo lectura.", "ARGB Color Lens")
                return
            }
            ArgbPsi.replaceOriginal(field, selected)
        } else {
            if (!currentExpression.isWritable) {
                Messages.showWarningDialog(project, "El archivo es de solo lectura.", "ARGB Color Lens")
                return
            }
            ArgbPsi.replaceColor(currentExpression, selected)
        }
    }

    private fun expressionAt(element: PsiElement): PsiExpression? {
        if (!element.isValid) return null
        return when (element) {
            is PsiIdentifier -> (element.parent as? PsiReferenceExpression)
                ?.takeIf { it.referenceNameElement == element && ArgbPsi.sourceField(it) != null }
            is PsiJavaToken -> (element.parent as? PsiLiteralExpression)
                ?.takeIf { ArgbPsi.colorOf(it) != null }
            else -> null
        }
    }
}

/** Transparency checkerboard + an ARGB overlay with a distinct border. */
private class ArgbSwatchIcon(private val argb: Int) : Icon {
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
