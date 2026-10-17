package io.github.leonardoaliaga.argbcolorlens

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.ColorChooserService
import com.intellij.ui.ColorPickerListener
import java.awt.Color
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Settings > Tools > ARGB Color Lens.
 *
 * The editor is only changed after Apply/OK, never while the native color
 * chooser is open. Settings persist at the application level across projects.
 */
class ArgbSettingsConfigurable : Configurable {
    private var panel: JPanel? = null
    private var enabledCheck: JCheckBox? = null
    private var swatchCheck: JCheckBox? = null
    private var chooseButton: JButton? = null
    private var colorPreview: JLabel? = null
    private var colorText: JLabel? = null

    private var customArgb: Int = ArgbHighlightSettings.DEFAULT_COLOR

    override fun getDisplayName(): String = "ARGB Color Lens"

    override fun createComponent(): JComponent {
        val enabled = JCheckBox(ArgbMessages.message("settings.highlight.enabled"))
        val swatch = JCheckBox(ArgbMessages.message("settings.highlight.swatch"))
        val choose = JButton(ArgbMessages.message("settings.highlight.choose"))
        val preview = JLabel()
        val hex = JLabel()
        val info = JLabel(ArgbMessages.message("settings.highlight.description"))

        enabledCheck = enabled
        swatchCheck = swatch
        chooseButton = choose
        colorPreview = preview
        colorText = hex

        val previewRow = JPanel(FlowLayout(FlowLayout.LEADING, 8, 0)).apply {
            add(JLabel(ArgbMessages.message("settings.highlight.custom")))
            add(preview)
            add(hex)
            add(choose)
        }

        val content = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            anchor = GridBagConstraints.LINE_START
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = Insets(5, 8, 5, 8)
        }
        content.add(enabled, gbc)
        gbc.gridy++
        gbc.insets = Insets(2, 28, 4, 8)
        content.add(swatch, gbc)
        gbc.gridy++
        content.add(previewRow, gbc)
        gbc.gridy++
        gbc.insets = Insets(8, 8, 5, 8)
        content.add(info, gbc)
        gbc.gridy++
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        content.add(JPanel(), gbc)

        enabled.addActionListener { refreshEnabled() }
        swatch.addActionListener { refreshEnabled() }
        choose.addActionListener {
            val unchanged = Color(customArgb, true)
            // A non-empty listener list uses the native "Choose" confirmation
            // instead of the misleading "Copy" action.
            val listener = object : ColorPickerListener {
                override fun colorChanged(color: Color) = Unit
                override fun closed(color: Color?) = Unit
            }
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            val picked = ColorChooserService.getInstance().showDialog(
                project, choose, ArgbMessages.message("settings.highlight.picker"),
                unchanged, true, listOf(listener), false
            ) ?: return@addActionListener
            customArgb = picked.rgb
            updateColorPreview()
        }

        panel = content
        reset()
        return content
    }

    private fun refreshEnabled() {
        val active = enabledCheck?.isSelected == true
        swatchCheck?.isEnabled = active
        val customEnabled = active && swatchCheck?.isSelected != true
        chooseButton?.isEnabled = customEnabled
        colorPreview?.isEnabled = customEnabled
        colorText?.isEnabled = customEnabled
    }

    private fun updateColorPreview() {
        colorPreview?.icon = ArgbSwatchIcon(customArgb)
        colorText?.text = "0x%08X".format(customArgb)
    }

    override fun isModified(): Boolean {
        val state = ArgbHighlightSettings.getInstance().snapshot()
        return enabledCheck?.isSelected != state.enabled ||
            swatchCheck?.isSelected != state.useSwatchColor ||
            customArgb != state.customColorArgb
    }

    override fun apply() {
        val state = ArgbHighlightSettings.Options(
            enabled = enabledCheck?.isSelected == true,
            useSwatchColor = swatchCheck?.isSelected == true,
            customColorArgb = customArgb
        )
        ArgbHighlightSettings.getInstance().update(state)
        ArgbHoverHighlight.clear()
    }

    override fun reset() {
        val state = ArgbHighlightSettings.getInstance().snapshot()
        enabledCheck?.isSelected = state.enabled
        swatchCheck?.isSelected = state.useSwatchColor
        customArgb = state.customColorArgb
        updateColorPreview()
        refreshEnabled()
    }

    override fun disposeUIResources() {
        panel = null
        enabledCheck = null
        swatchCheck = null
        chooseButton = null
        colorPreview = null
        colorText = null
    }
}
