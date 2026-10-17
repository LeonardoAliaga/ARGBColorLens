
package io.github.leonardoaliaga.argbcolorlens

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiReferenceExpression
import com.intellij.ui.ColorChooserService

/**
 * Acción para editar colores ARGB.
 *
 * Permite elegir entre:
 * - Modificar solo la referencia seleccionada.
 * - Modificar la constante original.
 */
class EditArgbColorAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {

        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)

        e.presentation.isEnabledAndVisible =
            editor != null &&
                    file != null &&
                    ArgbPsi.expressionAtCaret(
                        file,
                        editor.caretModel.offset
                    ) != null
    }

    override fun actionPerformed(e: AnActionEvent) {

        val editor = e.getData(CommonDataKeys.EDITOR)
            ?: return

        val file = e.getData(CommonDataKeys.PSI_FILE)
            ?: return

        val expression = ArgbPsi.expressionAtCaret(
            file,
            editor.caretModel.offset
        ) ?: return

        val originalColor = ArgbPsi.colorOf(expression)
            ?: return

        // Buscar la constante original si es una referencia.
        val field = if (expression is PsiReferenceExpression) {
            ArgbPsi.sourceField(expression)
        } else {
            null
        }

        // Elegir dónde aplicar el cambio.
        val editOriginal = if (field != null) {

            val choice = Messages.showDialog(
                e.project,
                "¿Dónde deseas aplicar el nuevo color de ${field.name}?",
                "ARGB Color Lens",
                arrayOf(
                    "Solo este uso",
                    "Constante original",
                    "Cancelar"
                ),
                0,
                Messages.getQuestionIcon()
            )

            when (choice) {
                0 -> false
                1 -> true
                else -> return
            }

        } else {
            false
        }

        // Abrir selector nativo de IntelliJ.
        // El cambio se aplica después de confirmar.
        val chosen = ColorChooserService.getInstance().showDialog(
            e.project,
            editor.contentComponent,
            "ARGB Color Lens: seleccionar color",
            originalColor,
            true,
            emptyList(),
            false
        ) ?: return

        // Aplicar color.
        if (editOriginal && field != null) {
            ArgbPsi.replaceOriginal(field, chosen)
        } else {
            ArgbPsi.replaceColor(expression, chosen)
        }
    }
}
