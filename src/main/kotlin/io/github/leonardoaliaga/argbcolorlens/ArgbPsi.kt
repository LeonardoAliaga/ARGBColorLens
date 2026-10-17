
package io.github.leonardoaliaga.argbcolorlens

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.*
import java.awt.Color

/**
 * Utilidades para reconocer y editar colores ARGB
 * en expresiones Java.
 *
 * Separa la lógica PSI de la interfaz del selector.
 */
internal object ArgbPsi {

    private val pattern = Regex("0[xX][0-9a-fA-F]{8}")

    /**
     * Identifica la expresión ARGB donde está el cursor.
     */
    fun expressionAtCaret(
        file: PsiFile,
        offset: Int
    ): PsiExpression? {

        if (file.textLength == 0) return null

        val element = file.findElementAt(
            offset.coerceIn(0, file.textLength - 1)
        ) ?: return null

        var node: PsiElement? = element

        while (node != null && node !== file) {

            when (node) {
                is PsiReferenceExpression -> {
                    if (sourceField(node) != null) return node
                }

                is PsiLiteralExpression -> {
                    if (parseArgb(node.text) != null) return node
                }
            }

            node = node.parent
        }

        return null
    }

    /**
     * Resuelve el campo original de una referencia.
     *
     * Ejemplo: UiTheme.TEXT_PRIMARY
     */
    fun sourceField(expression: PsiExpression): PsiField? {

        val reference = expression as? PsiReferenceExpression
            ?: return null

        val field = reference.resolve() as? PsiField
            ?: return null

        if (!field.hasModifierProperty(PsiModifier.FINAL)) {
            return null
        }

        val initializer = field.initializer as? PsiLiteralExpression
            ?: return null

        if (parseArgb(initializer.text) == null) return null

        return field
    }

    /**
     * Obtiene el color de un literal o una referencia.
     */
    fun colorOf(expression: PsiExpression): Color? {

        val argb = when (expression) {

            is PsiLiteralExpression -> {
                parseArgb(expression.text)
            }

            is PsiReferenceExpression -> {
                val initializer = sourceField(expression)
                    ?.initializer as? PsiLiteralExpression

                initializer?.let { parseArgb(it.text) }
            }

            else -> null

        } ?: return null

        return Color(argb, true)
    }

    /**
     * Modifica solamente la expresión seleccionada.
     */
    fun replaceColor(expression: PsiExpression, color: Color) {

        val project = expression.project

        WriteCommandAction.runWriteCommandAction(project) {

            if (!expression.isValid || !expression.isWritable) {
                return@runWriteCommandAction
            }

            val replacement = JavaPsiFacade
                .getElementFactory(project)
                .createExpressionFromText(
                    hexArgb(color),
                    expression
                )

            expression.replace(replacement)
        }
    }

    /**
     * Modifica el inicializador de la constante original.
     */
    fun replaceOriginal(field: PsiField, color: Color) {

        val project = field.project

        WriteCommandAction.runWriteCommandAction(project) {

            if (!field.isValid || !field.isWritable) {
                return@runWriteCommandAction
            }

            val initializer = field.initializer as? PsiLiteralExpression
                ?: return@runWriteCommandAction

            if (parseArgb(initializer.text) == null) {
                return@runWriteCommandAction
            }

            val replacement = JavaPsiFacade
                .getElementFactory(project)
                .createExpressionFromText(
                    hexArgb(color),
                    initializer
                )

            initializer.replace(replacement)
        }
    }

    private fun parseArgb(text: String): Int? {

        if (!pattern.matches(text)) return null

        return text.substring(2)
            .toLongOrNull(16)
            ?.toInt()
    }

    private fun hexArgb(color: Color): String {
        return "0x%08X".format(color.rgb)
    }
}
