
package io.github.leonardoaliaga.argbcolorlens

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ElementColorProvider
import com.intellij.psi.*
import java.awt.Color

/**
 * ARGB Color Lens
 *
 * Soporta:
 * - Literales Java: 0xFF212121
 * - Transparencia: 0x804995B6
 * - Constantes Java: UiTheme.TEXT_PRIMARY
 */
class ArgbColorProvider : ElementColorProvider {

    private val argbPattern = Regex("0[xX][0-9a-fA-F]{8}")

    override fun getColorFrom(element: PsiElement): Color? {
        val expression = findExpression(element) ?: return null
        val argb = getArgbBits(expression) ?: return null

        return Color(argb, true)
    }

    override fun setColorTo(element: PsiElement, color: Color) {
        val expression = findExpression(element) ?: return
        val replacement = "0x%08X".format(color.rgb)

        WriteCommandAction.runWriteCommandAction(element.project) {
            val factory = JavaPsiFacade.getElementFactory(element.project)

            val newExpression = factory.createExpressionFromText(
                replacement,
                expression
            )

            expression.replace(newExpression)
        }
    }

    /**
     * Identifica literales y referencias por separado.
     *
     * Los literales conservan su propia ruta de detección,
     * independientemente de las referencias Java.
     */
    private fun findExpression(element: PsiElement): PsiExpression? {
        val parent = element.parent

        // Caso 1: literal hexadecimal.
        if (element is PsiJavaToken &&
            parent is PsiLiteralExpression
        ) {
            return parent
        }

        // Caso 2: nombre de una referencia Java.
        if (element is PsiIdentifier &&
            parent is PsiReferenceExpression
        ) {
            if (parent.referenceNameElement == element) {
                return parent
            }
        }

        return null
    }

    /**
     * Recupera el valor ARGB de la expresión.
     */
    private fun getArgbBits(expression: PsiExpression): Int? {

        // Color hexadecimal directo.
        if (expression is PsiLiteralExpression) {
            return parseArgb(expression.text)
        }

        // Referencia a una constante Java.
        if (expression is PsiReferenceExpression) {
            val field = expression.resolve() as? PsiField
                ?: return null

            if (!field.hasModifierProperty(PsiModifier.FINAL)) {
                return null
            }

            val initializer = field.initializer ?: return null

            return parseArgb(initializer.text)
        }

        return null
    }

    /**
     * Convierte 0xAARRGGBB en un entero de 32 bits.
     */
    private fun parseArgb(text: String): Int? {
        if (!argbPattern.matches(text)) return null

        return text.substring(2)
            .toLongOrNull(16)
            ?.toInt()
    }
}
