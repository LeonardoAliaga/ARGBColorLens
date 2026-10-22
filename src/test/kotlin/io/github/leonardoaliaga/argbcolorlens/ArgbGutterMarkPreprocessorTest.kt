package io.github.leonardoaliaga.argbcolorlens

import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.ElementColorProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.ui.ColorLineMarkerProvider
import java.awt.Color
import java.util.function.Supplier

class ArgbGutterMarkPreprocessorTest : LightJavaCodeInsightFixtureTestCase() {
    private val preprocessor = ArgbGutterMarkPreprocessor()

    fun testLiteralKeepsOnlyPluginSwatchAndBothMouseActions() {
        configure("class Palette { static final int BASE = 0x804995B6; }")
        val plugin = pluginMarker(leaf("0x804995B6"))
        val native = nativeMarkers(leaf("0x804995B6")).single().createGutterRenderer()!!

        // The real platform color provider creates the second, default-picker swatch.
        val result = preprocessor.processMarkers(listOf(native, plugin))

        assertEquals(1, result.size)
        assertSame(plugin, result.single())
        assertNotNull(plugin.clickAction)
        assertNotNull(plugin.rightButtonClickAction)
    }

    fun testNativeSwatchOnFieldNameIsAlsoRemoved() {
        configure("class Palette { static final int BASE = 0x804995B6; }")
        val plugin = pluginMarker(leaf("0x804995B6"))
        val native = nativeMarkers(leaf("BASE")).single().createGutterRenderer()!!

        assertEquals(listOf(plugin), preprocessor.processMarkers(listOf(plugin, native)))
    }

    fun testLocalVariableNameAndTransparentLiteralAreDeduplicated() {
        configure("class Palette { void draw() { int color = 0x00000000; } }")
        val plugin = pluginMarker(leaf("0x00000000"))
        val native = nativeMarkers(leaf("color"), leaf("0x00000000"))
            .map { it.createGutterRenderer()!! }

        assertEquals(listOf(plugin), preprocessor.processMarkers(native + plugin))
    }

    fun testConstantUsageKeepsPluginDefinitionEditing() {
        configure("class Palette { static final int BASE = 0x804995B6; int used = BASE; }")
        val usage = leaf("BASE", last = true)
        val plugin = pluginMarker(usage)
        val native = nativeMarkers(usage).single().createGutterRenderer()!!

        assertEquals(listOf(plugin), preprocessor.processMarkers(listOf(native, plugin)))
        assertNotNull(plugin.rightButtonClickAction)
    }

    fun testDifferentNativeColorOnSameLineIsPreserved() {
        configure("class Palette { int a = 0x804995B6, b = 0x123456; }")
        val plugin = pluginMarker(leaf("0x804995B6"))
        val native = nativeMarkers(leaf("0x123456")).single().createGutterRenderer()!!
        val markers = listOf<GutterMark>(native, plugin)

        assertEquals(markers, preprocessor.processMarkers(markers))
    }

    fun testUnrelatedGutterIconAtSamePositionIsPreserved() {
        configure("class Palette { int color = 0x804995B6; }")
        val token = leaf("0x804995B6")
        val plugin = pluginMarker(token)
        val unrelated = LineMarkerInfo(
            token, token.textRange, AllIcons.Gutter.OverridingMethod,
            null, null, GutterIconRenderer.Alignment.LEFT, Supplier { "Unrelated marker" }
        ).createGutterRenderer()!!
        val markers = listOf<GutterMark>(plugin, unrelated)

        assertEquals(markers, preprocessor.processMarkers(markers))
    }

    fun testNativePreviewRemainsWhenThereIsNoPluginSwatch() {
        configure("class Palette { int color = 0x123456; }")
        val token = leaf("0x123456")
        assertNull(ArgbLineMarkerProvider().getLineMarkerInfo(token))
        val markers = nativeMarkers(token).map { it.createGutterRenderer()!! }

        assertSame(markers, preprocessor.processMarkers(markers))
    }

    fun testMixedNativeGroupKeepsItsUnrelatedColorAndAction() {
        configure("class Palette { int a = 0x804995B6, b = 0x123456; }")
        val plugin = pluginMarker(leaf("0x804995B6"))
        val natives = nativeMarkers(leaf("0x804995B6"), leaf("0x123456"))
        val group = MergeableLineMarkerInfo.merge(
            natives.filterIsInstance<MergeableLineMarkerInfo<*>>(), 0
        ).single().createGutterRenderer()!!

        val result = preprocessor.processMarkers(listOf(group, plugin))

        assertEquals(2, result.size)
        assertSame(plugin, result.last())
        val retained = result.first() as LineMarkerInfo.LineMarkerGutterIconRenderer<*>
        assertSame(natives.last(), retained.lineMarkerInfo)
        assertNotNull(retained.clickAction)
    }

    fun testFullyDuplicatedNativeGroupIsRemoved() {
        configure("class Palette { int a = 0x804995B6, b = 0xFF123456; }")
        val first = leaf("0x804995B6")
        val second = leaf("0xFF123456")
        val plugins = listOf(pluginMarker(first), pluginMarker(second))
        val natives = nativeMarkers(first, second)
        val group = MergeableLineMarkerInfo.merge(
            natives.filterIsInstance<MergeableLineMarkerInfo<*>>(), 0
        ).single().createGutterRenderer()!!

        assertEquals(plugins, preprocessor.processMarkers(plugins + group))
    }

    private fun configure(source: String) {
        myFixture.configureByText("Palette.java", source)
    }

    private fun leaf(text: String, last: Boolean = false): PsiElement {
        val source = myFixture.file.text
        val offset = if (last) source.lastIndexOf(text) else source.indexOf(text)
        check(offset >= 0) { "Missing test token: $text" }
        return checkNotNull(myFixture.file.findElementAt(offset))
    }

    private fun pluginMarker(element: PsiElement): GutterIconRenderer =
        checkNotNull(ArgbLineMarkerProvider().getLineMarkerInfo(element)?.createGutterRenderer())

    private fun nativeMarkers(vararg elements: PsiElement): List<LineMarkerInfo<*>> {
        // Model another ElementColorProvider recognizing the same Java color.
        // Let IntelliJ create the actual native renderer and native picker action.
        ElementColorProvider.EP_NAME.point.registerExtension(object : ElementColorProvider {
            override fun getColorFrom(element: PsiElement): Color? =
                if (elements.any { it === element }) Color(0x804995B6.toInt(), true) else null

            override fun setColorTo(element: PsiElement, color: Color) = Unit
        }, testRootDisposable)
        val result = mutableListOf<LineMarkerInfo<*>>()
        ColorLineMarkerProvider().collectSlowLineMarkers(elements.toList(), result)
        return result
    }
}
